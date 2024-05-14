package dev.maxig.ms_core.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.Map;


@Repository
public class DynamoRepository {

    @Autowired
    public RedisRepository redisRepository;

    public void getAllUrls(DynamoDbClient dynamoDbClient, int maxRecords) {
        Map<String, AttributeValue> lastEvaluatedKey = null;
        int totalCount = 0;
        do {
            int recordsToFetch = Math.min(1000, maxRecords - totalCount);
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
                if (totalCount >= maxRecords) {
                    break;
                }
                String shortUrl = item.get("shortId").s();
                String longUrl = item.get("longUrl").s();
                redisRepository.save(shortUrl, longUrl);
                totalCount++;
            }
            lastEvaluatedKey = response.lastEvaluatedKey();
            if (lastEvaluatedKey == null || lastEvaluatedKey.isEmpty()) {
                break;
            }
        } while (totalCount < maxRecords);
    }

}
