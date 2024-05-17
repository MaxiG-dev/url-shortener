package dev.maxig.ms_core.controllers;

import dev.maxig.ms_core.enums.*;
import dev.maxig.ms_core.events.requests.DeleteRequestEvent;
import dev.maxig.ms_core.events.requests.InfoRequestEvent;
import dev.maxig.ms_core.events.requests.ShortenRequestEvent;
import dev.maxig.ms_core.events.requests.SyncRequestEvent;
import dev.maxig.ms_core.services.EventService;
import dev.maxig.ms_core.utils.JsonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class PrometheusController {

    @Value("${config.response-timeout}")
    private Long responseTimeout;

    @Value("${config.kafka-topics.redirect.request}")
    private String redirectRequestTopic;

    @Value("${config.kafka-topics.info.request}")
    private String infoRequestTopic;

    @Value("${config.kafka-topics.delete.request}")
    private String deleteRequestTopic;

    @Value("${config.kafka-topics.shorten.request}")
    private String shortenRequestTopic;

    @Value("${config.kafka-topics.sync.request}")
    private String syncRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    public PrometheusController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService, Environment env) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @GetMapping(value = "/actuator/redirect/prometheus", produces = "text/plain")
    public Mono<ResponseEntity<String>> prometheusRedirect() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation redirectObservation = Observation.createNotStarted("prometheus-request-redirect", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(redirectRequestTopic, JsonUtils.toJson(new InfoRequestEvent(RedirectOperationsEnum.GET_PROMETHEUS, traceId, null)));

            return Mono.fromFuture(eventService.createPendingRedirectRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .map(metrics -> {
                        redirectObservation.stop();
                        return ResponseEntity.ok(metrics);
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        redirectObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("Failed to get prometheus data from redirect service"));
        });
    }

    @GetMapping(value = "/actuator/shorten/prometheus", produces = "text/plain")
    public Mono<ResponseEntity<String>> prometheusShorten() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation shortenObservation = Observation.createNotStarted("prometheus-request-shorten", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(shortenRequestTopic, JsonUtils.toJson(new ShortenRequestEvent(ShortenOperationsEnum.GET_PROMETHEUS, traceId, null, null)));

            return Mono.fromFuture(eventService.createPendingShortenRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .map(metrics -> {
                        shortenObservation.stop();
                        return ResponseEntity.ok(metrics);
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        shortenObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("Failed to get prometheus data from shorten service"));
        });
    }

    @GetMapping(value = "/actuator/info/prometheus", produces = "text/plain")
    public Mono<ResponseEntity<String>> prometheusInfo() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation infoObservation = Observation.createNotStarted("prometheus-request-info", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(infoRequestTopic, JsonUtils.toJson(new InfoRequestEvent(InfoOperationsEnum.GET_PROMETHEUS, traceId, null)));

            return Mono.fromFuture(eventService.createPendingInfoRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .map(metrics -> {
                        String responseString = JsonUtils.toJson(metrics);
                        infoObservation.stop();
                        return ResponseEntity.ok(metrics.toString());
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        infoObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("Failed to get prometheus data from info service"));
        });
    }

    @GetMapping(value = "/actuator/delete/prometheus", produces = "text/plain")
    public Mono<ResponseEntity<String>> prometheusDelete() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation deleteObservation = Observation.createNotStarted("prometheus-request-delete", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(deleteRequestTopic, JsonUtils.toJson(new DeleteRequestEvent(DeleteOperationsEnum.GET_PROMETHEUS, traceId, null)));

            return Mono.fromFuture(eventService.createPendingDeleteRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .map(metrics -> {
                        deleteObservation.stop();
                        return ResponseEntity.ok(metrics);
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        deleteObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("Failed to get prometheus data from delete service"));
        });
    }


    @GetMapping(value = "/actuator/sync/prometheus", produces = "text/plain")
    public Mono<ResponseEntity<String>> prometheusSync() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation syncObservation = Observation.createNotStarted("prometheus-request-sync", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send(syncRequestTopic, JsonUtils.toJson(new SyncRequestEvent(SyncOperationsEnum.GET_PROMETHEUS, traceId)));

            return Mono.fromFuture(eventService.createPendingSyncRequest(traceId))
                    .timeout(Duration.ofMillis(responseTimeout))
                    .map(metrics -> {
                        syncObservation.stop();
                        return ResponseEntity.ok(metrics);
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
