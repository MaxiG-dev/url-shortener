package dev.maxig.ms_info.servicesimpl;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;
import dev.maxig.ms_info.repository.DynamoRepository;
import dev.maxig.ms_info.repository.RedisRepository;
import dev.maxig.ms_info.services.InfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class InfoServiceImpl implements InfoService {

    @Autowired
    private final DynamoRepository dynamoRepository;

    @Autowired
    private final RedisRepository redisRepository;

    @Override
    @Async("asyncExecutor")
    public CompletableFuture<String> getLongUrl(String shortId) {
        CompletableFuture<String> future = new CompletableFuture<>();
        try {

            Object redisUrl = redisRepository.get(shortId);
            if (redisUrl != null) {
                future.complete(redisUrl.toString());
                return future;
            }

            Object notFoundUrl = redisRepository.getNotFoundUrl(shortId);
            if (notFoundUrl != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            String dynamoLongUrl = dynamoRepository.getLongUrlFromDynamo(shortId);
            if (dynamoLongUrl == null) {
                redisRepository.saveNotFoundUrl(shortId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }
            future.complete(dynamoLongUrl);
            redisRepository.save(shortId, dynamoLongUrl);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }


    @Override
    public CompletableFuture<Stats> getGlobalStats() {
        CompletableFuture<Stats> future = new CompletableFuture<>();
        try {
            Stats globalStats = dynamoRepository.getStats();
            if (globalStats == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stats not found");
            }
            future.complete(globalStats);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<Url> getUrl(String shortId) {
        CompletableFuture<Url> future = new CompletableFuture<>();
        try{
            Object notFoundUrl = redisRepository.getNotFoundUrl(shortId);
            if (notFoundUrl != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            Url dynamoUrl = dynamoRepository.getUrlFromDynamo(shortId);
            if (dynamoUrl == null) {
                redisRepository.saveNotFoundUrl(shortId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            future.complete(dynamoUrl);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    @Override
    public CompletableFuture<List<Url>> getAllUrls(String shortUrl, boolean getDeletedUrls) {
        return null;
    }

    @Override
    public CompletableFuture<List<Url>> getUserUrls(String userId, boolean getDeletedUrls) {
        return null;
    }


}
