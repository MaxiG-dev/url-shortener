package dev.maxig.ms_sync.events.requests;

public record SyncRequestEvent(String operation, String data, String traceId) {
}
