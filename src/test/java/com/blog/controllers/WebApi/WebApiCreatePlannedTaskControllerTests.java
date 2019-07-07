package com.blog.controllers.WebApi;

import com.blog.ApplicationTests;
import com.blog.entities.backup.BackupProperties;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.entities.task.PlannedTask;
import com.blog.manager.BackupPropertiesManager;
import com.blog.manager.DatabaseSettingsManager;
import com.blog.manager.StorageSettingsManager;
import com.blog.repositories.BackupPropertiesRepository;
import com.blog.repositories.PlannedTasksRepository;
import com.blog.webUI.formTransfer.WebAddPlannedTaskRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.StreamSupport;

import static com.blog.TestUtils.clearDatabase;
import static com.blog.TestUtils.initDatabase;
import static org.junit.jupiter.api.Assertions.assertEquals;

class WebApiCreatePlannedTaskControllerTests extends ApplicationTests {
    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private JdbcTemplate jdbcPostgresMasterTemplate;

    @Autowired
    private DatabaseSettingsManager databaseSettingsManager;

    @Autowired
    private StorageSettingsManager storageSettingsManager;

    @Autowired
    private Map<StorageType, String> storageSettingsNameMap;

    @Autowired
    private BackupPropertiesManager backupPropertiesManager;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Autowired
    private WebApiClient webApiClient;

    @Autowired
    private List<DatabaseSettings> allDatabaseSettings;

    @Autowired
    private List<StorageSettings> allStorageSettings;

    @Autowired
    private PlannedTasksRepository plannedTasksRepository;
    @Autowired
    private Map<DatabaseType, String> databaseSettingsNameMap;
    @Autowired
    private BackupPropertiesRepository backupPropertiesRepository;

    @BeforeEach
    void init() {
        if (initialized.compareAndSet(false, true)) {
            databaseSettingsManager.saveAll(allDatabaseSettings);
            storageSettingsManager.saveAll(allStorageSettings);
            webApiClient.setTestRestTemplate(restTemplate);
        }

        clearDatabase(jdbcPostgresMasterTemplate);
        initDatabase(jdbcPostgresMasterTemplate);
    }

    @AfterEach
    void beforeEach() {
        backupPropertiesRepository.deleteAll();
        plannedTasksRepository.deleteAll();
    }

    @Test
    void givenProperRequestWithAllStoragesAndPostgresDatabase_addPlannedTask_shouldAddPlannedTaskAndExecuteInTime()
            throws InterruptedException {
        Duration interval = Duration.ofSeconds(30);
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = webApiClient.buildAddPlannedTaskRequest(
                databaseSettingsNameMap.get(DatabaseType.POSTGRES),
                storageSettingsNameMap.values(),
                Collections.emptyList(),
                interval);

        ResponseEntity<String> response = webApiClient.addPlannedTask(webAddPlannedTaskRequest);
        assertEquals(response.getStatusCode(), HttpStatus.FOUND);

        Iterable<PlannedTask> plannedTasks = plannedTasksRepository.findAllByOrderByIdDesc();
        assertEquals(1, StreamSupport.stream(plannedTasks.spliterator(), false).count());
        PlannedTask plannedTask = plannedTasks.iterator().next();
        LocalDateTime initStartTime = plannedTask.getLastStartedTime();

        while (plannedTasksRepository.findAllByOrderByIdDesc().iterator().next()
                .getLastStartedTime().equals(initStartTime)) {
            Thread.sleep(30);
        }

        List<BackupProperties> backupPropertiesList = backupPropertiesManager.findAllByOrderByIdDesc();
        assertEquals(1, backupPropertiesList.size());
    }
}
