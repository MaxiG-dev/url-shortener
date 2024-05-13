package dev.maxig.ms_delete.repository;

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

    public boolean deleteUrlFromDynamoDB(String shortUrl) {
        Map<String, AttributeValue> key = new HashMap<>();
        key.put("shortId", AttributeValue.builder().s(shortUrl).build());

        GetItemRequest request = GetItemRequest.builder()
                .tableName("urls")
                .key(key)
                .build();

        GetItemResponse getItemRequest = dynamoDbClient.getItem(request);

        if (getItemRequest.item().isEmpty() || !Objects.equals(getItemRequest.item().get("deletedAt").n(), "0")) {
            return false;
        }

        Map<String, AttributeValueUpdate> attributeUpdates = new HashMap<>();
        attributeUpdates.put("deletedAt", AttributeValueUpdate.builder().value(AttributeValue.builder().n(String.valueOf(System.currentTimeMillis())).build()).build());

       dynamoDbClient.updateItem(
                UpdateItemRequest.builder()
                        .tableName("urls")
                        .key(key)
                        .attributeUpdates(attributeUpdates)
                        .build()
        );
        return true;
    }
}
