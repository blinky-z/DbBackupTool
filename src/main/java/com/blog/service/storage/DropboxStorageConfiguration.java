package com.blog.service.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class DropboxStorageConfiguration {
    @Bean(destroyMethod = "shutdown")
    public ExecutorService dropboxExecutorService() {
        return Executors.newFixedThreadPool(20);
    }
}
