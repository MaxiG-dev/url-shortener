package dev.maxig.api_gateway.controllers;

import dev.maxig.api_gateway.config.WebClientConfig;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.result.view.RedirectView;
import reactor.core.publisher.Mono;

import java.net.URI;



@RestController
public class RedirectController {
    @Value("${config.url}")
    private String redirectUrl;

    @Value ("${config.application.x-api-key.ms-redirect}")
    private String msRedirectApiKey;

    private final ObservationRegistry observationRegistry;
    private final WebClient.Builder webClientBuilder;

    @Autowired
    public RedirectController(ObservationRegistry observationRegistry, WebClient.Builder webClientBuilder) {
        this.observationRegistry = observationRegistry;
        this.webClientBuilder = webClientBuilder;
    }

    @GetMapping("/{shortUrl}")
    public Mono<ResponseEntity<Object>> redirect(@PathVariable String shortUrl) {
        Observation redirectObservation = Observation.createNotStarted("ms-redirect", observationRegistry);
        return redirectObservation.observe(() -> {
            if (shortUrl.contains("/")) {
                return Mono.just(ResponseEntity.status(303).location(URI.create(redirectUrl + "page-not-found/404.html")).build());
            }
            return webClientBuilder.build().get()
                    .uri("lb://ms-redirect" + "/api/v1/redirect/" + shortUrl)
                    .header("x-api-key", msRedirectApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .map(url -> ResponseEntity.status(303)
                            .location(URI.create(url))
                            .build())
                    .onErrorResume(e -> {
                        return Mono.just(ResponseEntity.status(303).location(URI.create(redirectUrl + "/page-not-found/404.html")).build());
                    });
        });
    }
}