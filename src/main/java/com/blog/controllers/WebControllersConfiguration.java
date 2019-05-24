package com.blog.controllers;

import com.blog.manager.BackupTaskManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration
public class WebControllersConfiguration {
    private static final Logger logger = LoggerFactory.getLogger("ErrorCallBack");

    private BackupTaskManager backupTaskManager;

    /**
     * This time format is used in all elements of front-end (backup creation time, task start time and so on).
     */
    private static final String WEB_TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @Bean
    public ErrorCallback errorCallback() {
        return new ErrorCallback() {
            @Override
            public void onError(@NotNull Throwable t, @NotNull Integer id) {
                logger.error("Error catched: ", t);
                backupTaskManager.setError(id);
            }
        };
    }

    @Bean
    public SimpleDateFormat dateFormat() {
        return new SimpleDateFormat(WEB_TIME_FORMAT);
    }
}
