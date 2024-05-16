//package dev.maxig.api_gateway.config;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//
//@Configuration
//public class InstanceIdConfig {
//    @Value("${config.instance.id}")
//    private String instanceId;
//
//    @Bean
//    public String instanceId(Environment environment) {
//        System.setProperty("config.instance.id", String.valueOf(System.currentTimeMillis()/1000));
//        return instanceId;
//    }
//}
