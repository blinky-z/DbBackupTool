package com.blog.controllers.RestApi;

import com.blog.ApplicationTests;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.task.Task;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.TasksManager;
import com.blog.repositories.BackupPropertiesRepository;
import com.blog.repositories.TasksRepository;
import com.blog.webUI.renderModels.WebBackupTask;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RestApiBackupTaskMonitorTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate testRestTemplate;
    @Autowired
    private RestApiClient restApiClient;
    @Autowired
    private TasksManager tasksManager;
    @Autowired
    private TasksRepository tasksRepository;
    @Autowired
    private BackupPropertiesManager backupPropertiesManager;
    @Autowired
    private BackupPropertiesRepository backupPropertiesRepository;
    @Autowired
    private DateTimeFormatter webDateFormatter;

    private AtomicBoolean initialized = new AtomicBoolean(false);

    @BeforeEach
    void init() {
        if (initialized.compareAndSet(false, true)) {
            restApiClient.setTestRestTemplate(testRestTemplate);
        }
    }

    @AfterEach
    void afterEach() {
        tasksRepository.deleteAll();
        backupPropertiesRepository.deleteAll();
    }

    @Test
    void getStates_shouldReturnOnlyUserInitiatedTasks(TestInfo testInfo) {
        // create user initiated tasks
        int userTasksAmount = 2;
        for (int i = 0; i < userTasksAmount; i++) {
            BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                    testInfo.getDisplayName(), null, testInfo.getDisplayName());
            tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.USER, backupProperties.getId());
        }

        // create at least one internal task to make sure further that controller method will return only user initiated tasks
        BackupProperties internalBackupProperties = backupPropertiesManager.initNewBackupProperties(
                testInfo.getDisplayName(), null, testInfo.getDisplayName());
        tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.INTERNAL, internalBackupProperties.getId());

        List<WebBackupTask> taskStates = restApiClient.getTaskStates();
        assertEquals(userTasksAmount, taskStates.size());
    }

    @Test
    void getStates_shouldProperlyFormatFieldsInDto(TestInfo testInfo) {
        BackupProperties backupProperties = backupPropertiesManager.initNewBackupProperties(
                testInfo.getDisplayName(), null, testInfo.getDisplayName());
        Integer taskId = tasksManager.initNewTask(Task.Type.CREATE_BACKUP, Task.RunType.USER, backupProperties.getId());
        Task task = tasksManager.findById(taskId).get();

        List<WebBackupTask> taskStates = restApiClient.getTaskStates();
        assertEquals(1, taskStates.size());
        WebBackupTask webBackupTask = taskStates.iterator().next();

        assertEquals(task.getId(), webBackupTask.getId());
        assertEquals(task.getState().toString(), webBackupTask.getState());
        assertEquals(webDateFormatter.format(task.getDate()), webBackupTask.getTime());
        assertEquals(task.getType().toString(), webBackupTask.getType());
    }
}