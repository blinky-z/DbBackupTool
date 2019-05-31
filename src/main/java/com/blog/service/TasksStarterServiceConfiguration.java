package com.blog.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class TasksStarterServiceConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService tasksStarterExecutorService() {
        return Executors.newFixedThreadPool(100);
    }
}
