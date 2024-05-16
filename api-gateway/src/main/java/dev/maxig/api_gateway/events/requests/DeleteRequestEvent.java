package dev.maxig.api_gateway.events.requests;

public record DeleteRequestEvent(String shortUrl, String traceId) {
}
