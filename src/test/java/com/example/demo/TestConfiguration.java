package com.example.demo;

import com.example.demo.entities.database.DatabaseType;
import com.example.demo.entities.database.DatabaseSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class TestConfiguration {
    @Autowired
    @Qualifier("masterDataSource")
    private DataSource masterDataSource;

    @Autowired
    @Qualifier("copyDataSource")
    private DataSource copyDataSource;

    @Autowired
    private TestUtils testUtils;

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
        return testUtils.buildDatabaseSettings(DatabaseType.POSTGRES, masterDataSource);
    }

    @Bean
    public DatabaseSettings copyDatabaseSettings() {
        return testUtils.buildDatabaseSettings(DatabaseType.POSTGRES, copyDataSource);
    }
}
