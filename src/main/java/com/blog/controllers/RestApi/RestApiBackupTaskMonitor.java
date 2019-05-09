package com.blog.controllers.RestApi;

import com.blog.entities.backup.BackupTask;
import com.blog.manager.BackupTaskManager;
import com.blog.webUI.renderModels.WebBackupTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@RestController
public class RestApiBackupTaskMonitor {
    private SimpleDateFormat dateFormat;

    private BackupTaskManager backupTaskManager;

    @Autowired
    public void setDateFormat(SimpleDateFormat dateFormat) {
        this.dateFormat = dateFormat;
    }

    @Autowired
    public void setBackupTaskManager(BackupTaskManager backupTaskManager) {
        this.backupTaskManager = backupTaskManager;
    }

    @GetMapping(path = "/api/get-states")
    public List<WebBackupTask> getStates() {
        List<WebBackupTask> webBackupTasks = new ArrayList<>();

        for (BackupTask backupTask : backupTaskManager.getBackupTasks()) {
            WebBackupTask webBackupTask = new WebBackupTask(backupTask.getId(), backupTask.getType().toString(),
                    backupTask.getState().toString(), dateFormat.format(backupTask.getDate()), backupTask.isError());
            webBackupTasks.add(webBackupTask);
        }

        return webBackupTasks;
    }
}
