package com.blog.controllers;

import com.blog.manager.BackupTaskManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private static final Logger logger = LoggerFactory.getLogger("ErrorCallBack");

    private BackupTaskManager backupTaskManager;

    private static final String TIME_FORMAT = "dd.MM.yyyy HH:mm:ss";

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
    public ExecutorService executorService() {
        return Executors.newFixedThreadPool(10);
    }

    @Bean
    public SimpleDateFormat dateFormat() {
        return new SimpleDateFormat(TIME_FORMAT);
    }
}
