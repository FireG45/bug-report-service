package ru.bre;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@EnableScheduling
@SpringBootApplication
public class SummaryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SummaryServiceApplication.class, args);
    }

    @Bean
    public Executor jobExecutor() {
        return Executors.newFixedThreadPool(1);
    }

    @Bean
    public Executor threadPool() {
        return Executors.newCachedThreadPool();
    }

}
