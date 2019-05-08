package com.blog.service.databaseBackup.PostgresDatabaseBackup;

import com.blog.manager.BackupTaskManager;
import com.blog.service.databaseBackup.DatabaseBackup;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:postgres.properties")
@ConfigurationProperties(prefix = "postgres")
class PostgresDatabaseBackupConfiguration {
    private final BackupTaskManager backupTaskManager;
    private String pgDumpToolPath;
    private String psqlToolPath;

    public PostgresDatabaseBackupConfiguration(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    public void setPgDumpToolPath(String pgDumpToolPath) {
        this.pgDumpToolPath = pgDumpToolPath;
    }

    public void setPsqlToolPath(String psqlToolPath) {
        this.psqlToolPath = psqlToolPath;
    }

    @Bean
    public DatabaseBackup.ErrorCallback errorCallback() {
        return new DatabaseBackup.ErrorCallback() {
            @Override
            public void onError(@NotNull Throwable t, @NotNull Integer id) {
                backupTaskManager.setError(id);
            }
        };
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
