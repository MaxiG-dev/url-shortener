package dev.maxig.ms_shorten.events.requests;

public record ShortenRequestEvent(String operation,  String traceId, String longUrl, String userId) {
}