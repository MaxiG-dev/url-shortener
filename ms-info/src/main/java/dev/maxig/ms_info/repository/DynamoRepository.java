package dev.maxig.ms_info.repository;

import dev.maxig.ms_info.entities.Stats;
import dev.maxig.ms_info.entities.Url;
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

    public String getLongUrlFromDynamo(String shortUrl) {
        Url url = getUrlFromDynamo(shortUrl);
        return url != null ? url.getLongUrl() : null;
    }

    public Url getUrlFromDynamo(String shortUrl) {
        return getUrlFromDynamo(shortUrl, false);
    }

    public Url getUrlFromDynamo(String shortUrl, boolean getDeleted) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("shortId", AttributeValue.builder().s(shortUrl).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName("urls")
                .key(key)
                .build();

        GetItemResponse getItemRequest = dynamoDbClient.getItem(request);

        if (getItemRequest.item().isEmpty() || !getDeleted && !Objects.equals(getItemRequest.item().get("deletedAt").n(), "0")) {
            return null;
        }
        return Url.builder()
                .shortId(getItemRequest.item().get("shortId").s())
                .longUrl(getItemRequest.item().get("longUrl").s())
                .userId(getItemRequest.item().get("userId").s())
                .accessCount(Long.valueOf(getItemRequest.item().get("accessCount").n()))
                .createdAt(Long.valueOf(getItemRequest.item().get("createdAt").n()))
                .updatedAt(Long.valueOf(getItemRequest.item().get("updatedAt").n()))
                .deletedAt(Long.valueOf(getItemRequest.item().get("deletedAt").n()))
                .build();
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
