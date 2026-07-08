package com.proactive.demo.repository;

import com.proactive.demo.model.Project;
import com.proactive.demo.model.ProjectModule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProjectModuleRepository extends JpaRepository<ProjectModule, Long> {

    /** Modules racines d'un projet (sans parent), triés */
    List<ProjectModule> findAllByProjectAndParentIsNullOrderBySortOrderAsc(Project project);

    /** Tous les modules d'un projet (pour recalcul avancement) */
    List<ProjectModule> findAllByProject(Project project);

    /** Compter les modules racines */
    long countByProjectAndParentIsNull(Project project);
}
