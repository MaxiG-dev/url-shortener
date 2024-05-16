package dev.maxig.ms_delete.listeners;

import dev.maxig.ms_delete.events.requests.DeleteRequestEvent;
import dev.maxig.ms_delete.events.responses.DeleteResponseEvent;
import dev.maxig.ms_delete.services.DeleteService;
import dev.maxig.ms_delete.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class DeleteEventListener {

    @Value("${config.kafka-topics.delete.response}")
    private String deleteResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;

    DeleteService service;

    public DeleteEventListener(KafkaTemplate<String, String> kafkaTemplate, DeleteService service) {
        this.kafkaTemplate = kafkaTemplate;
        this.service = service;
    }

    @KafkaListener(topics = "${config.kafka-topics.delete.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleDelete(String message) {
        DeleteRequestEvent deleteRequestEvent = JsonUtils.fromJson(message, DeleteRequestEvent.class);

        CompletableFuture<String> urlFuture = service.deleteUrl(deleteRequestEvent.shortUrl());

        urlFuture.thenAccept(longUrl -> {
            DeleteResponseEvent deleteResponseEvent = new DeleteResponseEvent(null, deleteRequestEvent.traceId());
            this.kafkaTemplate.send(deleteResponseTopic, deleteRequestEvent.traceId(), JsonUtils.toJson(deleteResponseEvent));
        }).exceptionally(ex -> {
            log.error("Failed to delete URL for {}", deleteRequestEvent.shortUrl(), ex);
            DeleteResponseEvent deleteResponseEvent = new DeleteResponseEvent(ex.toString(), deleteRequestEvent.traceId());
            this.kafkaTemplate.send(deleteResponseTopic, deleteRequestEvent.traceId(), JsonUtils.toJson(deleteResponseEvent));
            return null;
        });
    }

}
