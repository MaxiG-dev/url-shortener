package dev.maxig.ms_redirect.events.requests;

public record RedirectRequestEvent(String operation, String shortUrl, String traceId) {
}
