package dev.maxig.ms_info.listeners;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;
import dev.maxig.ms_info.enums.InfoOperationsEnum;
import dev.maxig.ms_info.events.requests.InfoRequestEvent;
import dev.maxig.ms_info.events.responses.InfoResponseEvent;
import dev.maxig.ms_info.services.InfoService;
import dev.maxig.ms_info.services.PrometheusService;
import dev.maxig.ms_info.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class InfoEventListener {

    @Value("${config.kafka-topics.info.response}")
    private String infoResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final InfoService infoService;
    private final PrometheusService prometheusService;

    public InfoEventListener(KafkaTemplate<String, String> kafkaTemplate, InfoService infoService, PrometheusService prometheusService) {
        this.kafkaTemplate = kafkaTemplate;
        this.infoService = infoService;
        this.prometheusService = prometheusService;
    }

    @KafkaListener(topics = "info-request-topic", groupId = "info-listener")
    public void handleInfo(String message) {
        InfoRequestEvent infoRequestEvent = JsonUtils.fromJson(message, InfoRequestEvent.class);

        if (Objects.equals(infoRequestEvent.operation(), InfoOperationsEnum.GET_STATS)) {
            CompletableFuture<Stats> result = infoService.getGlobalStats();
            result.thenAccept(response -> {
                InfoResponseEvent infoResponseEvent = new InfoResponseEvent(response, infoRequestEvent.traceId());
                this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
            }).exceptionally(ex -> {
                log.error("Failed to get Stats", ex);
                InfoResponseEvent infoResponseEvent = new InfoResponseEvent(ex.toString(), infoRequestEvent.traceId());
                this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
                return null;
            });
        }

        if (Objects.equals(infoRequestEvent.operation(), InfoOperationsEnum.GET_URL)) {
            CompletableFuture<Url> result = infoService.getUrl(infoRequestEvent.shortUrl());
            result.thenAccept(longUrl -> {
                InfoResponseEvent infoResponseEvent = new InfoResponseEvent(longUrl, infoRequestEvent.traceId());
                this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
            }).exceptionally(ex -> {
                log.error("Failed to get long URL for shortId {}", infoRequestEvent.shortUrl(), ex);
                InfoResponseEvent infoResponseEvent = new InfoResponseEvent(ex.toString(), infoRequestEvent.traceId());
                this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
                return null;
            });
        }

        if (Objects.equals(infoRequestEvent.operation(), InfoOperationsEnum.GET_PROMETHEUS)) {
            CompletableFuture<String> prometheusFuture = prometheusService.getPrometheusMetrics();
            prometheusFuture.thenAccept(metrics -> {
                    InfoResponseEvent infoResponseEvent = new InfoResponseEvent(metrics, infoRequestEvent.traceId());
                    this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
                }).exceptionally(ex -> {
                    log.error("Failed to get Prometheus metrics", ex);
                    InfoResponseEvent infoResponseEvent = new InfoResponseEvent(ex.toString(), infoRequestEvent.traceId());
                    this.kafkaTemplate.send(infoResponseTopic, infoRequestEvent.traceId(), JsonUtils.toJson(infoResponseEvent));
                    return null;
                });
        }

    }

}
