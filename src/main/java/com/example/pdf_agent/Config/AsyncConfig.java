package com.example.pdf_agent.Config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 1) Config: bounded executor for streams
@Configuration
public class AsyncConfig {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService streamExecutor() {
        return Executors.newFixedThreadPool(20);
    }
}