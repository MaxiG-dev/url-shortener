package dev.maxig.ms_delete.services;

import java.util.concurrent.CompletableFuture;

public interface PrometheusService {
    CompletableFuture<String> getPrometheusMetrics();
}
