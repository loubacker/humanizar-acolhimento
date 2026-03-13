package com.humanizar.acolhimento.infrastructure.config;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VirtualThreadsConfig {

    @Bean(destroyMethod = "close")
    public ExecutorService virtualThreadExecutorService() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean("inboundExecutor")
    public Executor inboundExecutor(ExecutorService virtualThreadExecutorService) {
        return virtualThreadExecutorService;
    }

    @Bean("outboxExecutor")
    public Executor outboxExecutor(ExecutorService virtualThreadExecutorService) {
        return virtualThreadExecutorService;
    }
}
