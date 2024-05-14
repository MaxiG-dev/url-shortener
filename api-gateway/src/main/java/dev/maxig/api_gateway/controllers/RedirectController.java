package dev.maxig.api_gateway.controllers;

import dev.maxig.api_gateway.config.WebClientConfig;
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

    private final String msRedirectApiKey;
    private final WebClient webClient;

    @Autowired
    public RedirectController(String msRedirectApiKey, WebClient webClient) {
        this.msRedirectApiKey = msRedirectApiKey;
        this.webClient = webClient;
    }

    @GetMapping("/{shortUrl}")
    public Mono<ResponseEntity<Void>> redirect(@PathVariable String shortUrl) {
        return webClient.get()
                .uri("http://localhost:8081/api/v1/redirect/" + shortUrl)
                .header("x-api-key", msRedirectApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(url -> ResponseEntity.status(303)
                        .location(URI.create(url))
                        .build());
    }
}