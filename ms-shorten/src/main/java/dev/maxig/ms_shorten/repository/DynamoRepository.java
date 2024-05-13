package dev.maxig.ms_shorten.repository;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
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

    @Getter
    public static DynamoDbClient dynamoDbClient;

    @PostConstruct
    public void init() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, accessSecret);
        dynamoDbClient = DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDBEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    public void saveUrlToDynamoDB(String shortUrl, String longUrl, String userId) {
        Map<String, AttributeValue> item = new HashMap<>();
        item.put("shortId", AttributeValue.builder().s(shortUrl).build());
        item.put("longUrl", AttributeValue.builder().s(longUrl).build());
        item.put("userId", AttributeValue.builder().s(userId).build());
        item.put("accessCount", AttributeValue.builder().n("0").build());
        item.put("createdAt", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        item.put("updatedAt", AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build());
        item.put("deletedAt", AttributeValue.builder().n("0").build());

        try {
            dynamoDbClient.putItem(
                    PutItemRequest.builder()
                            .tableName("urls")
                            .item(item)
                            .conditionExpression("attribute_not_exists(shortId)")
                            .build()
            );
        } catch (ConditionalCheckFailedException ignored) {
        }
    }
}
