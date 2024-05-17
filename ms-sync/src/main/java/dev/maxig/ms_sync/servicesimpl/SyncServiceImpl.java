package dev.maxig.ms_sync.servicesimpl;

import dev.maxig.ms_sync.entities.Stats;
import dev.maxig.ms_sync.repository.DynamoRepository;
import dev.maxig.ms_sync.repository.RedisRepository;
import dev.maxig.ms_sync.services.SyncService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class SyncServiceImpl implements SyncService {
    private final DynamoRepository dynamoRepository;
    private final RedisRepository redisRepository;

    public SyncServiceImpl(DynamoRepository dynamoRepository, RedisRepository redisRepository) {
        this.dynamoRepository = dynamoRepository;
        this.redisRepository = redisRepository;
    }

    @Override
    @Async("asyncExecutor")
    public void updateUrlCount(Object data, String traceId) {
        String shortUrl = (String) data;
        CompletableFuture.runAsync(() -> {
            try {
                dynamoRepository.updateUrlsCount(shortUrl);
            } catch (Exception e) {
                log.error("Failed to update URL count to DynamoDB, traceId {}, Error {}", traceId, e.getMessage());
            }
        });
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<String> syncCache() {
        CompletableFuture<String> future = new CompletableFuture<>();
        CompletableFuture.runAsync(() -> {
            try {
                dynamoRepository.syncCache();
                Stats globalStats = dynamoRepository.getStats();
                if (globalStats == null) {
                    log.error("Failed to get global stats from DynamoDB");
                }

                HashMap<String, String> hashStats = new HashMap<String, String>();
                hashStats.put("urlsCount", globalStats.getUrlsCount().toString());
                hashStats.put("urlsRedirect", globalStats.getUrlsRedirect().toString());

                redisRepository.saveGlobalStats(hashStats);

            } catch (Exception e) {
                log.error("Failed to sync cache to DynamoDB, Error: {}", e.getMessage());
            }
        });
        future.complete("Cache Synced to DynamoDB");
        return future;
    }


}
