package dev.maxig.ms_redirect.servicesimpl;

import dev.maxig.ms_redirect.enums.SyncOperationsEnum;
import dev.maxig.ms_redirect.events.requests.SyncRequestEvent;
import dev.maxig.ms_redirect.repository.DynamoRepository;
import dev.maxig.ms_redirect.repository.RedisRepository;
import dev.maxig.ms_redirect.services.RedirectService;
import dev.maxig.ms_redirect.utils.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class RedirectServiceImpl implements RedirectService {

    @Value("${config.kafka-topics.sync.request}")
    private String syncRequestTopic;

    private final DynamoRepository dynamoRepository;
    private final RedisRepository redisRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public RedirectServiceImpl(DynamoRepository dynamoRepository, RedisRepository redisRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.dynamoRepository = dynamoRepository;
        this.redisRepository = redisRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<String> getLongUrl(String shortUrl, String traceId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Object redisLongUrl = redisRepository.get(shortUrl);
            if (redisLongUrl != null) {
                future.complete(redisLongUrl.toString());
                updateUrlCount(shortUrl, traceId);
                return future;
            }

            Object notFoundUrl = redisRepository.getNotFoundUrl(shortUrl);
            if (notFoundUrl != null) {
                future.complete("404");
            }

            String DynamoUrl = dynamoRepository.getLongUrlFromDynamoDB(shortUrl);
            if (DynamoUrl != null) {
                future.complete(DynamoUrl);
                redisRepository.save(shortUrl, DynamoUrl);
                updateUrlCount(shortUrl, traceId);
                return future;
            }

            redisRepository.saveNotFoundUrl(shortUrl);
            future.complete("404");
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    protected void updateUrlCount(String shortUrl, String traceId) {
        try {
            SyncRequestEvent syncRequestEvent = new SyncRequestEvent(SyncOperationsEnum.UPDATE_URL_COUNT, shortUrl, traceId);
            this.kafkaTemplate.send(syncRequestTopic, traceId, JsonUtils.toJson(syncRequestEvent));
        } catch (Exception e) {
            log.warn("Failed to send message for URL count update: {}", e.getMessage());
        }
    }

}
