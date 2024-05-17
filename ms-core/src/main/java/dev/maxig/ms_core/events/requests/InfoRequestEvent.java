package dev.maxig.ms_core.events.requests;

public record InfoRequestEvent(String operation, String traceId, String shortUrl) {
}
