package dev.maxig.ms_core.controllers;

import dev.maxig.ms_core.enums.InfoOperationsEnum;
import dev.maxig.ms_core.events.requests.InfoRequestEvent;
import dev.maxig.ms_core.services.EventService;
import dev.maxig.ms_core.utils.JsonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class InfoController {

    @Value("${config.response-timeout}")
    private Long responseTimeout;

    @Value("${config.kafka-topics.info.request}")
    private String infoRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public InfoController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @GetMapping("/api/v1/info/stats")
    public Mono<ResponseEntity<Object>> infoStats() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation infoObservation = Observation.createNotStarted("ms-info", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(infoRequestTopic, traceId, JsonUtils.toJson(new InfoRequestEvent(InfoOperationsEnum.GET_STATS, traceId, null)));

            return Mono.fromFuture(eventService.createPendingInfoRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .flatMap(response -> {
                        if (response.toString().contains("Failed to get Stats")) {
                            return Mono.error(new NotFoundException("Failed to get Stats"));
                        }
                        if (response.toString().contains("Failed to get long URL for shortId")) {
                            return Mono.error(new NotFoundException("Failed to get long URL for shortId"));
                        }
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(response));
                    })
                    .onErrorResume(NotFoundException.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage()));
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error, please try again later or contact support."));
                    });
        });
    }

    @GetMapping("/api/v1/info/{shortUrl}")
    public Mono<ResponseEntity<Object>> infoUrl(@PathVariable String shortUrl) {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation infoObservation = Observation.createNotStarted("ms-info", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            if (shortUrl.contains("/")) {
                infoObservation.stop();
                return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("URL not found"));
            }

            kafkaTemplate.send(infoRequestTopic, traceId, JsonUtils.toJson(new InfoRequestEvent(InfoOperationsEnum.GET_URL, traceId, shortUrl)));

            return Mono.fromFuture(eventService.createPendingInfoRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .flatMap(response -> {
                        String responseString = JsonUtils.toJson(response);
                        if (responseString.contains("URL not found")) {
                            return Mono.error(new NotFoundException("URL not found"));
                        }
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.OK).body(response));
                    })
                    .onErrorResume(NotFoundException.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("URL not found"));
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        eventService.removeInfoRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error, please try again later or contact support."));
                    });
        });
    }
}