package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:postgres.properties")
@ConfigurationProperties(prefix = "postgres")
class PostgresDatabaseBackupConfiguration {
    private String pgDumpToolPath;
    private String psqlToolPath;

    public void setPgDumpToolPath(String pgDumpToolPath) {
        this.pgDumpToolPath = pgDumpToolPath;
    }

    public void setPsqlToolPath(String psqlToolPath) {
        this.psqlToolPath = psqlToolPath;
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
