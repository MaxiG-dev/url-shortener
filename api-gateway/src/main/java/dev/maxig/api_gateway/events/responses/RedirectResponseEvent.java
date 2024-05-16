package dev.maxig.api_gateway.events.responses;

public record RedirectResponseEvent(String longUrl, String traceId) {
}
