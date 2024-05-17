package dev.maxig.ms_sync.repository;

import dev.maxig.ms_sync.entities.Stats;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@Repository
public class DynamoRepository {

    @Value("${config.aws.access-key}")
    private String accessKey;

    @Value("${config.aws.secret-key}")
    private String accessSecret;

    @Value("${config.aws.region}")
    private String region;

    @Value("${config.dynamodb.endpoint}")
    private String dynamoDBEndpoint;

    @Value("${config.application.cache.save-urls-limit}")
    private int maxSaveUrlsCacheLimit;

    @Getter
    public static DynamoDbClient dynamoDbClient;

    @Autowired
    public RedisRepository redisRepository;

    @PostConstruct
    public void init() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, accessSecret);
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDBEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public void updateUrlsCount(String shortUrl) {
        int retryCount = 0;
        int delay = 0;
        while (retryCount <= 3) {
            try {
                Thread.sleep(delay);
                updateCounts(shortUrl);
                break;
            } catch (Exception e) {
                delay = (int) Math.pow(2, retryCount) * 100;

                retryCount++;
            }
        }
    }

    private void updateUrlCount(String shortUrl) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("shortId", AttributeValue.builder().s(shortUrl).build());

        UpdateItemRequest updateItemRequest = UpdateItemRequest.builder()
                .tableName("urls")
                .key(key)
                .updateExpression("ADD accessCount :val")
                .expressionAttributeValues(Map.of(":val", AttributeValue.builder().n("1").build()))
                .build();

        dynamoDbClient.updateItem(updateItemRequest);
    }

    public void updateCounts(String shortUrl) {
        Map<String, AttributeValue> urlKey = new HashMap<>();
        urlKey.put("shortId", AttributeValue.builder().s(shortUrl).build());
        Update updateUrlCount = Update.builder()
                .tableName("urls")
                .key(urlKey)
                .updateExpression("ADD accessCount :val")
                .expressionAttributeValues(Map.of(":val", AttributeValue.builder().n("1").build()))
                .build();


        Map<String, AttributeValue> statsKey = new HashMap<>();
        statsKey.put("statName", AttributeValue.builder().s("globalStats").build());
        Update updateRedirectsCount = Update.builder()
                .tableName("stats")
                .key(statsKey)
                .updateExpression("ADD urlsRedirect :val")
                .expressionAttributeValues(Map.of(":val", AttributeValue.builder().n("1").build()))
                .build();

        TransactWriteItemsRequest transactionRequest = TransactWriteItemsRequest.builder()
                .transactItems(TransactWriteItem.builder().update(updateUrlCount).build(),
                        TransactWriteItem.builder().update(updateRedirectsCount).build())
                .build();

        dynamoDbClient.transactWriteItems(transactionRequest);
    }

    public void syncCache() {
        Map<String, AttributeValue> lastEvaluatedKey = null;
        int totalCount = 0;
        do {
            int recordsToFetch = Math.min(1000, maxSaveUrlsCacheLimit - totalCount);
            ScanRequest scanRequest = ScanRequest.builder()
                    .tableName("urls")
                    .filterExpression("deletedAt = :zero")
                    .expressionAttributeValues(Map.of(
                            ":zero", AttributeValue.builder().n("0").build()
                    ))
                    .limit(recordsToFetch)
                    .exclusiveStartKey(lastEvaluatedKey)
                    .build();

            ScanResponse response = dynamoDbClient.scan(scanRequest);

            if (response.items().isEmpty()) {
                break;
            }

            for (Map<String, AttributeValue> item : response.items()) {
                if (totalCount >= maxSaveUrlsCacheLimit) {
                    break;
                }
                String shortUrl = item.get("shortId").s();
                String longUrl = item.get("longUrl").s();
                redisRepository.saveCacheUrl(shortUrl, longUrl);
                totalCount++;
            }
            lastEvaluatedKey = response.lastEvaluatedKey();
            if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
                break;
            }
        } while (totalCount < maxSaveUrlsCacheLimit);
    }

    public Stats getStats() {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("statName", AttributeValue.builder().s("globalStats").build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName("stats")
                .key(key)
                .build();

        GetItemResponse getItemRequest = dynamoDbClient.getItem(request);

        if (getItemRequest.item().isEmpty()) {
            return null;
        }

        return Stats.builder()
                .urlsCount(Long.valueOf(getItemRequest.item().get("urlsCount").n()))
                .urlsRedirect(Long.valueOf(getItemRequest.item().get("urlsRedirect").n()))
                .build();
    }

}
