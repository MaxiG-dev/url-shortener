package dev.maxig.ms_shorten.services;

import dev.maxig.ms_shorten.dto.CreateUrlDTO;

import java.util.concurrent.CompletableFuture;

public interface ShortenService {
    CompletableFuture<String> create(CreateUrlDTO createUrlDTO);
}
