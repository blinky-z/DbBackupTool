package com.blog.entities.task;

import javax.persistence.*;

/**
 * This entity represents erroneous backup task. It maps to the corresponding erroneous {@link Task}.
 */
@Entity
@Table(name = "error_tasks")
public class ErrorTask {
    /**
     * Identifier of this entity. It is not equal to the corresponding task ID.
     */
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Identifier of corresponding erroneous task.
     */
    @Column(updatable = false)
    private Integer taskId;

    /**
     * Has error been handled or not.
     */
    @Column(insertable = false)
    private boolean errorHandled;

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

    public boolean isErrorHandled() {
        return errorHandled;
    }

    public void setErrorHandled(boolean errorHandled) {
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
