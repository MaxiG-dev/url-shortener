package dev.maxig.ms_redirect.services;

import java.util.concurrent.CompletableFuture;

public interface PrometheusService {
    CompletableFuture<String> getPrometheusMetrics();
}
