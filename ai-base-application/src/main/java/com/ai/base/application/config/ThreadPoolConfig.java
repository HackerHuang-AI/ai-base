package com.ai.base.application.config;

import jakarta.annotation.PreDestroy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {
    private final ExecutorService defaultExecutor = Executors.newFixedThreadPool(10);

    @Bean("defaultExecutor")
    public ExecutorService defaultExecutor() {
        return defaultExecutor;
    }

    @PreDestroy
    public void shutdown() {
        defaultExecutor.shutdown();
    }
}

