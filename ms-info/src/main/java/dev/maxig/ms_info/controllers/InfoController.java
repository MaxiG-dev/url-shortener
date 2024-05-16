package dev.maxig.ms_info.controllers;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;
import dev.maxig.ms_info.services.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/info/")
public class InfoController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    @Autowired
    private InfoService service;

    @GetMapping("{shortUrl}")
    public CompletableFuture<ResponseEntity<Url>> getUrl(@PathVariable String shortUrl) {
        return service.getUrl(shortUrl).thenApply(ResponseEntity::ok);
    }

    @GetMapping("stats")
    public CompletableFuture<ResponseEntity<Stats>> getGlobalStats(@RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getGlobalStats().thenApply(ResponseEntity::ok);
    }
}
