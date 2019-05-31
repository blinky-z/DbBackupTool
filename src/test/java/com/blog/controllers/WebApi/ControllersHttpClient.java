package com.blog.controllers.WebApi;

import com.blog.entities.backup.Task;
import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.manager.TasksManager;
import com.blog.service.processor.Processor;
import com.blog.settings.UserSettings;
import com.blog.webUI.formTransfer.*;
import com.blog.webUI.formTransfer.database.WebPostgresSettings;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
@SpringBootTest
class ControllersHttpClient {
    private TestRestTemplate restTemplate;

    @Autowired
    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    private StorageSettings dropboxStorageSettings;

    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;

    @Autowired
    private UserSettings userSettings;

    @Autowired
    private TasksManager tasksManager;

    private String jsessionId = null;

    void login() {
        if (jsessionId == null) {
            String jsessionCookie;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("username", userSettings.getWebUILogin());
            body.add("password", userSettings.getWebUIPassword());

            HttpEntity<MultiValueMap<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.exchange(
                    "/api/login", HttpMethod.POST, entity, String.class);

            jsessionCookie = Objects.requireNonNull(resp.getHeaders().getFirst(HttpHeaders.SET_COOKIE));

            jsessionId = jsessionCookie.substring(0, jsessionCookie.indexOf(";"));
        }
    }

