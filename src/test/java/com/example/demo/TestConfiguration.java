package com.example.demo;

import com.example.demo.entities.database.Database;
import com.example.demo.entities.database.DatabaseSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class TestConfiguration {
    @Autowired
    private DataSource masterDataSource;

    @Autowired
    private DataSource copyDataSource;

    @Bean
    public JdbcTemplate jdbcMasterTemplate() {
        return new JdbcTemplate(masterDataSource);
    }

    @Bean
    public JdbcTemplate jdbcCopyTemplate() {
        return new JdbcTemplate(copyDataSource);
    }

    @Bean
    public DatabaseSettings masterDatabaseSettings() {
        return TestUtils.buildDatabaseSettings(Database.POSTGRES, masterDataSource);
    }

    @Bean
    public DatabaseSettings copyDatabaseSettings() {
        return TestUtils.buildDatabaseSettings(Database.POSTGRES, copyDataSource);
    }
}
