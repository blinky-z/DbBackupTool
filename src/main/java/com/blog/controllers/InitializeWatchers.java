package com.blog.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
class InitializeWatchers {
    private BackupTasksWatcher backupTasksWatcher;

    private PlannedBackupTasksWatcher plannedBackupTasksWatcher;

    @Autowired
    public void setBackupTasksWatcher(BackupTasksWatcher backupTasksWatcher) {
        this.backupTasksWatcher = backupTasksWatcher;
    }

    @Autowired
    public void setPlannedBackupTasksWatcher(PlannedBackupTasksWatcher plannedBackupTasksWatcher) {
        this.plannedBackupTasksWatcher = plannedBackupTasksWatcher;
    }

    @PostConstruct
    private void initialize() {
        backupTasksWatcher.handleCompletedAndInterruptedTasks();
        backupTasksWatcher.watchErrorTasks();
        plannedBackupTasksWatcher.watchPlannedTasks();
    }
}
