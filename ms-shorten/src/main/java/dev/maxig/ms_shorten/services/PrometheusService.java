package dev.maxig.ms_shorten.services;

import java.util.concurrent.CompletableFuture;

public interface PrometheusService {
    CompletableFuture<String> getPrometheusMetrics();
}
