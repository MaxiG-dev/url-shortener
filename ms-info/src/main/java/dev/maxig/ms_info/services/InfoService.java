package dev.maxig.ms_info.services;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;

import java.util.concurrent.CompletableFuture;

public interface InfoService {
//    CompletableFuture<String> getLongUrl(String shortId);
    CompletableFuture<Stats> getGlobalStats();
    CompletableFuture<Url> getUrl(String shortId);
//    CompletableFuture<List<Url>> getAllUrls(String shortUrl, boolean getDeletedUrls);
//    CompletableFuture<List<Url>> getUserUrls(String userId, boolean getDeletedUrls);
}
