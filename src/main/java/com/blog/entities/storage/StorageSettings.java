package com.blog.entities.storage;

import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

@Entity
@Table(name = "storage_settings")
public class StorageSettings {
    @Id
    private String settingsName;

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

    private StorageSettings(@NotNull StorageType type, @NotNull String settingsName,
                            @NotNull AdditionalStorageSettings additionalStorageSettings) {
        this.type = Objects.requireNonNull(type);
        this.settingsName = settingsName;
        this.additionalStorageSettings = Objects.requireNonNull(additionalStorageSettings);
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

    public String getSettingsName() {
        return settingsName;
    }

    public void setSettingsName(String settingsName) {
        this.settingsName = settingsName;
    }

    void setAdditionalStorageSettings(AdditionalStorageSettings additionalStorageSettings) {
        this.additionalStorageSettings = additionalStorageSettings;
    }

    public StorageType getType() {
        return type;
    }

    void setType(StorageType type) {
        this.type = type;
    }

    public Date getDate() {
        return date;
    }

    void setDate(Date date) {
        this.date = date;
    }

    public Optional<DropboxSettings> getDropboxSettings() {
        return Optional.ofNullable(additionalStorageSettings.getDropboxSettings());
    }

    public Optional<LocalFileSystemSettings> getLocalFileSystemSettings() {
        return Optional.ofNullable(additionalStorageSettings.getLocalFileSystemSettings());
    }

    @Override
    public String toString() {
        return "StorageSettings{" +
                ", settingsName=" + settingsName +
                ", type=" + type +
                ", date=" + date +
                ", additionalStorageSettings=" + additionalStorageSettings +
                '}';
    }

    public static final class Builder {
        private StorageType type;

        private String settingsName;

        private LocalFileSystemSettings localFileSystemSettings;

        private DropboxSettings dropboxSettings;

        private Builder() {
        }

        public StorageSettings build() {
            AdditionalStorageSettings additionalStorageSettings = new AdditionalStorageSettings(
                    type, localFileSystemSettings, dropboxSettings);
            return new StorageSettings(type, settingsName, additionalStorageSettings);
        }

        public Builder withSettingsName(@NotNull String settingsName) {
            this.settingsName = settingsName;
            return this;
        }
    }
}
