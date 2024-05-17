package dev.maxig.ms_sync.listeners;

import dev.maxig.ms_sync.enums.SyncOperationsEnum;
import dev.maxig.ms_sync.events.requests.SyncRequestEvent;
import dev.maxig.ms_sync.events.responses.SyncResponseEvent;
import dev.maxig.ms_sync.services.PrometheusService;
import dev.maxig.ms_sync.services.SyncService;
import dev.maxig.ms_sync.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class SyncEventListener {

    @Value("${config.kafka-topics.sync.response}")
    private String syncResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SyncService syncService;
    private final PrometheusService prometheusService;

    public SyncEventListener(KafkaTemplate<String, String> kafkaTemplate, SyncService service, PrometheusService prometheusService) {
        this.kafkaTemplate = kafkaTemplate;
        this.syncService = service;

        this.prometheusService = prometheusService;
    }

    @KafkaListener(topics = "${config.kafka-topics.sync.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleSync(String message) {
        SyncRequestEvent syncRequestEvent = JsonUtils.fromJson(message, SyncRequestEvent.class);
        String operation = syncRequestEvent.operation();

        if (operation.equals(SyncOperationsEnum.UPDATE_URL_COUNT)) {
            syncService.updateUrlCount(syncRequestEvent.data(), syncRequestEvent.traceId());
        }

        if (Objects.equals(syncRequestEvent.operation(), SyncOperationsEnum.GET_PROMETHEUS)) {
            CompletableFuture<String> prometheusFuture = prometheusService.getPrometheusMetrics();
            prometheusFuture.thenAccept(prometheusMetrics -> {
                SyncResponseEvent syncResponseEvent = new SyncResponseEvent(prometheusMetrics, syncRequestEvent.traceId());
                this.kafkaTemplate.send(syncResponseTopic, syncRequestEvent.traceId(), JsonUtils.toJson(syncResponseEvent));
            }).exceptionally(ex -> {
                log.error("Failed to get Prometheus metrics", ex);
                SyncResponseEvent syncResponseEvent = new SyncResponseEvent(ex.toString(), syncRequestEvent.traceId());
                this.kafkaTemplate.send(syncResponseTopic, syncRequestEvent.traceId(), JsonUtils.toJson(syncResponseEvent));
                return null;
            });
        }

        if (Objects.equals(syncRequestEvent.operation(), SyncOperationsEnum.SYNC_CACHE)) {
            CompletableFuture<String> syncResult = syncService.syncCache();
            syncResult.thenAccept(prometheusMetrics -> {
                SyncResponseEvent syncResponseEvent = new SyncResponseEvent(prometheusMetrics, syncRequestEvent.traceId());
                this.kafkaTemplate.send(syncResponseTopic, syncRequestEvent.traceId(), JsonUtils.toJson(syncResponseEvent));
            }).exceptionally(ex -> {
                log.error("Failed to get Prometheus metrics", ex);
                SyncResponseEvent syncResponseEvent = new SyncResponseEvent(ex.toString(), syncRequestEvent.traceId());
                this.kafkaTemplate.send(syncResponseTopic, syncRequestEvent.traceId(), JsonUtils.toJson(syncResponseEvent));
                return null;
            });
        }

    }

}
