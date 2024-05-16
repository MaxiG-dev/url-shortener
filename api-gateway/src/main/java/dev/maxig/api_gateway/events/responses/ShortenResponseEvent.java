package dev.maxig.api_gateway.events.responses;

public record ShortenResponseEvent(String shortUrl, String traceId) {
}