    void setRestTemplate(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    void waitForLastOperationComplete() throws InterruptedException {
        Iterable<Task> backupTaskCollection = tasksManager.findAllByOrderByDateDesc();
        Task task = backupTaskCollection.iterator().next();
        Integer id = task.getId();

        while (tasksManager.findById(id).orElseThrow(RuntimeException::new).getState() != Task.State.COMPLETED) {
            Thread.sleep(300);
        }
    }

    WebDeleteBackupRequest buildDeleteBackupRequest(Integer backupId) {
        WebDeleteBackupRequest request = new WebDeleteBackupRequest();
        request.setBackupId(String.valueOf(backupId));

        return request;
    }

    WebCreateBackupRequest buildCreateBackupRequest(String databaseSettingsName, String storageSettingsName) {
        WebCreateBackupRequest request = new WebCreateBackupRequest();
        request.setDatabaseSettingsName(databaseSettingsName);

        HashMap<String, WebCreateBackupRequest.BackupCreationProperties> backupCreationPropertiesMap = new HashMap<>();
        WebCreateBackupRequest.BackupCreationProperties backupCreationProperties = new WebCreateBackupRequest.BackupCreationProperties();
        backupCreationPropertiesMap.put(storageSettingsName, backupCreationProperties);

        request.setBackupCreationPropertiesMap(backupCreationPropertiesMap);

        return request;
    }

    WebRestoreBackupRequest buildRestoreBackupRequest(Integer backupId, String databaseSettingsName) {
        WebRestoreBackupRequest request = new WebRestoreBackupRequest();
        request.setBackupId(String.valueOf(backupId));
        request.setDatabaseSettingsName(databaseSettingsName);

        return request;
    }

    WebAddStorageRequest buildDefaultAddStorageRequest(StorageType type, String settingsName) {
        WebAddStorageRequest request = new WebAddStorageRequest();
        request.setSettingsName(settingsName);

        switch (type) {
            case LOCAL_FILE_SYSTEM: {
                request.setStorageType("localFileSystem");

                WebLocalFileSystemSettings webLocalFileSystemSettings = new WebLocalFileSystemSettings();
                webLocalFileSystemSettings.setBackupPath(localFileSystemStorageSettings.getLocalFileSystemSettings()
                        .orElseThrow(RuntimeException::new).getBackupPath());

                request.setLocalFileSystemSettings(webLocalFileSystemSettings);
                break;
            }
            case DROPBOX: {
                request.setStorageType("dropbox");

                WebDropboxSettings webDropboxSettings = new WebDropboxSettings();
                webDropboxSettings.setAccessToken(dropboxStorageSettings.getDropboxSettings()
                        .orElseThrow(RuntimeException::new).getAccessToken());

                request.setDropboxSettings(webDropboxSettings);
                break;
            }
            default: {
                throw new RuntimeException("Can't build storage settings DTO: unknown database type");
            }
        }

        return request;
    }

    WebAddPlannedTaskRequest buildAddPlannedTaskRequest(String databaseSettingsName, Collection<String> storageSettingsNameList,
                                                        @Nullable List<Processor> processors, Duration interval) {
        WebAddPlannedTaskRequest webAddPlannedTaskRequest = new WebAddPlannedTaskRequest();
        webAddPlannedTaskRequest.setDatabaseSettingsName(databaseSettingsName);
        webAddPlannedTaskRequest.setStorageSettingsNameList(new ArrayList<>(storageSettingsNameList));
        if (processors != null) {
            webAddPlannedTaskRequest.setProcessors(processors.stream().map(Processor::getName).collect(Collectors.toList()));
        }
        webAddPlannedTaskRequest.setInterval(String.valueOf(interval.getSeconds()));

        return webAddPlannedTaskRequest;
    }

    WebAddDatabaseRequest buildDefaultAddDatabaseRequest(DatabaseType type, String settingsName) {
        WebAddDatabaseRequest request = new WebAddDatabaseRequest();
        request.setSettingsName(settingsName);

        switch (type) {
            case POSTGRES: {
                request.setDatabaseName(masterPostgresDatabaseSettings.getName());
                request.setHost(masterPostgresDatabaseSettings.getHost());
                request.setPort(String.valueOf(masterPostgresDatabaseSettings.getPort()));
                request.setLogin(masterPostgresDatabaseSettings.getLogin());
                request.setPassword(masterPostgresDatabaseSettings.getPassword());

                request.setDatabaseType("postgres");

                WebPostgresSettings webPostgresSettings = new WebPostgresSettings();
                request.setPostgresSettings(webPostgresSettings);
                break;
            }
            default: {
                throw new RuntimeException("Can't build database settings DTO: unknown database type");
            }
        }

        return request;
    }

    private String convertListToString(List<String> list) {
        Iterator<String> iterator = list.iterator();

        StringBuilder listAsString = new StringBuilder();
        while (iterator.hasNext()) {
            listAsString.append(iterator.next());
            if (iterator.hasNext()) {
                listAsString.append(",");
            }
        }

        return listAsString.toString();
    }

    ResponseEntity<String> addDatabase(WebAddDatabaseRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("databaseType", request.getDatabaseType());
        body.add("host", request.getHost());
        body.add("port", request.getPort());
        body.add("databaseName", request.getDatabaseName());
        body.add("settingsName", request.getSettingsName());
        body.add("login", request.getLogin());
        body.add("password", request.getPassword());

        Optional<DatabaseType> databaseTypeOptional = DatabaseType.of(request.getDatabaseType());

        if (!databaseTypeOptional.isPresent()) {
            throw new RuntimeException("Can't perform add database request: invalid storage type");
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/database", HttpMethod.POST, entity, String.class);
    }

    ResponseEntity<String> deleteDatabase(@Nullable String settingsName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("settingsName", settingsName);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/database", HttpMethod.DELETE, entity, String.class);
    }

    ResponseEntity<String> addStorage(WebAddStorageRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("settingsName", request.getSettingsName());
        body.add("storageType", request.getStorageType());
        Optional<StorageType> storageTypeOptional = StorageType.of(request.getStorageType());

        if (!storageTypeOptional.isPresent()) {
            throw new RuntimeException("Can't perform add storage request: invalid storage type");
        }

        switch (storageTypeOptional.get()) {
            case LOCAL_FILE_SYSTEM: {
                body.add("localFileSystemSettings.backupPath", request.getLocalFileSystemSettings().getBackupPath());
                break;
            }
            case DROPBOX: {
                body.add("dropboxSettings.accessToken", request.getDropboxSettings().getAccessToken());
                break;
            }
            default: {
                throw new RuntimeException("Can't perform add storage request: unknown storage type");
            }
        }

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/storage", HttpMethod.POST, entity, String.class);
    }

    ResponseEntity<String> deleteStorage(@Nullable String settingsName) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("settingsName", settingsName);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/storage", HttpMethod.DELETE, entity, String.class);
    }

    ResponseEntity<String> createBackup(WebCreateBackupRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("databaseSettingsName", request.getDatabaseSettingsName());

        for (Map.Entry<String, WebCreateBackupRequest.BackupCreationProperties> entry :
                request.getBackupCreationPropertiesMap().entrySet()) {
            String storageSettingsName = entry.getKey();
            WebCreateBackupRequest.BackupCreationProperties backupCreationProperties = entry.getValue();

            body.add("backupCreationPropertiesMap[" + storageSettingsName + "].selected", "true");

            List<String> processors = backupCreationProperties.getProcessors();
            if (!processors.isEmpty()) {
                body.add("backupCreationPropertiesMap[" + storageSettingsName + "].processors", convertListToString(processors));
            }
        }
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.postForEntity("/create-backup", entity, String.class);
    }

    ResponseEntity<String> deleteBackup(WebDeleteBackupRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("backupId", request.getBackupId());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/delete-backup", HttpMethod.DELETE, entity, String.class);
    }

    ResponseEntity<String> restoreBackup(WebRestoreBackupRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("databaseSettingsName", request.getDatabaseSettingsName());
        body.add("backupId", request.getBackupId());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/restore-backup", HttpMethod.POST, entity, String.class);
    }

    ResponseEntity<String> addPlannedTask(WebAddPlannedTaskRequest request) {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        httpHeaders.add("Cookie", jsessionId);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("databaseSettingsName", request.getDatabaseSettingsName());
        body.add("storageSettingsNameList", convertListToString(request.getStorageSettingsNameList()));
        body.add("processors", convertListToString(request.getProcessors()));
        body.add("interval", request.getInterval());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, httpHeaders);

        return restTemplate.exchange("/add-planned-task", HttpMethod.POST, entity, String.class);
    }
}
