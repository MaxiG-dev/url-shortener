package dev.maxig.ms_delete.events.requests;

public record DeleteRequestEvent(String operation, String traceId, String shortUrl) {
}

