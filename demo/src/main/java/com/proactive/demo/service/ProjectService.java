package com.proactive.demo.service;

import com.proactive.demo.dto.project.ProjectRequest;
import com.proactive.demo.dto.project.ProjectResponse;
import com.proactive.demo.model.Project;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.ProjectRepository;
import com.proactive.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    // ── Lecture ──────────────────────────────────────────────────────────

    /** Retourne les projets selon le rôle de l'utilisateur connecté */
    public List<ProjectResponse> getProjectsForCurrentUser() {
        User current = currentUser();
        if (current.getRole() == User.Role.ADMIN) {
            return projectRepository.findAllByOrderByCreatedAtDesc()
                    .stream().map(ProjectResponse::from).toList();
        }
        // Manager/User : uniquement les projets où il est manager ou créateur
        return projectRepository.findAllByManagerOrCreatedByOrderByCreatedAtDesc(current)
                .stream().map(ProjectResponse::from).toList();
    }

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAllByOrderByCreatedAtDesc()
                .stream().map(ProjectResponse::from).toList();
    }

    public ProjectResponse getById(Long id) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        return ProjectResponse.from(p);
    }

    // ── Statistiques ──────────────────────────────────────────────────────

    public Map<String, Long> getStats() {
        return Map.of(
            "total",     projectRepository.count(),
            "planning",  projectRepository.countByStatus(Project.Status.PLANNING),
            "active",    projectRepository.countByStatus(Project.Status.ACTIVE),
            "on_hold",   projectRepository.countByStatus(Project.Status.ON_HOLD),
            "completed", projectRepository.countByStatus(Project.Status.COMPLETED),
            "cancelled", projectRepository.countByStatus(Project.Status.CANCELLED)
        );
    }

    // ── Écriture ──────────────────────────────────────────────────────────

    public ProjectResponse create(ProjectRequest req) {
        Project p = new Project();
        applyRequest(p, req);
        p.setCreatedBy(currentUser());
        return ProjectResponse.from(projectRepository.save(p));
    }

    public ProjectResponse update(Long id, ProjectRequest req) {
        Project p = projectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
        applyRequest(p, req);
        return ProjectResponse.from(projectRepository.save(p));
    }

    public void delete(Long id) {
        if (!projectRepository.existsById(id))
            throw new RuntimeException("Projet non trouvé");
        projectRepository.deleteById(id);
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void applyRequest(Project p, ProjectRequest req) {
        if (req.getName()        != null) p.setName(req.getName());
        if (req.getDescription() != null) p.setDescription(req.getDescription());
        if (req.getStartDate()   != null) p.setStartDate(req.getStartDate());
        if (req.getEndDate()     != null) p.setEndDate(req.getEndDate());
        if (req.getStatus()      != null) p.setStatus(req.getStatus());
        if (req.getPriority()    != null) p.setPriority(req.getPriority());
        if (req.getBudget()      != null) p.setBudget(req.getBudget());
        if (req.getProgressPct() != null) p.setProgressPct(req.getProgressPct());

        if (req.getManagerId() != null) {
            User manager = userRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
            p.setManager(manager);
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}
