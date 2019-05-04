package com.blog.WebApiTests.ControllerTests;

import com.blog.entities.database.DatabaseSettings;
import com.blog.entities.storage.StorageSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Configuration
public class ControllerTestsConfiguration {
    @Autowired
    private DatabaseSettings masterPostgresDatabaseSettings;

    @Autowired
    private StorageSettings localFileSystemStorageSettings;

    @Autowired
    private StorageSettings dropboxStorageSettings;

    @Bean
    public MultiValueMap<String, Object> postgresDatabaseSettingsAsMultiValueMap() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("databaseType", "postgres");
        body.add("host", masterPostgresDatabaseSettings.getHost());
        body.add("port", String.valueOf(masterPostgresDatabaseSettings.getPort()));
        body.add("databaseName", masterPostgresDatabaseSettings.getName());
        body.add("login", masterPostgresDatabaseSettings.getLogin());
        body.add("password", masterPostgresDatabaseSettings.getPassword());

        return body;
    }

    @Bean
    public MultiValueMap<String, Object> localFileSystemStorageSettingsAsMultiValueMap() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("storageType", "localFileSystem");
        body.add("localFileSystemSettings.backupPath", localFileSystemStorageSettings.getLocalFileSystemSettings().
                orElseThrow(RuntimeException::new).getBackupPath());

        return body;
    }

    @Bean
    public MultiValueMap<String, Object> dropboxStorageSettingsAsMultiValueMap() {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("storageType", "dropbox");
        body.add("dropboxSettings.accessToken", dropboxStorageSettings.getDropboxSettings().
                orElseThrow(RuntimeException::new).getAccessToken());

        return body;
    }
}
