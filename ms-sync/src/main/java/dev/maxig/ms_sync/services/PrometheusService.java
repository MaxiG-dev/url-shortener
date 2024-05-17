package dev.maxig.ms_sync.services;

import java.util.concurrent.CompletableFuture;

public interface PrometheusService {
    CompletableFuture<String> getPrometheusMetrics();
}
