package dev.maxig.ms_shorten.controllers;

import dev.maxig.ms_shorten.dto.CreateUrlDTO;

import dev.maxig.ms_shorten.services.ShortenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/shorten")
public class ShortenController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    @Autowired
    private ShortenService service;

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public CompletableFuture<ResponseEntity<String>> shorten(@RequestBody CreateUrlDTO createUrlDTO, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.create(createUrlDTO).thenApply(ResponseEntity::ok);
    }

}
