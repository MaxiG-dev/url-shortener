package dev.maxig.api_gateway.listeners;

import dev.maxig.api_gateway.events.responses.*;
import dev.maxig.api_gateway.services.EventService;
import dev.maxig.api_gateway.utils.JsonUtils;
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
        log.info("Received info event: shortUrl {}, traceId {}", infoResponseEvent.data(), infoResponseEvent.traceId());
        eventService.completeInfoRequest(infoResponseEvent.traceId(), infoResponseEvent.data());
    }

    @KafkaListener(topics = "${config.kafka-topics.delete.response}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDelete(String message) {
        DeleteResponseEvent deleteResponseEvent = JsonUtils.fromJson(message, DeleteResponseEvent.class);
        eventService.completeDeleteRequest(deleteResponseEvent.traceId(), deleteResponseEvent.error());
    }

    @KafkaListener(topics = "prometheus-response-topic", groupId = "prometheus-listener" + "${spring.kafka.consumer.group-id}")
    public void handlePrometheusRequest(String message) {
        PrometheusResponseEvent prometheusResponseEvent = JsonUtils.fromJson(message, PrometheusResponseEvent.class);
        log.info("Received Prometheus request: requestId {}", prometheusResponseEvent.traceId());
        eventService.completePrometheusRequest(prometheusResponseEvent.traceId(), prometheusResponseEvent.data());

    }
}
