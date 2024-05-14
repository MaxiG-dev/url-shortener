package dev.maxig.ms_redirect.servicesimpl;

import dev.maxig.ms_redirect.repository.DynamoRepository;
import dev.maxig.ms_redirect.repository.RedisRepository;
import dev.maxig.ms_redirect.services.RedirectService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RedirectServiceImpl implements RedirectService {

    @Autowired
    private final DynamoRepository dynamoRepository;

    @Autowired
    private final RedisRepository redisRepository;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<String> getLongUrl(String shortUrl) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Object redisLongUrl = redisRepository.get(shortUrl);
            if (redisLongUrl != null) {
                future.complete(redisLongUrl.toString());
                updateUrlCount(shortUrl);
                return future;
            }

            Object notFoundUrl = redisRepository.getNotFoundUrl(shortUrl);
            if (notFoundUrl != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            String DynamoUrl = dynamoRepository.getLongUrlFromDynamoDB(shortUrl);
            if (DynamoUrl != null) {
                future.complete(DynamoUrl);
                redisRepository.save(shortUrl, DynamoUrl);
                updateUrlCount(shortUrl);
                return future;
            }

            redisRepository.saveNotFoundUrl(shortUrl);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Async("asyncExecutor")
    protected void updateUrlCount(String shortUrl) {
        CompletableFuture.runAsync(() -> {
            try {
                dynamoRepository.updateUrlCountFromDynamoDB(shortUrl);
            } catch (Exception e) {
                System.err.println("Failed to save URL to DynamoDB: " + e.getMessage());
            }
        });
    }
}
