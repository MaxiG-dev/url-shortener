package dev.maxig.ms_info.services;

import java.util.concurrent.CompletableFuture;

public interface PrometheusService {
    CompletableFuture<String> getPrometheusMetrics();
}
