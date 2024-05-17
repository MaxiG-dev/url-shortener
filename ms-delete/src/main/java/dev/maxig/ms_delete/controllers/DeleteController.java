package dev.maxig.ms_delete.controllers;

import dev.maxig.ms_delete.services.DeleteService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/v1/delete/")
public class DeleteController {
    @Value("${config.application.x-api-key}")
    private String configApiKey;

    private final DeleteService deleteService;

    public DeleteController(DeleteService deleteService) {
        this.deleteService = deleteService;
    }

    @DeleteMapping("{shortUrl}")
    public CompletableFuture<ResponseEntity<String>> get(@PathVariable String shortUrl, @RequestHeader("x-api-key") String apikey) {
        if (apikey == null || !apikey.equals(configApiKey)) {
            return CompletableFuture.completedFuture(ResponseEntity.status(403).build());
        }
        return deleteService.deleteUrl(shortUrl).thenApply(result -> ResponseEntity.status(HttpStatus.NO_CONTENT).build());
    }

}
