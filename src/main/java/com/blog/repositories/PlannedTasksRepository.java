package com.blog.repositories;

import com.blog.entities.backup.PlannedTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.ArrayList;

public interface PlannedTasksRepository extends CrudRepository<PlannedTask, Integer>,
        PagingAndSortingRepository<PlannedTask, Integer> {
    ArrayList<PlannedTask> findAllByOrderByIdDesc();

    /**
     * Returns first page that is not locked. Also sets pessimistic lock on retrieved page in case of method is called in transaction block.
     * <p>
     * This method uses {@code select for update} statement alongside with {@code skip locked}.
     * <p>
     * If you want to acquire a pessimistic lock on retrieved page, use this method inside transaction block, otherwise lock will not be
     * attempted.
     * <p>
     *
     * @param page {@literal Pageable} instance describing page and limit.
     * @return page of entities
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "-2")})
    @NotNull
    Page<PlannedTask> findAllByState(@NotNull Pageable page, @NotNull PlannedTask.State state);
}
