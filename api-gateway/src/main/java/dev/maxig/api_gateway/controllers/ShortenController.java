package dev.maxig.api_gateway.controllers;

import dev.maxig.api_gateway.dtos.CreateUrlDTO;
import dev.maxig.api_gateway.events.requests.ShortenRequestEvent;
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
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class ShortenController {

    @Value("${config.gateway-timeout}")
    private Long gatewayTimeout;

    @Value("${config.kafka-topics.shorten.request}")
    private String shortenRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public ShortenController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @PostMapping("/api/v1/shorten")
    public Mono<ResponseEntity<String>> shorten(@RequestBody CreateUrlDTO createUrlDTO) {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation shortenObservation = Observation.createNotStarted("ms-shorten", observationRegistry)
                                                         .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            if (createUrlDTO.getUserId().isEmpty()) {
                return Mono.just(ResponseEntity.status(400).body("userId is invalid"));
            }

            kafkaTemplate.send(shortenRequestTopic, traceId, JsonUtils.toJson(new ShortenRequestEvent(createUrlDTO.getLongUrl(), createUrlDTO.getUserId(), traceId)));

            return Mono.fromFuture(eventService.createPendingShortenRequest(traceId))
                    .timeout(Duration.ofMillis(gatewayTimeout))
                    .flatMap(response -> {
                        if (response.contains("Invalid URL")) {
                            return Mono.error(new NotFoundException("Invalid URL"));
                        }
                        shortenObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.CREATED).body(response));
                    })
                    .onErrorResume(NotFoundException.class, ex -> {
                        eventService.removeShortenRequest(traceId);
                        shortenObservation.stop();
                        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid URL"));
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeShortenRequest(traceId);
                        shortenObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        eventService.removeShortenRequest(traceId);
                        shortenObservation.stop();
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error, please try again later or contact support."));
                    });
        });
    }
}