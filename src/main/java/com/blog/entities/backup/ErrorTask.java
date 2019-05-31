package com.blog.entities.backup;

import javax.persistence.*;

/**
 * This entity represents erroneous backup task.
 */
@Entity
@Table(name = "error_tasks")
public class ErrorTask {
    /**
     * Identifier of each error backup task. This identifier is equal to backup task identifier.
     */
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(updatable = false)
    private Integer taskId;

    @Column(insertable = false)
    private Boolean errorHandled;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getTaskId() {
        return taskId;
    }

    public void setTaskId(Integer taskId) {
        this.taskId = taskId;
    }

    public Boolean isErrorHandled() {
        return errorHandled;
    }

    public void setErrorHandled(Boolean errorHandled) {
        this.errorHandled = errorHandled;
    }

    @Override
    public String toString() {
        return "ErrorTask{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", errorHandled=" + errorHandled +
                '}';
    }
}
