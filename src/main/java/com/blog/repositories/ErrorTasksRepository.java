package com.blog.repositories;

import com.blog.entities.backup.ErrorTask;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.PagingAndSortingRepository;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;
import java.util.Optional;

public interface ErrorTasksRepository extends CrudRepository<ErrorTask, Integer>, PagingAndSortingRepository<ErrorTask, Integer> {
    Optional<ErrorTask> findByTaskId(Integer id);

    /**
     * Returns first page that is not locked and sets pessimistic lock on retrieved page.
     * <p>
     * This method uses {@code select for update} statement alongside with {@code skip locked}.
     * <p>
     * Use this method only inside manually started transaction otherwise lock will not be attempted.
     * <p>
     * Reading by other transactions is still able without being locked.
     *
     * @param page {@literal Pageable} instance describing page and limit.
     * @return page of entities
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints({@QueryHint(name = "javax.persistence.lock.timeout", value = "-2000")})
    @NotNull
    Page<ErrorTask> findAll(@NotNull Pageable page);

    boolean existsByTaskId(Integer taskId);
}
