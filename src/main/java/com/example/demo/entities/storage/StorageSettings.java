package com.example.demo.entities.storage;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "storage_settings")
public class StorageSettings {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private final Storage type;

    @Column(insertable = false, updatable = false)
    private Date date;

    @Column(name = "additionalFields")
    @Convert(converter = AdditionalStorageSettingsConverter.class)
    private final AdditionalStorageSettings additionalStorageSettings;

    private StorageSettings(Storage type, AdditionalStorageSettings additionalStorageSettings) {
        this.type = type;
        this.additionalStorageSettings = additionalStorageSettings;
    }

    public int getId() {
        return id;
    }

    public Storage getType() {
        return type;
    }

    public Date getDate() {
        return date;
    }

    public Optional<DropboxSettings> getDropboxSettings() {
        return Optional.ofNullable(additionalStorageSettings.getDropboxSettings());
    }

    public Optional<LocalFileSystemSettings> getLocalFileSystemSettings() {
        return Optional.ofNullable(additionalStorageSettings.getLocalFileSystemSettings());
    }

    public static final class Builder {
        private Storage type;

        private LocalFileSystemSettings localFileSystemSettings;

        private DropboxSettings dropboxSettings;

        private Builder() {
        }

        public StorageSettings build() {
            AdditionalStorageSettings additionalStorageSettings = new AdditionalStorageSettings(
                    type, localFileSystemSettings, dropboxSettings);
            return new StorageSettings(type, additionalStorageSettings);
        }
    }

    public static Builder localFileSystemSettings(@NotNull LocalFileSystemSettings localFileSystemSettings) {
        Builder builder = new Builder();
        builder.type = Storage.LOCAL_FILE_SYSTEM;
        builder.localFileSystemSettings = Objects.requireNonNull(localFileSystemSettings);
        return builder;
    }

    public static Builder dropboxSettings(@NotNull DropboxSettings dropboxSettings) {
        Builder builder = new Builder();
        builder.type = Storage.DROPBOX;
        builder.dropboxSettings = Objects.requireNonNull(dropboxSettings);
        return builder;
    }

    @Override
    public String toString() {
        return "StorageSettings{" +
                "id=" + id +
                ", type=" + type +
                ", date=" + date +
                '}';
    }
}
