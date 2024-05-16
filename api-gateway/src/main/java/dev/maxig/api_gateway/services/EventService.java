package dev.maxig.api_gateway.services;

import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class EventService {
    private final Map<String, CompletableFuture<String>> pendingRedirectRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingShortenRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Object>> pendingInfoRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingDeleteRequests = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<String>> pendingPrometheusRequests = new ConcurrentHashMap<>();

    public CompletableFuture<String> createPendingRedirectRequest(String traceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingRedirectRequests.put(traceId, future);
        return future;
    }

    public void completeRedirectRequest(String traceId, String longUrl) {
        CompletableFuture<String> future = pendingRedirectRequests.remove(traceId);
        if (future != null) {
            pendingRedirectRequests.remove(traceId);
            future.complete(longUrl);
        }
    }

    public void removeRedirectRequest(String traceId) {
        pendingRedirectRequests.remove(traceId);
    }

    public CompletableFuture<String> createPendingShortenRequest(String traceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingShortenRequests.put(traceId, future);
        return future;
    }

    public void completeShortenRequest(String traceId, String shortUrl) {
        CompletableFuture<String> future = pendingShortenRequests.remove(traceId);
        if (future != null) {
            pendingShortenRequests.remove(traceId);
            future.complete(shortUrl);
        }
    }

    public void removeShortenRequest(String traceId) {
        pendingShortenRequests.remove(traceId);
    }

    public CompletableFuture<Object> createPendingInfoRequest(String traceId) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        pendingInfoRequests.put(traceId, future);
        return future;
    }

    public void completeInfoRequest(String traceId, Object response) {
        CompletableFuture<Object> future = pendingInfoRequests.remove(traceId);
        if (future != null) {
            pendingInfoRequests.remove(traceId);
            future.complete(response);
        }
    }

    public void removeInfoRequest(String traceId) {
        pendingInfoRequests.remove(traceId);
    }

    public CompletableFuture<String> createPendingDeleteRequest(String traceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingDeleteRequests.put(traceId, future);
        return future;
    }

    public void completeDeleteRequest(String traceId, String response) {
        CompletableFuture<String> future = pendingDeleteRequests.remove(traceId);
        if (future != null) {
            pendingDeleteRequests.remove(traceId);
            future.complete(response);
        }
    }

    public void removeDeleteRequest(String traceId) {
        pendingDeleteRequests.remove(traceId);
    }

    public CompletableFuture<String> createPendingPrometheusRequest(String traceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        pendingPrometheusRequests.put(traceId, future);
        return future;
    }

    public void completePrometheusRequest(String traceId, String response) {
        CompletableFuture<String> future = pendingPrometheusRequests.remove(traceId);
        if (future != null) {
            pendingPrometheusRequests.remove(traceId);
            future.complete(response);
        }
    }

    public void removePrometheusRequest(String traceId) {
        pendingPrometheusRequests.remove(traceId);
    }
}
