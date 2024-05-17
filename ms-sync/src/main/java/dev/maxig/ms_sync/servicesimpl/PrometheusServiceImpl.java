package dev.maxig.ms_sync.servicesimpl;

import dev.maxig.ms_sync.services.PrometheusService;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class PrometheusServiceImpl implements PrometheusService {

    private final PrometheusMeterRegistry prometheusMeterRegistry;

    public PrometheusServiceImpl(PrometheusMeterRegistry prometheusMeterRegistry) {
        this.prometheusMeterRegistry = prometheusMeterRegistry;
    }

    @Async("asyncExecutor")
    public CompletableFuture<String> getPrometheusMetrics() {
        return CompletableFuture.completedFuture(prometheusMeterRegistry.scrape());
    }
}
