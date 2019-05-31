package com.blog.controllers;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

@Configuration
public class WebControllersConfiguration {
    /**
     * This time format is used in all elements of front-end (backup creation time, task start time and etc).
     */
    private static final String WEB_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    @Bean
    public DateTimeFormatter dateFormat() {
        return DateTimeFormatter.ofPattern(WEB_TIME_FORMAT);
    }
}
