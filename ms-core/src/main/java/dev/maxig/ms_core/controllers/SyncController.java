package dev.maxig.ms_core.controllers;

import dev.maxig.ms_core.enums.InfoOperationsEnum;
import dev.maxig.ms_core.enums.SyncOperationsEnum;
import dev.maxig.ms_core.events.requests.InfoRequestEvent;
import dev.maxig.ms_core.events.requests.SyncRequestEvent;
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
public class SyncController {

    @Value("${config.response-timeout}")
    private Long responseTimeout;

    @Value("${config.kafka-topics.sync.request}")
    private String syncRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public SyncController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @GetMapping("/api/v1/sync")
    public Mono<ResponseEntity<String>> loadCache() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation syncObservation = Observation.createNotStarted("ms-sync", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(syncRequestTopic, traceId, JsonUtils.toJson(new SyncRequestEvent(SyncOperationsEnum.SYNC_CACHE, traceId)));

            return Mono.fromFuture(eventService.createPendingSyncRequest(traceId))
                    .timeout(Duration.ofMillis(60000))
                    .map(response -> {
                        syncObservation.stop();
                        return ResponseEntity.ok(response);
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        syncObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("Failed to get prometheus data from sync service"));
        });
    }
}