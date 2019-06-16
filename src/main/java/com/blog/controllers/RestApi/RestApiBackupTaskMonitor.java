package com.blog.controllers.RestApi;

import com.blog.entities.task.Task;
import com.blog.manager.TasksManager;
import com.blog.webUI.renderModels.WebBackupTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;


/**
 * This rest controller allows to get backup task states as JSON data.
 * <p>
 * That is, it becomes possible to refresh task states not reloading page using ajax.
 * This feature is optional and even if javascript is disabled in browser no main functionality will be affected.
 */
@RestController
public class RestApiBackupTaskMonitor {
    private DateTimeFormatter webDateFormatter;

    private TasksManager tasksManager;

    @Autowired
    public void setWebDateFormatter(DateTimeFormatter webDateFormatter) {
        this.webDateFormatter = webDateFormatter;
    }

    @Autowired
    public void setTasksManager(TasksManager tasksManager) {
        this.tasksManager = tasksManager;
    }

    @GetMapping(path = "/api/get-tasks")
    public List<WebBackupTask> getStates() {
        List<WebBackupTask> webBackupTasks = new ArrayList<>();

        for (Task task : tasksManager.findAllByRunType(Task.RunType.USER)) {
            WebBackupTask webBackupTask = new WebBackupTask.Builder()
                    .withId(task.getId())
                    .withType(task.getType().toString())
                    .withState(task.getState().toString())
                    .withInterrupted(task.getInterrupted())
                    .withTime(webDateFormatter.format(task.getDate()))
                    .build();

            webBackupTasks.add(webBackupTask);
        }

        return webBackupTasks;
    }
}
