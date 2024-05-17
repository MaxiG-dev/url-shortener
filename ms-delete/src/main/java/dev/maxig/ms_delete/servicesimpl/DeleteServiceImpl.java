package dev.maxig.ms_delete.servicesimpl;

import dev.maxig.ms_delete.repository.DynamoRepository;
import dev.maxig.ms_delete.repository.RedisRepository;
import dev.maxig.ms_delete.services.DeleteService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

@Service
public class DeleteServiceImpl implements DeleteService {

    private final DynamoRepository dynamoRepository;
    private final RedisRepository redisRepository;

    public DeleteServiceImpl(DynamoRepository dynamoRepository, RedisRepository redisRepository) {
        this.dynamoRepository = dynamoRepository;
        this.redisRepository = redisRepository;
    }

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<String> deleteUrl(String shortUrl) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {
            Object notFoundUrl = redisRepository.getNotFoundUrl(shortUrl);
            if (notFoundUrl != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            boolean urlExist = dynamoRepository.deleteUrlFromDynamoDB(shortUrl);

            if (!urlExist) {
                redisRepository.saveNotFoundUrl(shortUrl);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            redisRepository.delete(shortUrl);
            redisRepository.saveNotFoundUrl(shortUrl);
            future.complete(null);

        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }
}
