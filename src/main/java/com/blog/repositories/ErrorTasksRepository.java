package com.blog.repositories;

import com.blog.entities.task.ErrorTask;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.Collection;

public interface ErrorTasksRepository extends CrudRepository<ErrorTask, Integer> {
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
    @Query(value = "select * from error_tasks limit :#{#size} FOR UPDATE skip locked", nativeQuery = true)
    Iterable<ErrorTask> findFirstNAndLock(@Param(value = "size") Integer size);

    boolean existsByTaskId(Integer taskId);

    Iterable<ErrorTask> findAllByTaskIdIn(Collection<Integer> ids);
}
