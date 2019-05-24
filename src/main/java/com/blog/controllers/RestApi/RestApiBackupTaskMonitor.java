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


/**
 * This rest controller allows to get backup task states as JSON data.
 * <p>
 * That is, it becomes possible to refresh task states not reloading page using ajax.
 * This feature is very optional and even if javascript is disabled in browser no main functionality will be affected.
 */
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

        for (BackupTask backupTask : backupTaskManager.findAllByRunType(BackupTask.RunType.USER)) {
            WebBackupTask webBackupTask = new WebBackupTask.Builder()
                    .withId(backupTask.getId())
                    .withType(backupTask.getType().toString())
                    .withState(backupTask.getState().toString())
                    .withTime(dateFormat.format(backupTask.getDate()))
                    .withIsError(backupTask.isError())
                    .build();

            webBackupTasks.add(webBackupTask);
        }

        return webBackupTasks;
    }
}
