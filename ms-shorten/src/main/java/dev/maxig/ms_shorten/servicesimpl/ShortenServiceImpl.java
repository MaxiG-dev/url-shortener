package dev.maxig.ms_shorten.servicesimpl;

import dev.maxig.ms_shorten.dto.CreateUrlDTO;
import dev.maxig.ms_shorten.repository.DynamoRepository;
import dev.maxig.ms_shorten.repository.RedisRepository;
import dev.maxig.ms_shorten.services.ShortenService;
import dev.maxig.ms_shorten.utils.ShortenUrl;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class ShortenServiceImpl implements ShortenService {
        @Value("${config.url}")
        private String redirectUrl;

        @Autowired
        private final DynamoRepository dynamoRepository;

        @Autowired
        private final RedisRepository redisRepository;

        @Autowired
        private final ShortenUrl shortenUrl;

        @Override
        @Async("asyncExecutor")
        public CompletableFuture<String> create(CreateUrlDTO createUrlDTO) {
            CompletableFuture<String> future = new CompletableFuture<>();
            try {
                if (!createUrlDTO.getLongUrl().matches("^(https?):\\/\\/(?:(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,6})(?:\\/\\S*)?$")) {
                    throw new BadRequestException("Invalid URL");
                }
                if (createUrlDTO.getUserId().isEmpty()) {
                    throw new BadRequestException("Invalid User ID");
                }

                String shortUrl = shortenUrl.generate();
                String longUrl = createUrlDTO.getLongUrl();
                String userId = createUrlDTO.getUserId();

                Object urlInRedis = redisRepository.get(shortUrl);

                while (urlInRedis != null) {
                    shortUrl = shortenUrl.generate();
                    urlInRedis = redisRepository.get(shortUrl);
                }

                boolean savedUrlSuccess = dynamoRepository.saveUrlToDynamoDB(shortUrl, longUrl, userId);

                while (!savedUrlSuccess) {
                    shortUrl = shortenUrl.generate();
                    savedUrlSuccess = dynamoRepository.saveUrlToDynamoDB(shortUrl, longUrl, userId);
                }

                redisRepository.save(shortUrl, longUrl);
                redisRepository.deleteFromNotFoundedUrls(shortUrl);

                future.complete(redirectUrl + "/" + shortUrl);

                updateStats();
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
            return future;
        }

        @Async("asyncExecutor")
        protected void updateStats() {
            CompletableFuture.runAsync(() -> {
                try {
                    dynamoRepository.updateStats();
                } catch (Exception e) {
                    System.err.println("Failed to update stats: " + e.getMessage());
                }
            });
        }
}
