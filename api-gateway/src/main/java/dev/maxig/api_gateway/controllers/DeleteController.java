package dev.maxig.api_gateway.controllers;


import dev.maxig.api_gateway.events.requests.DeleteRequestEvent;
import dev.maxig.api_gateway.services.EventService;
import dev.maxig.api_gateway.utils.JsonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class DeleteController {

    @Value("${config.gateway-timeout}")
    private Long gatewayTimeout;

    @Value("${config.kafka-topics.delete.request}")
    private String deleteRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public DeleteController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @DeleteMapping("/api/v1/delete/{shortUrl}")
    public Mono<ResponseEntity<Object>> delete(@PathVariable String shortUrl) {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation deleteObservation = Observation.createNotStarted("ms-delete", observationRegistry)
                                                         .lowCardinalityKeyValue("traceId", traceId)
                                                         .start();

            kafkaTemplate.send(deleteRequestTopic, traceId, JsonUtils.toJson(new DeleteRequestEvent(shortUrl, traceId)));

            return Mono.fromFuture(eventService.createPendingDeleteRequest(traceId))
                    .timeout(Duration.ofMillis(gatewayTimeout))
                    .flatMap(response -> {
                        if (response.contains("URL not found")) {
                            return Mono.error(new NotFoundException("URL not found"));
                        }
                        deleteObservation.stop();
                        return Mono.just(ResponseEntity.noContent().build());
                    })
                    .onErrorResume(NotFoundException.class, ex -> {
                        eventService.removeDeleteRequest(traceId);
                        deleteObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).body("URL not found"));
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeDeleteRequest(traceId);
                        deleteObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        eventService.removeDeleteRequest(traceId);
                        deleteObservation.stop();
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error, please try again later or contact support."));
                    });
        });
    }
}