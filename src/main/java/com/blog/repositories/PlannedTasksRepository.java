package com.blog.repositories;

import com.blog.entities.task.PlannedTask;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.ArrayList;

public interface PlannedTasksRepository extends CrudRepository<PlannedTask, Integer> {
    ArrayList<PlannedTask> findAllByOrderByIdDesc();

    /**
     * Returns first N rows that are not locked. Also sets pessimistic lock on retrieved rows in case of method is called in transaction block.
     * <p>
     * This method uses {@code select for update} statement alongside with {@code skip locked}.
     * <p>
     * Use this method only inside manually started transaction otherwise lock will not be attempted.
     *
     * @param size how many rows to retrieve
     * @return first N entities
     */
    @Query(value = "select * from planned_backup_tasks where state = :#{#state.name()} limit :#{#size} FOR UPDATE skip locked", nativeQuery = true)
    Iterable<PlannedTask> findFirstNByState(@Param(value = "size") Integer size, @Param(value = "state") PlannedTask.State state);
}
