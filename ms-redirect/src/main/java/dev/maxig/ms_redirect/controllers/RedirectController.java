package dev.maxig.ms_redirect.controllers;

import dev.maxig.ms_redirect.services.RedirectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/redirect/")
public class RedirectController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    @Autowired
    private RedirectService service;

    @GetMapping("{shortUrl}")
    @ResponseBody
    public CompletableFuture<ResponseEntity<String>> redirect(@PathVariable String shortUrl, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getLongUrl(shortUrl).thenApply(ResponseEntity::ok);
    }

}
