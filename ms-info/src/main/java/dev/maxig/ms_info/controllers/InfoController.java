package dev.maxig.ms_info.controllers;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.services.InfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/info/")
public class InfoController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    @Autowired
    private InfoService service;

//    CompletableFuture<Url> getUrl(String shortId);
//    CompletableFuture<List<Url>> getAllUrls(String shortUrl, boolean getDeletedUrls);
//    CompletableFuture<List<Url>> getUserUrls(String userId, boolean getDeletedUrls);

    @GetMapping("{shortUrl}")
    public CompletableFuture<ResponseEntity<String>> getLongUrl(@PathVariable String shortUrl, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getLongUrl(shortUrl).thenApply(ResponseEntity::ok);
    }

    @GetMapping("stats")
    public CompletableFuture<ResponseEntity<Stats>> getGlobalStats(@RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return service.getGlobalStats().thenApply(ResponseEntity::ok);
    }
}
