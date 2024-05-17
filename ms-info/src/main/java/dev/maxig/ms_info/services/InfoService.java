package dev.maxig.ms_info.services;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;

import java.util.concurrent.CompletableFuture;

public interface InfoService {
    CompletableFuture<Stats> getGlobalStats();
    CompletableFuture<Url> getUrl(String shortId);
}
