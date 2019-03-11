package com.example.demo;

import com.example.demo.settings.DatabaseSettings;
import com.example.demo.settings.DatabaseTestSettings;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class TestConfiguration {
    @Bean
    DatabaseSettings databaseSettings(DataSource dataSource) {
        return new DatabaseTestSettings(dataSource);
    }
}
