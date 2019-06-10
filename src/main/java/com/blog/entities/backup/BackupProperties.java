package com.blog.entities.backup;

import com.blog.entities.ProcessorTypeEnumListToStringFieldConverter;
import com.blog.entities.StringListToStringFieldConverter;
import com.blog.entities.storage.StorageSettings;
import com.blog.service.processor.ProcessorType;
import org.jetbrains.annotations.NotNull;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * This entity represents created backup properties.
 * <p>
 * This entity contains required fields to get access to saved backup on storage: {@link #backupName} and {@link #storageSettingsNameList}.
 * <p>
 * The logic is following:
 * {@link StorageSettings} can be retrieved using <i>storageSettingsName</i> and backup can be uploaded/downloaded/deleted
 * passing <i>backupName</i> and <i>storage settings</i> to storage service.
 *
 * @see com.blog.manager.BackupPropertiesManager#initNewBackupProperties(List, List, String)
 */
@Entity
@Table(name = "backup_properties")
public class BackupProperties {
    /**
     * Identifier of each backup properties.
     */
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Backup creation time.
     */
    @Column(updatable = false)
    private LocalDateTime date;

    /**
     * Backup name. Backup name is an identifier how backup can be accessed using storage service.
     */
    @Column(updatable = false)
    private String backupName;

    /**
     * Applied processors on backup.
     * <p>
     * This field is automatically converted into single string and back to List by {@link StringListToStringFieldConverter} class.
     */
    @Column(updatable = false)
    @Convert(converter = ProcessorTypeEnumListToStringFieldConverter.class)
    private List<ProcessorType> processors;

    /**
     * List of storage identifiers where backup is saved.
     */
    @Convert(converter = StringListToStringFieldConverter.class)
    private List<String> storageSettingsNameList;

    BackupProperties() {

    }

    public BackupProperties(@NotNull String backupName, @NotNull List<ProcessorType> processors, @NotNull LocalDateTime date,
                            @NotNull List<String> storageSettingsNameList) {
        this.backupName = Objects.requireNonNull(backupName);
        this.processors = Objects.requireNonNull(processors);
        this.date = Objects.requireNonNull(date);
        this.storageSettingsNameList = Objects.requireNonNull(storageSettingsNameList);
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public LocalDateTime getDate() {
        return date;
    }

    public void setDate(LocalDateTime date) {
        this.date = date;
    }

    public String getBackupName() {
        return backupName;
    }

    public void setBackupName(String backupName) {
        this.backupName = backupName;
    }

    public List<ProcessorType> getProcessors() {
        return processors;
    }

    public void setProcessors(List<ProcessorType> processors) {
        this.processors = processors;
    }

    public List<String> getStorageSettingsNameList() {
        return storageSettingsNameList;
    }

    public void setStorageSettingsNameList(List<String> storageSettingsNameList) {
        this.storageSettingsNameList = storageSettingsNameList;
    }

    @Override
    public String toString() {
        return "BackupProperties{" +
                "id=" + id +
                ", date=" + date +
                ", backupName='" + backupName + '\'' +
                ", processors=" + processors +
                ", storageSettingsNameList=" + storageSettingsNameList +
                '}';
    }
}
