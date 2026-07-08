package com.proactive.demo.repository;

import com.proactive.demo.model.Project;
import com.proactive.demo.model.Task;
import com.proactive.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findAllByProjectOrderByCreatedAtDesc(Project project);

    List<Task> findAllByProjectAndStatusOrderByCreatedAtDesc(Project project, Task.Status status);

    List<Task> findAllByAssigneeOrderByDueDateAsc(User assignee);

    List<Task> findAllByCreatedByOrderByCreatedAtDesc(User createdBy);

    @Query("SELECT t FROM Task t WHERE t.assignee = :user OR t.createdBy = :user ORDER BY t.createdAt DESC")
    List<Task> findAllByAssigneeOrCreatedBy(User user);

    long countByProjectAndStatus(Project project, Task.Status status);

    long countByProject(Project project);
}
