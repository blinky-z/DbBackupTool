package com.blog.entities.backup;

import javax.persistence.*;
import java.util.Date;

/**
 * This entity represents backup task.
 */
@Entity
@Table(name = "backup_tasks")
public class BackupTask {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(updatable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    private State state;

    private Integer backupPropertiesId;

    private Boolean isError;

    @Column(insertable = false, updatable = false)
    private Date date;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public Integer getBackupPropertiesId() {
        return backupPropertiesId;
    }

    public void setBackupPropertiesId(Integer backupPropertiesId) {
        this.backupPropertiesId = backupPropertiesId;
    }

    public Boolean isError() {
        return isError;
    }

    public void setError(Boolean error) {
        isError = error;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public String toString() {
        return "BackupTask{" +
                "id=" + id +
                ", type=" + type +
                ", state=" + state +
                ", isError=" + isError +
                ", date=" + date +
                '}';
    }

    public enum State {
        PLANNED,
        CREATING,
        RESTORING,
        DELETING,
        APPLYING_PROCESSORS,
        APPLYING_DEPROCESSORS,
        DOWNLOADING,
        UPLOADING,
        COMPLETED
    }

    public enum Type {
        CREATE_BACKUP {
            @Override
            public String toString() {
                return "CREATE BACKUP";
            }
        },
        RESTORE_BACKUP {
            @Override
            public String toString() {
                return "RESTORE BACKUP";
            }
        },
        DELETE_BACKUP {
            @Override
            public String toString() {
                return "DELETE BACKUP";
            }
        }
    }
}
