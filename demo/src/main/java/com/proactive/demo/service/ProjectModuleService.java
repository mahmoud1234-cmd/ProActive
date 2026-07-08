package com.proactive.demo.service;

import com.proactive.demo.dto.module.ModuleRequest;
import com.proactive.demo.dto.module.ModuleResponse;
import com.proactive.demo.model.Project;
import com.proactive.demo.model.ProjectModule;
import com.proactive.demo.repository.ProjectModuleRepository;
import com.proactive.demo.repository.ProjectRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectModuleService {

    private final ProjectModuleRepository moduleRepository;
    private final ProjectRepository projectRepository;

    // ── Lecture ──────────────────────────────────────────────────────────

    /** Retourne les modules racines d'un projet (avec leurs enfants imbriqués) */
    public List<ModuleResponse> getModules(Long projectId) {
        Project project = getProject(projectId);
        return moduleRepository
                .findAllByProjectAndParentIsNullOrderBySortOrderAsc(project)
                .stream()
                .map(ModuleResponse::from)
                .toList();
    }

    // ── Création ─────────────────────────────────────────────────────────

    @Transactional
    public ModuleResponse create(Long projectId, ModuleRequest req) {
        Project project = getProject(projectId);

        ProjectModule module = new ProjectModule();
        module.setProject(project);
        module.setName(req.getName());
        module.setDescription(req.getDescription());
        module.setProgressPct(req.getProgressPct() != null ? req.getProgressPct() : 0);
        module.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);

        if (req.getParentId() != null) {
            ProjectModule parent = moduleRepository.findById(req.getParentId())
                    .orElseThrow(() -> new RuntimeException("Module parent non trouvé"));
            module.setParent(parent);
        }

        ProjectModule saved = moduleRepository.save(module);
        recalculateProjectProgress(project);
        return ModuleResponse.from(saved);
    }

    // ── Mise à jour ───────────────────────────────────────────────────────

    @Transactional
    public ModuleResponse update(Long moduleId, ModuleRequest req) {
        ProjectModule module = getModule(moduleId);

        if (req.getName()        != null) module.setName(req.getName());
        if (req.getDescription() != null) module.setDescription(req.getDescription());
        if (req.getProgressPct() != null) module.setProgressPct(req.getProgressPct());
        if (req.getSortOrder()   != null) module.setSortOrder(req.getSortOrder());

        ProjectModule saved = moduleRepository.save(module);
        recalculateProjectProgress(module.getProject());
        return ModuleResponse.from(saved);
    }

    /** Met l'avancement à 100% (tick) ou 0% (untick) */
    @Transactional
    public ModuleResponse tick(Long moduleId, boolean done) {
        ProjectModule module = getModule(moduleId);

        // Si ce module n'a pas d'enfants, on modifie directement son progressPct
        if (module.getChildren().isEmpty()) {
            module.setProgressPct(done ? 100 : 0);
        } else {
            // Si il a des enfants, on tick/untick tous les enfants leaf
            tickRecursive(module, done);
        }

        moduleRepository.save(module);
        recalculateProjectProgress(module.getProject());

        // Recharger pour avoir les valeurs calculées à jour
        ProjectModule refreshed = moduleRepository.findById(moduleId)
                .orElse(module);
        return ModuleResponse.from(refreshed);
    }

    private void tickRecursive(ProjectModule m, boolean done) {
        if (m.getChildren().isEmpty()) {
            m.setProgressPct(done ? 100 : 0);
            moduleRepository.save(m);
        } else {
            m.getChildren().forEach(child -> tickRecursive(child, done));
        }
    }

    // ── Suppression ───────────────────────────────────────────────────────

    @Transactional
    public void delete(Long moduleId) {
        ProjectModule module = getModule(moduleId);
        Project project = module.getProject();
        moduleRepository.delete(module);
        recalculateProjectProgress(project);
    }

    // ── Recalcul avancement projet ────────────────────────────────────────

    /**
     * Recalcule l'avancement global du projet
     * = moyenne des avancements effectifs de tous ses modules racines
     */
    @Transactional
    public void recalculateProjectProgress(Project project) {
        List<ProjectModule> roots = moduleRepository
                .findAllByProjectAndParentIsNullOrderBySortOrderAsc(project);

        if (roots.isEmpty()) return;

        double avg = roots.stream()
                .mapToInt(ProjectModule::getEffectiveProgress)
                .average()
                .orElse(0.0);

        int newProgress = (int) Math.round(avg);
        project.setProgressPct(newProgress);
        projectRepository.save(project);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private Project getProject(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
    }

    private ProjectModule getModule(Long id) {
        return moduleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Module non trouvé"));
    }
}
