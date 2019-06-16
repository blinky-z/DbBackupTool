package com.blog.entities.task;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * This entity represents task canceling request. It maps to the corresponding {@link Task} to cancel.
 */
@Entity
@Table(name = "cancel_tasks")
public class CancelTask {
    /**
     * Identifier of this entity. It is not equal to the corresponding task ID.
     */
    @Id
    @Column(insertable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /**
     * Identifier of corresponding task to cancel.
     */
    @Column(updatable = false)
    private Integer taskId;

    /**
     * When cancel request was received.
     * <p>
     * We need this field to be able handle request timeout.
     */
    @Column(updatable = false)
    private LocalDateTime putTime;

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

    public LocalDateTime getPutTime() {
        return putTime;
    }

    public void setPutTime(LocalDateTime putTime) {
        this.putTime = putTime;
    }

    @Override
    public String toString() {
        return "CancelTask{" +
                "id=" + id +
                ", taskId=" + taskId +
                ", putTime=" + putTime +
                '}';
    }
}
