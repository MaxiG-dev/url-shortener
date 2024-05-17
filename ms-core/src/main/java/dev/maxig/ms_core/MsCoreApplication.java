package dev.maxig.ms_core;

import io.micrometer.observation.annotation.Observed;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Observed
@SpringBootApplication
public class MsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsCoreApplication.class, args);
    }

}
