package dev.maxig.ms_shorten.events.requests;

public record ShortenRequestEvent(String longUrl, String userId, String traceId) {
}
