package dev.maxig.api_gateway.events.requests;

public record ShortenRequestEvent(String longUrl, String userId, String traceId) {
}
