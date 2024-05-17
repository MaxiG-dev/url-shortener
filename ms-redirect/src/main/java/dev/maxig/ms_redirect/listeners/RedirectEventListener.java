package dev.maxig.ms_redirect.listeners;

import dev.maxig.ms_redirect.enums.RedirectOperationsEnum;
import dev.maxig.ms_redirect.events.requests.RedirectRequestEvent;
import dev.maxig.ms_redirect.events.responses.RedirectResponseEvent;
import dev.maxig.ms_redirect.services.PrometheusService;
import dev.maxig.ms_redirect.services.RedirectService;
import dev.maxig.ms_redirect.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class RedirectEventListener {

    @Value("${config.kafka-topics.redirect.response}")
    private String redirectResponseTopic;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RedirectService redirectService;
    private final PrometheusService prometheusService;

    public RedirectEventListener(KafkaTemplate<String, String> kafkaTemplate, RedirectService service, PrometheusService prometheusService) {
        this.kafkaTemplate = kafkaTemplate;
        this.redirectService = service;
        this.prometheusService = prometheusService;
    }

    @KafkaListener(topics = "${config.kafka-topics.redirect.request}", groupId = "${spring.kafka.consumer.group-id}")
    public void handleRedirect(String message) {
        RedirectRequestEvent redirectRequestEvent = JsonUtils.fromJson(message, RedirectRequestEvent.class);

        if (Objects.equals(redirectRequestEvent.operation(), RedirectOperationsEnum.GET_REDIRECT)) {
            CompletableFuture<String> urlFuture = redirectService.getLongUrl(redirectRequestEvent.shortUrl(), redirectRequestEvent.traceId());
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

        if (Objects.equals(redirectRequestEvent.operation(), RedirectOperationsEnum.GET_PROMETHEUS)) {
            CompletableFuture<String> prometheusFuture = prometheusService.getPrometheusMetrics();
            prometheusFuture.thenAccept(prometheusMetrics -> {
                RedirectResponseEvent redirectResponseEvent = new RedirectResponseEvent(prometheusMetrics, redirectRequestEvent.traceId());
                this.kafkaTemplate.send(redirectResponseTopic, redirectRequestEvent.traceId(), JsonUtils.toJson(redirectResponseEvent));
            }).exceptionally(ex -> {
                log.error("Failed to get Prometheus metrics", ex);
                RedirectResponseEvent redirectResponseEvent = new RedirectResponseEvent(ex.toString(), redirectRequestEvent.traceId());
                this.kafkaTemplate.send(redirectResponseTopic, redirectRequestEvent.traceId(), JsonUtils.toJson(redirectResponseEvent));
                return null;
            });
        }
    }

}
