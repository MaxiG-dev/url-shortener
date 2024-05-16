package dev.maxig.ms_info.events.requests;

public record InfoRequestEvent(String action, String shortUrl, String traceId) {
}
