package dev.maxig.ms_core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ConfigService {

    @Value("${config.dynamodb.configure}")
    private String dynamoDBConfigure;

    @Autowired
    private DynamoDBInitializer dynamoDBInitializer;

    @Bean
    public String config() {
        if ("true".equals(dynamoDBConfigure)) {
            String result = dynamoDBInitializer.createTable();
            System.out.println("DynamoDB table creation result: " + result);
        }
        return "Configured";
    }
}
