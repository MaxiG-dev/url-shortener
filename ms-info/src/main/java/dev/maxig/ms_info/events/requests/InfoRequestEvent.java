package dev.maxig.ms_info.events.requests;

public record InfoRequestEvent(String operation, String traceId, String shortUrl) {
}
