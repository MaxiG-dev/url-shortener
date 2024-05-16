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

import java.util.HashMap;
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
    public CompletableFuture<Url> getUrl(String shortId) {
        CompletableFuture<Url> future = new CompletableFuture<>();
        try {

            Url redisUrl = redisRepository.getCompleteUrl(shortId);
            if (redisUrl != null) {
                future.complete(redisUrl);
                return future;
            }

            Object notFoundUrl = redisRepository.getNotFoundUrl(shortId);
            if (notFoundUrl != null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            Url dynamoUrl = dynamoRepository.getUrlFromDynamo(shortId);
            if (dynamoUrl == null) {
                redisRepository.saveNotFoundUrl(shortId);
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "URL not found");
            }

            redisRepository.saveCompleteUrl(shortId, dynamoUrl);
            future.complete(dynamoUrl);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }


    @Override
    @Async("asyncExecutor")
    public CompletableFuture<Stats> getGlobalStats() {
        CompletableFuture<Stats> future = new CompletableFuture<>();
        try {
            Stats redisStats = redisRepository.getGlobalStats();
            if (redisStats != null) {
                future.complete(redisStats);
                return future;
            }

            Stats globalStats = dynamoRepository.getStats();
            if (globalStats == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Stats not found");
            }

            HashMap<String, String> hashStats = new HashMap<String, String>();
            hashStats.put("urlsCount", globalStats.getUrlsCount().toString());
            hashStats.put("urlsRedirect", globalStats.getUrlsRedirect().toString());

            redisRepository.saveGlobalStats(hashStats);
            future.complete(globalStats);
        } catch (Exception e) {
            future.completeExceptionally(e);
        }
        return future;
    }

}
