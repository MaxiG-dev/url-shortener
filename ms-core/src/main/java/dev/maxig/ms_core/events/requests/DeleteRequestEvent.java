package dev.maxig.ms_core.events.requests;

public record DeleteRequestEvent(String operation, String traceId, String shortUrl) {
}
