package com.blog.entities.backup;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table(name = "backup_tasks")
public class BackupTask {
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    private BackupTaskState state;

    private Boolean isError;

    @Column(insertable = false, updatable = false)
    private Date date;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public BackupTaskState getState() {
        return state;
    }

    public void setState(BackupTaskState state) {
        this.state = state;
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
}
