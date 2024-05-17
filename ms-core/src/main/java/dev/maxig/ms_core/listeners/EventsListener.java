package dev.maxig.ms_core.listeners;

import dev.maxig.ms_core.events.responses.*;
import dev.maxig.ms_core.services.EventService;
import dev.maxig.ms_core.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EventsListener {
    private final EventService eventService;

    public EventsListener(EventService eventService) {
        this.eventService = eventService;
    }

    @KafkaListener(topics = "${config.kafka-topics.redirect.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleRedirect(String message) {
        RedirectResponseEvent redirectResponseEvent = JsonUtils.fromJson(message, RedirectResponseEvent.class);
        eventService.completeRedirectRequest(redirectResponseEvent.traceId(), redirectResponseEvent.longUrl());
    }

    @KafkaListener(topics = "${config.kafka-topics.shorten.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShorten(String message) {
        ShortenResponseEvent shortenResponseEvent = JsonUtils.fromJson(message, ShortenResponseEvent.class);
        eventService.completeShortenRequest(shortenResponseEvent.traceId(), shortenResponseEvent.shortUrl());
    }

    @KafkaListener(topics = "${config.kafka-topics.info.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleInfo(String message) {
        InfoResponseEvent infoResponseEvent = JsonUtils.fromJson(message, InfoResponseEvent.class);
        eventService.completeInfoRequest(infoResponseEvent.traceId(), infoResponseEvent.data());
    }

    @KafkaListener(topics = "${config.kafka-topics.delete.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDelete(String message) {
        DeleteResponseEvent deleteResponseEvent = JsonUtils.fromJson(message, DeleteResponseEvent.class);
        eventService.completeDeleteRequest(deleteResponseEvent.traceId(), deleteResponseEvent.error());
    }

    @KafkaListener(topics = "${config.kafka-topics.sync.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSync(String message) {
        SyncResponseEvent syncResponseEvent = JsonUtils.fromJson(message, SyncResponseEvent.class);
        eventService.completeSyncRequest(syncResponseEvent.traceId(), syncResponseEvent.data());
    }
}
