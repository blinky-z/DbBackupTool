package com.example.demo.models.storage;

import java.util.Objects;

public class AdditionalStorageSettings {
    private Storage type;

    private LocalFileSystemSettings localFileSystemSettings;

    private DropboxSettings dropboxSettings;

    public Storage getType() {
        return type;
    }

    public LocalFileSystemSettings getLocalFileSystemSettings() {
        return localFileSystemSettings;
    }

    public DropboxSettings getDropboxSettings() {
        return dropboxSettings;
    }

    private AdditionalStorageSettings(Storage type, LocalFileSystemSettings localFileSystemSettings, DropboxSettings dropboxSettings) {
        this.type = type;
        this.localFileSystemSettings = localFileSystemSettings;
        this.dropboxSettings = dropboxSettings;
    }

    public static class Builder {
        private Storage type;

        private LocalFileSystemSettings localFileSystemSettings;

        private DropboxSettings dropboxSettings;

        private Builder() {
        }

        public AdditionalStorageSettings build() {
            return new AdditionalStorageSettings(type, localFileSystemSettings, dropboxSettings);
        }
    }

    public static Builder localFileSystemSettings(LocalFileSystemSettings localFileSystemSettings) {
        Builder builder = new Builder();
        builder.type = Storage.LOCAL_FILE_SYSTEM;
        builder.localFileSystemSettings = Objects.requireNonNull(localFileSystemSettings);
        return builder;
    }

    public static Builder dropboxSettings(DropboxSettings dropboxSettings) {
        Builder builder = new Builder();
        builder.type = Storage.DROPBOX;
        builder.dropboxSettings = Objects.requireNonNull(dropboxSettings);
        return builder;
    }
}
