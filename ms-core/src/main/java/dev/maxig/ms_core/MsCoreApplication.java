package dev.maxig.ms_core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class MsCoreApplication {
	public static void main(String[] args) {
		SpringApplication.run(MsCoreApplication.class, args);
	}

	@Bean
	public String test() {
		return "Test";
	}
}
