package com.blog.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
class InitializeBackupStateWatcher {
    private BackupStateWatcher backupStateWatcher;

    @Autowired
    public void setBackupStateWatcher(BackupStateWatcher backupStateWatcher) {
        this.backupStateWatcher = backupStateWatcher;
    }

    @PostConstruct
    private void initialize() {
        backupStateWatcher.handleCompletedAndInterruptedTasks();
        backupStateWatcher.watchErrorTasks();
    }
}
