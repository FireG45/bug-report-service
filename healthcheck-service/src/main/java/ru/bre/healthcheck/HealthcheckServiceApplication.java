package ru.bre.healthcheck;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HealthcheckServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(HealthcheckServiceApplication.class, args);
    }
}
