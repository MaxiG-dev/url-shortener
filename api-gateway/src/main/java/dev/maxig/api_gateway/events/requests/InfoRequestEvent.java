package dev.maxig.api_gateway.events.requests;

public record InfoRequestEvent(String action, String shortUrl, String traceId) {
}

