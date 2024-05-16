package dev.maxig.api_gateway.controllers;

import dev.maxig.api_gateway.events.requests.RedirectRequestEvent;
import dev.maxig.api_gateway.services.EventService;
import dev.maxig.api_gateway.utils.JsonUtils;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

@RestController
@Slf4j
public class RedirectController {

    @Value("${config.domain-to-redirect}")
    private String domainToRedirect;

    @Value("${config.gateway-timeout}")
    private Long gatewayTimeout;

    @Value("${config.kafka-topics.redirect.request}")
    private String redirectRequestTopic;

    private final ObservationRegistry observationRegistry;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final EventService eventService;

    @Autowired
    public RedirectController(ObservationRegistry observationRegistry, KafkaTemplate<String, String> kafkaTemplate, EventService eventService) {
        this.observationRegistry = observationRegistry;
        this.kafkaTemplate = kafkaTemplate;
        this.eventService = eventService;
    }

    @GetMapping("/{shortUrl}")
    public Mono<ResponseEntity<Object>> redirect(@PathVariable String shortUrl) {
        String traceId = UUID.randomUUID().toString();
        return Mono.deferContextual(contextView -> {
            Observation redirectObservation = Observation.createNotStarted("ms-redirect", observationRegistry)
                                                         .lowCardinalityKeyValue("traceId", traceId)
                                                         .start();

            if (shortUrl.contains("/")) {
                redirectObservation.stop();
                return Mono.just(ResponseEntity.status(303).location(URI.create(domainToRedirect + "/page-not-found/404.html")).build());
            }

            kafkaTemplate.send(redirectRequestTopic, traceId, JsonUtils.toJson(new RedirectRequestEvent(shortUrl, traceId)));

            return Mono.fromFuture(eventService.createPendingRedirectRequest(traceId))
                    .timeout(Duration.ofMillis(gatewayTimeout))
                    .map(response -> {
                        if (response.contains("404")) {
                            redirectObservation.stop();
                            return ResponseEntity.status(303).location(URI.create(domainToRedirect + "/page-not-found/404.html")).build();
                        }
                        redirectObservation.stop();
                        return ResponseEntity.status(HttpStatus.SEE_OTHER).location(URI.create(response)).build();
                    })
                    .onErrorResume(TimeoutException.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        redirectObservation.stop();
                        return Mono.just(ResponseEntity.status(504).body(("Timeout waiting for response")));
                    })
                    .onErrorResume(Exception.class, ex -> {
                        eventService.removeRedirectRequest(traceId);
                        redirectObservation.stop();
                        return Mono.just(ResponseEntity.status(500).body("Internal Server Error, please try again later or contact support."));
                    });
        });
    }
}