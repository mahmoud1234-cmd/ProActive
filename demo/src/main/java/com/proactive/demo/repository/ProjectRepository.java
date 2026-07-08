package com.proactive.demo.repository;

import com.proactive.demo.model.Project;
import com.proactive.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    List<Project> findAllByOrderByCreatedAtDesc();

    List<Project> findAllByManagerOrderByCreatedAtDesc(User manager);

    List<Project> findAllByStatusOrderByCreatedAtDesc(Project.Status status);

    List<Project> findAllByCreatedByOrderByCreatedAtDesc(User createdBy);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.members m WHERE p.manager = :user OR p.createdBy = :user OR m.user = :user ORDER BY p.createdAt DESC")
    List<Project> findAllAccessibleByUser(User user);

    long countByStatus(Project.Status status);
}
