package com.blog.controllers.WebApi;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.database.DatabaseType;
import com.blog.entities.storage.StorageSettings;
import com.blog.entities.storage.StorageType;
import com.blog.entities.task.Task;
import com.blog.manager.ErrorTasksManager;
import com.blog.manager.TasksManager;
import com.blog.service.processor.Processor;
import com.blog.settings.UserSettings;
import com.blog.webUI.formTransfer.*;
import com.blog.webUI.formTransfer.database.WebPostgresSettings;
import com.blog.webUI.formTransfer.storage.WebDropboxSettings;
import com.blog.webUI.formTransfer.storage.WebLocalFileSystemSettings;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Component
class WebApiClient {
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

    @Autowired
    private ErrorTasksManager errorTasksManager;

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

        System.out.println("waiting...");
        while (tasksManager.findById(id).get().getState() != Task.State.COMPLETED) {
            if (errorTasksManager.isError(id)) {
                throw new RuntimeException("Error occurred while executing task");
            }
            Thread.yield();
        }
        System.out.println("completed");
    }

    WebDeleteBackupRequest buildDeleteBackupRequest(Integer backupId) {
        WebDeleteBackupRequest request = new WebDeleteBackupRequest();
        request.setBackupId(String.valueOf(backupId));

        return request;
    }

    WebCreateBackupRequest buildCreateBackupRequest(String databaseSettingsName, List<String> storageSettingsNameList,
                                                    @Nullable List<String> processors) {
        WebCreateBackupRequest request = new WebCreateBackupRequest();
        request.setDatabaseSettingsName(databaseSettingsName);
        request.setStorageSettingsNameList(storageSettingsNameList);
        if (processors != null) {
            request.setProcessors(processors);
        } else {
            request.setProcessors(Collections.emptyList());
        }

        return request;
    }

    WebCreateBackupRequest buildCreateBackupRequest(String databaseSettingsName, String storageSettingsName,
                                                    @Nullable List<String> processors) {
        return buildCreateBackupRequest(databaseSettingsName, Collections.singletonList(storageSettingsName), processors);
    }

    WebCreateBackupRequest buildCreateBackupRequest(String databaseSettingsName, String storageSettingsName) {
        return buildCreateBackupRequest(databaseSettingsName, Collections.singletonList(storageSettingsName), null);
    }

    WebRestoreBackupRequest buildRestoreBackupRequest(Integer backupId, String storageSettingsName, String databaseSettingsName) {
        WebRestoreBackupRequest request = new WebRestoreBackupRequest();
        request.setBackupId(String.valueOf(backupId));
        request.setStorageSettingsName(storageSettingsName);
        request.setDatabaseSettingsName(databaseSettingsName);

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
        body.add("processors", convertListToString(request.getProcessors()));
        body.add("storageSettingsNameList", convertListToString(request.getStorageSettingsNameList()));

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
        body.add("storageSettingsName", request.getStorageSettingsName());
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
