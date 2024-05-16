package dev.maxig.api_gateway.events.requests;

public record RedirectRequestEvent(String shortUrl, String traceId) {
}
