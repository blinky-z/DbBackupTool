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
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private StorageType type;

    @Column(insertable = false, updatable = false)
    private Date date;

    @Column(name = "additionalFields")
    @Convert(converter = AdditionalStorageSettingsConverter.class)
    private AdditionalStorageSettings additionalStorageSettings;

    StorageSettings() {
    }

    void setId(Integer id) {
        this.id = id;
    }

    void setDate(Date date) {
        this.date = date;
    }

    void setType(StorageType type) {
        this.type = type;
    }

    void setAdditionalStorageSettings(AdditionalStorageSettings additionalStorageSettings) {
        this.additionalStorageSettings = additionalStorageSettings;
    }

    private StorageSettings(@NotNull StorageType type, @NotNull AdditionalStorageSettings additionalStorageSettings) {
        this.type = Objects.requireNonNull(type);
        this.additionalStorageSettings = Objects.requireNonNull(additionalStorageSettings);
    }

    public Integer getId() {
        return id;
    }

    public StorageType getType() {
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
        private StorageType type;

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
        builder.type = StorageType.LOCAL_FILE_SYSTEM;
        builder.localFileSystemSettings = Objects.requireNonNull(localFileSystemSettings);
        return builder;
    }

    public static Builder dropboxSettings(@NotNull DropboxSettings dropboxSettings) {
        Builder builder = new Builder();
        builder.type = StorageType.DROPBOX;
        builder.dropboxSettings = Objects.requireNonNull(dropboxSettings);
        return builder;
    }

    @Override
    public String toString() {
        return "StorageSettings{" +
                "id=" + id +
                ", type=" + type +
                ", date=" + date +
                ", additionalStorageSettings=" + additionalStorageSettings +
                '}';
    }
}
