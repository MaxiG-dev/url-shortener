package dev.maxig.ms_sync.services;

import java.util.concurrent.CompletableFuture;

public interface SyncService {
    void updateUrlCount(Object data, String traceId);
    CompletableFuture<String> syncCache();
}