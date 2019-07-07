package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
@PropertySource("classpath:application-postgres.properties")
@ConfigurationProperties(prefix = "postgres")
public class PostgresDatabaseBackupConfiguration {
    /**
     * Pg_dump binary file path
     */
    private String pgDumpToolPath;

    /**
     * Psql binary file path
     */
    private String psqlToolPath;

    public String getPgDumpToolPath() {
        return pgDumpToolPath;
    }

    public void setPgDumpToolPath(String pgDumpToolPath) {
        this.pgDumpToolPath = pgDumpToolPath;
    }

    public String getPsqlToolPath() {
        return psqlToolPath;
    }

    public void setPsqlToolPath(String psqlToolPath) {
        this.psqlToolPath = psqlToolPath;
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService postgresExecutorService() {
        return Executors.newFixedThreadPool(20);
    }

    @Bean
    public String pgDumpToolPath() {
        if (!pgDumpToolPath.isEmpty()) {
            return pgDumpToolPath;
        }

        return "pg_dump";
    }

    @Bean
    public String psqlToolPath() {
        if (!psqlToolPath.isEmpty()) {
            return psqlToolPath;
        }

        return "psql";
    }
}
