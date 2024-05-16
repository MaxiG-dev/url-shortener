package dev.maxig.api_gateway.controllers;

import dev.maxig.api_gateway.events.requests.PrometheusRequestEvent;
import dev.maxig.api_gateway.services.EventService;
import dev.maxig.api_gateway.utils.JsonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Slf4j
public class PrometheusController {

    @Value("${config.domain-to-redirect}")
    private String domainToRedirect;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public PrometheusController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService, Environment env) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @GetMapping("/actuator/redirect/prometheus")
    public Mono<ResponseEntity<String>> redirect() {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation redirectObservation = Observation.createNotStarted("prometheus-request-redirect", observationRegistry)
                    .lowCardinalityKeyValue("traceId", traceId)
                    .start();

            kafkaTemplate.send("prometheus-request-topic", JsonUtils.toJson(new PrometheusRequestEvent(traceId)));

            return Mono.fromFuture(eventService.createPendingPrometheusRequest(traceId))
                    .map(url -> {
                        redirectObservation.stop();
                        return ResponseEntity.ok(url);
                    })
                    .defaultIfEmpty(ResponseEntity.status(404).body("URL not found"));
        });
    }
}
