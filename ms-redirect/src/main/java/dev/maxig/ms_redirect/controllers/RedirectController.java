package dev.maxig.ms_redirect.controllers;

import dev.maxig.ms_redirect.services.RedirectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
public class RedirectController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    @Autowired
    private RedirectService service;

    @GetMapping("/api/v1/{shortUrl}")
    @ResponseBody
    public CompletableFuture<ResponseEntity<String>> get(@PathVariable String shortUrl, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getLongUrl(shortUrl, false).thenApply(ResponseEntity::ok);
    }

    @GetMapping("/api/v1/redirect/{shortUrl}")
    @ResponseBody
    public CompletableFuture<ResponseEntity<String>> redirect(@PathVariable String shortUrl, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getLongUrl(shortUrl, true).thenApply(ResponseEntity::ok);
    }

}
