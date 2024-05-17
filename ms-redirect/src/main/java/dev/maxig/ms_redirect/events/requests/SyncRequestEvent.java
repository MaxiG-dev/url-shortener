package dev.maxig.ms_redirect.events.requests;

public record SyncRequestEvent(String operation, String data, String traceId) {
}
