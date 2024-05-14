package dev.maxig.api_gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApiKeyConfig {

    @Value("${config.application.x-api-key.ms-redirect}")
    private String msRedirectApiKey;

    @Bean
    public String msRedirectApiKey() {
        return msRedirectApiKey;
    }
}
