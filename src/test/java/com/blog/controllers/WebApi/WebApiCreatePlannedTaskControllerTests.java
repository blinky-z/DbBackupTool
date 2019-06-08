package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.TestUtils;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.entities.task.PlannedTask;
import com.blog.manager.*;
import com.blog.repositories.PlannedTasksRepository;
import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class WebApiCreatePlannedTaskControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private TestUtils testUtils;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private HashMap<StorageType, String> storageSettingsNameMap;

    @Autowired
    private HashMap<DatabaseType, String> databaseSettingsNameMap;

    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    @Autowired
    private DatabaseBackupManager databaseBackupManager;

    @Autowired
    private BackupLoadManager backupLoadManager;

    @Autowired
    private WebApiClient webApiClient;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;

    @Autowired
    private PlannedTasksManager plannedTasksManager;

    @Autowired
    private PlannedTasksRepository plannedTasksRepository;

    @BeforeAll
    void setup() {
        databaseSettingsManager.saveAll(allDatabaseSettings);
        storageSettingsManager.saveAll(allStorageSettings);
        webApiClient.setRestTemplate(restTemplate);
        webApiClient.login();
    }

    @BeforeEach
    void init() {
        testUtils.clearDatabase(jdbcPostgresMasterTemplate);
        testUtils.initDatabase(jdbcPostgresMasterTemplate);
    }

    //    @Test
    void givenProperRequestWithAllStoragesAndPostgresDatabase_addPlannedTask_shouldAddPlannedTaskAndExecuteInTime()
            throws InterruptedException {
        Duration interval = Duration.ofSeconds(30);
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = webApiClient.buildAddPlannedTaskRequest(
                databaseSettingsNameMap.get(DatabaseType.POSTGRES),
                storageSettingsNameMap.values(),
                null,
                interval);

        ResponseEntity<String> response = webApiClient.addPlannedTask(webAddPlannedTaskRequest);
        assertEquals(response.getStatusCode(), HttpStatus.FOUND);

        PlannedTask plannedTask = plannedTasksRepository.findAllByOrderByIdDesc().iterator().next();
        LocalDateTime lastTimeStarted = plannedTask.getLastStartedTime();

        while (plannedTasksRepository.findAllByOrderByIdDesc().iterator().next()
                .getLastStartedTime().equals(lastTimeStarted)) {
            Thread.sleep(30);
        }

        List<BackupProperties> backupPropertiesList = backupPropertiesManager.findAllByOrderByIdDesc();
        assertTrue(backupPropertiesList.size() >= storageSettingsNameMap.values().size());
    }
}
