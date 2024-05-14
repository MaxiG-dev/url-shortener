package dev.maxig.ms_core.config;

import dev.maxig.ms_core.repository.DynamoRepository;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Component
public class DynamoDBInitializer {

    @Value("${config.aws.access-key}")
    private String accessKey;

    @Value("${config.aws.secret-key}")
    private String accessSecret;

    @Value("${config.aws.region}")
    private String region;

    @Value("${config.dynamodb.endpoint}")
    private String dynamoDBEndpoint;

    @Value("${config.dynamodb.tables.urls.name}")
    private String urlsTableName;

    @Value("${config.dynamodb.tables.urls.read}")
    private Long urlsReadCapacity;

    @Value("${config.dynamodb.tables.urls.read}")
    private Long urlsWriteCapacity;

    @Value("${config.dynamodb.tables.urls.indexes.createdAt.read}")
    private Long urlsCreatedAtReadCapacity;

    @Value("${config.dynamodb.tables.urls.indexes.createdAt.write}")
    private Long urlsCreatedAtWriteCapacity;

    @Value("${config.dynamodb.tables.urls.indexes.userid.read}")
    private Long urlsUserIdReadCapacity;

    @Value("${config.dynamodb.tables.urls.indexes.userid.write}")
    private Long urlsUserIdWriteCapacity;

    @Value("${config.dynamodb.tables.users.name}")
    private String usersTableName;

    @Value("${config.dynamodb.tables.users.read}")
    private Long usersReadCapacity;

    @Value("${config.dynamodb.tables.users.read}")
    private Long usersWriteCapacity;

    @Value("${config.dynamodb.tables.stats.name}")
    private String statsTableName;

    @Value("${config.dynamodb.tables.stats.read}")
    private Long statsReadCapacity;

    @Value("${config.dynamodb.tables.stats.write}")
    private Long statsWriteCapacity;

    @Value("${config.application.cache.save-urls-limit}")
    private int saveUrlsCacheLimit;

    @Getter
    public static DynamoDbClient client;

    @Autowired
    DynamoRepository dynamoRepository;

    @PostConstruct
    public void init() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accessKey, accessSecret);
        client = DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDBEndpoint))
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
    }

    @Bean
    public String createTables() {
        List<String> tablesResult = new ArrayList<>();

        tablesResult.add(createTableUrls());
        tablesResult.add(createTableUsers());
        tablesResult.add(createTableStats());

        return tablesResult.toString();
    }

    @Bean
    public String createTableUrls() {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(urlsTableName)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("shortId")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("shortId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("userId")
                                .attributeType(ScalarAttributeType.S)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("createdAt")
                                .attributeType(ScalarAttributeType.N)
                                .build(),
                        AttributeDefinition.builder()
                                .attributeName("deletedAt")
                                .attributeType(ScalarAttributeType.N)
                                .build())
                .billingMode(BillingMode.PROVISIONED)
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(urlsReadCapacity)
                        .writeCapacityUnits(urlsWriteCapacity)
                        .build())
                .globalSecondaryIndexes(
                        GlobalSecondaryIndex.builder()
                                .indexName("UserIdIndex")
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("userId")
                                        .keyType(KeyType.HASH)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.ALL)
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(urlsUserIdReadCapacity)
                                        .writeCapacityUnits(urlsUserIdWriteCapacity)
                                        .build())
                                .build(),
                        GlobalSecondaryIndex.builder()
                                .indexName("CreatedAtIndex")
                                .keySchema(KeySchemaElement.builder()
                                        .attributeName("shortId")  // This should be the hash key
                                        .keyType(KeyType.HASH)
                                        .build(),
                                        KeySchemaElement.builder()
                                        .attributeName("createdAt")
                                        .keyType(KeyType.RANGE)
                                        .build())
                                .projection(Projection.builder()
                                        .projectionType(ProjectionType.INCLUDE)
                                        .nonKeyAttributes("shortId", "longUrl", "deletedAt")
                                        .build())
                                .provisionedThroughput(ProvisionedThroughput.builder()
                                        .readCapacityUnits(urlsCreatedAtReadCapacity)
                                        .writeCapacityUnits(urlsCreatedAtWriteCapacity)
                                        .build())
                                .build())
                .build();

        try {
            CreateTableResponse response = client.createTable(request);
            System.out.println("Table " + urlsTableName + " created successfully. Table status: " + response.tableDescription().tableStatus());
            return "Table " + urlsTableName + " created successfully. Table status: " + response.tableDescription().tableStatus();
        } catch (DynamoDbException e) {
            System.err.println("Unable to create table "  + urlsTableName + ": " + e.getMessage());
            return "Unable to create table "  + urlsTableName + ": " + e.getMessage();
        }
    }
    @Bean
    public String createTableUsers() {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(usersTableName)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("email")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("email")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .billingMode(BillingMode.PROVISIONED)
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(usersReadCapacity)
                        .writeCapacityUnits(usersWriteCapacity)
                        .build())
                .build();

        try {
            CreateTableResponse response = client.createTable(request);
            System.out.println("Table " + usersTableName + " created successfully. Table status: " + response.tableDescription().tableStatus());
            return "Table " + usersTableName + " created successfully. Table status: " + response.tableDescription().tableStatus();
        } catch (DynamoDbException e) {
            System.err.println("Unable to create table "  + usersTableName + ": " + e.getMessage());
            return "Unable to create table "  + usersTableName + ": " + e.getMessage();
        }
    }
    @Bean
    public String createTableStats() {
        CreateTableRequest request = CreateTableRequest.builder()
                .tableName(statsTableName)
                .keySchema(KeySchemaElement.builder()
                        .attributeName("statName")
                        .keyType(KeyType.HASH)
                        .build())
                .attributeDefinitions(
                        AttributeDefinition.builder()
                                .attributeName("statName")
                                .attributeType(ScalarAttributeType.S)
                                .build())
                .billingMode(BillingMode.PROVISIONED)
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(statsReadCapacity)
                        .writeCapacityUnits(statsWriteCapacity)
                        .build())
                .build();

        try {
            CreateTableResponse response = client.createTable(request);
            System.out.println("Table " + statsTableName + " created successfully. Table status: " + response.tableDescription().tableStatus());
            return "Table " + statsTableName + " created successfully. Table status: " + response.tableDescription().tableStatus();
        } catch (DynamoDbException e) {
            System.err.println("Unable to create table "  + statsTableName + ": " + e.getMessage());
            return "Unable to create table "  + statsTableName + ": " + e.getMessage();
        }
    }

    public void loadInCache() {
        dynamoRepository.getAllUrls(client, saveUrlsCacheLimit);
    }

}
