package com.example.demo;

import com.example.demo.settings.DatabaseSettings;
import com.example.demo.settings.DatabaseTestSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class TestConfiguration {
    @Bean(name = "masterDatabaseSettings")
    @Primary
    @Autowired
    DatabaseSettings masterDatabaseSettings(DataSource masterDataSource) {
        return new DatabaseTestSettings(masterDataSource);
    }

    @Bean(name = "copyDatabaseSettings")
    @Autowired
    DatabaseSettings copyDatabaseSettings(DataSource copyDataSource) {
        return new DatabaseTestSettings(copyDataSource);
    }

    @Bean(name = "jdbcMaster")
    @Primary
    @Autowired
    public JdbcTemplate masterJdbcTemplate(DataSource masterDataSource) {
        return new JdbcTemplate(masterDataSource);
    }

    @Bean(name = "jdbcCopy")
    @Autowired
    public JdbcTemplate copyJdbcTemplate(DataSource copyDataSource) {
        return new JdbcTemplate(copyDataSource);
    }
}
