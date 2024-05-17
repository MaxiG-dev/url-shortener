package dev.maxig.ms_core.events.requests;

public record RedirectRequestEvent(String operation, String shortUrl, String traceId) {
}
