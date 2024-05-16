package dev.maxig.ms_shorten.listeners;

import dev.maxig.ms_shorten.dto.CreateUrlDTO;
import dev.maxig.ms_shorten.events.requests.ShortenRequestEvent;
import dev.maxig.ms_shorten.events.responses.ShortenResponseEvent;
import dev.maxig.ms_shorten.services.ShortenService;
import dev.maxig.ms_shorten.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ShortenEventListener {

    @Value("${config.kafka-topics.shorten.response}")
    private String shortenResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    ShortenService service;

    public ShortenEventListener(KafkaTemplate<String, String> kafkaTemplate, ShortenService service) {
        this.kafkaTemplate = kafkaTemplate;
        this.service = service;
    }

    @KafkaListener(topics = "${config.kafka-topics.shorten.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleShorten(String message) {
        ShortenRequestEvent shortenRequestEvent = JsonUtils.fromJson(message, ShortenRequestEvent.class);

        CompletableFuture<String> urlFuture = service.create(new CreateUrlDTO(shortenRequestEvent.longUrl(), shortenRequestEvent.userId()));

        urlFuture.thenAccept(longUrl -> {
            ShortenResponseEvent shortenResponseEvent = new ShortenResponseEvent(longUrl, shortenRequestEvent.traceId());
            this.kafkaTemplate.send(shortenResponseTopic, shortenRequestEvent.traceId(), JsonUtils.toJson(shortenResponseEvent));
        }).exceptionally(ex -> {
            log.error("Failed to create short URL for {}", shortenRequestEvent.longUrl(), ex);
            ShortenResponseEvent deleteResponseEvent = new ShortenResponseEvent(ex.getMessage(), shortenRequestEvent.traceId());
            this.kafkaTemplate.send(shortenResponseTopic, shortenRequestEvent.traceId(), JsonUtils.toJson(deleteResponseEvent));
            return null;
        });
    }

}
