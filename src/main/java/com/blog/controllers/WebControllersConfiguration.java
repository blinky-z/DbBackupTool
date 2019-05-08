package com.blog.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.text.SimpleDateFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@EnableAsync
@EnableScheduling
public class WebControllersConfiguration {
    private static final String TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    @Bean
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean
    public SimpleDateFormat dateFormat() {
        return new SimpleDateFormat(TIME_FORMAT);
    }
}
