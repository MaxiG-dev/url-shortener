package dev.maxig.ms_core.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.servlet.view.RedirectView;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
public class MsCoreController {

    private final WebClient webClient;

    @Value("${config.application.x-api-key.ms-shorten}")
    private String msShortenApiKey;
    @Value("${config.application.x-api-key.ms-redirect}")
    private String msRedirectApiKey;
    @Value("${config.application.x-api-key.ms-delete}")
    private String msDeleteApiKey;
    @Value("${config.application.x-api-key.ms-info}")
    private String msInfoApiKey;
    @Value("${config.application.x-api-key.ms-auth}")
    private String msAuthApiKey;
    @Value("${config.application.x-api-key.ms-user}")
    private String msUserApiKey;

    @GetMapping("/api/v1/health")
    @ResponseBody
    public String health() {
        return "Service available!";
    }

    @GetMapping("/api/v1/info/{shortUrl}")
    public Mono<String> get(@PathVariable String shortUrl) {
        try {
            var test = webClient.get()
                    .uri("http://localhost:8084/api/v1/info/" + shortUrl)
                    .header("x-api-key", msInfoApiKey)
                    .retrieve()
                    .bodyToMono(String.class)
                    .doOnError(error -> System.out.println("Error during WebClient call: " + error.getMessage()))
                    .onErrorResume(WebClientResponseException.NotFound.class, ex -> Mono.just("URL not found - handled gracefully."));
            return test;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Mono.just("Error");
    }

    @GetMapping("/{shortUrl}")
    public Mono<RedirectView> redirect(@PathVariable String shortUrl) {
        return webClient.get()
                .uri("http://localhost:8081/api/v1/" + shortUrl)
                .header("x-api-key", msRedirectApiKey)
                .retrieve()
                .bodyToMono(String.class)
                .map(RedirectView::new);

    }

}
