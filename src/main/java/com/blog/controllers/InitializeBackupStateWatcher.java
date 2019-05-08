package com.blog.controllers;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class InitializeBackupStateWatcher {
    private final BackupStateWatcher backupStateWatcher;

    public InitializeBackupStateWatcher(BackupStateWatcher backupStateWatcher) {
        this.backupStateWatcher = backupStateWatcher;
    }

    @PostConstruct
    public void initialize() {
        backupStateWatcher.watchStates();
    }
}
