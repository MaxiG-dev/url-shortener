package dev.maxig.ms_core.events.requests;

public record ShortenRequestEvent(String operation,  String traceId, String longUrl, String userId) {
}
