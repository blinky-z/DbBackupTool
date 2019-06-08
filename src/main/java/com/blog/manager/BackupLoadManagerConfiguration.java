package com.blog.manager;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class BackupLoadManagerConfiguration {

    @Bean(destroyMethod = "shutdown")
    public ExecutorService backupLoadManagerExecutorService() {
        return Executors.newFixedThreadPool(40);
    }
}
