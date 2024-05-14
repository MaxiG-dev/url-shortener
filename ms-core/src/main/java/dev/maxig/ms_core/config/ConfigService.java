package dev.maxig.ms_core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigService {

    @Value("${config.dynamodb.configure}")
    private String dynamoDBConfigure;

    @Value("${config.application.cache.load-urls-in-cache}")
    private String loadUrlsInCache;

    @Autowired
    private DynamoDBInitializer dynamoDBInitializer;

    @Bean
    public String config() {
        if ("true".equals(dynamoDBConfigure)) {
            String result = dynamoDBInitializer.createTables();
            System.out.println("DynamoDB table creation result: " + result);
        } else {
            System.out.println("DynamoDB table creation is disabled");
        }

        if ("true".equals(loadUrlsInCache)) {
            dynamoDBInitializer.loadInCache();
            System.out.println("Urls loaded in cache");
        } else {
            System.out.println("Loading urls in cache is disabled");
        }
        return "Configured";
    }
}
