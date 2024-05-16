package dev.maxig.ms_redirect.listeners;

import dev.maxig.ms_redirect.events.requests.RedirectRequestEvent;
import dev.maxig.ms_redirect.events.responses.RedirectResponseEvent;
import dev.maxig.ms_redirect.services.RedirectService;
import dev.maxig.ms_redirect.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RedirectEventListener {

    @Value("${config.kafka-topics.redirect.response}")
    private String redirectResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    RedirectService service;

    public RedirectEventListener(KafkaTemplate<String, String> kafkaTemplate, RedirectService service) {
        this.kafkaTemplate = kafkaTemplate;
        this.service = service;
    }

    @KafkaListener(topics = "${config.kafka-topics.redirect.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleRedirect(String message) {
        RedirectRequestEvent redirectRequestEvent = JsonUtils.fromJson(message, RedirectRequestEvent.class);

        CompletableFuture<String> urlFuture = service.getLongUrl(redirectRequestEvent.shortUrl());

        urlFuture.thenAccept(longUrl -> {
            RedirectResponseEvent redirectResponseEvent = new RedirectResponseEvent(longUrl, redirectRequestEvent.traceId());
            this.kafkaTemplate.send(redirectResponseTopic, redirectRequestEvent.traceId(), JsonUtils.toJson(redirectResponseEvent));
        }).exceptionally(ex -> {
            log.error("Failed to get long URL for shortId {}", redirectRequestEvent.shortUrl(), ex);
            RedirectResponseEvent redirectResponseEvent = new RedirectResponseEvent(ex.toString(), redirectRequestEvent.traceId());
            this.kafkaTemplate.send(redirectResponseTopic, redirectRequestEvent.traceId(), JsonUtils.toJson(redirectResponseEvent));
            return null;
        });
    }

}
