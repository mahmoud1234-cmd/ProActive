package com.proactive.demo.repository;

import com.proactive.demo.model.Project;
import com.proactive.demo.model.ProjectMember;
import com.proactive.demo.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
    List<ProjectMember> findAllByProject(Project project);
    Optional<ProjectMember> findByProjectAndUser(Project project, User user);
    boolean existsByProjectAndUser(Project project, User user);
    void deleteByProjectAndUser(Project project, User user);
}
