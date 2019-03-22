package com.example.demo.models.storage;

import com.example.demo.models.storage.converter.AdditionalStorageSettingsConverter;

import javax.persistence.*;
import java.util.Date;
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

    @Column(name = "additionalFields", nullable = false)
    @Convert(converter = AdditionalStorageSettingsConverter.class)
    private final AdditionalStorageSettings additionalStorageSettings;

    public StorageSettings(Storage type, AdditionalStorageSettings additionalStorageSettings) {
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

    @Override
    public String toString() {
        return "StorageSettings{" +
                "id=" + id +
                ", type=" + type +
                ", date=" + date +
                '}';
    }
}
