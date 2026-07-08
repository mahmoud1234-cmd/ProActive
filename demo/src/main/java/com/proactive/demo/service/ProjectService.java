package com.proactive.demo.service;

import com.proactive.demo.dto.project.ProjectRequest;
import com.proactive.demo.dto.project.ProjectResponse;
import com.proactive.demo.model.Project;
import com.proactive.demo.model.ProjectMember;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.ProjectRepository;
import com.proactive.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
        return projectRepository.findAllAccessibleByUser(current)
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
        if (req.getType()        != null) p.setType(req.getType());
        if (req.getMethodology() != null) p.setMethodology(req.getMethodology());
        if (req.getBudget()      != null) p.setBudget(req.getBudget());
        if (req.getProgressPct() != null) p.setProgressPct(req.getProgressPct());

        if (req.getTools() != null) {
            // tools est maintenant une String directe (ex: "Jira,Slack,GitHub")
            p.setTools(req.getTools().isBlank() ? null : req.getTools().trim());
        }

        syncTeam(p, req);
    }

    private void syncTeam(Project p, ProjectRequest req) {
        boolean hasTeamUpdate = req.getManagerIds() != null || req.getMemberIds() != null;

        if (hasTeamUpdate) {
            p.getMembers().clear();

            Set<Long> assigned = new HashSet<>();

            if (req.getManagerIds() != null) {
                for (Long id : req.getManagerIds()) {
                    if (id == null || assigned.contains(id)) continue;
                    User user = userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Manager non trouvé: " + id));
                    ProjectMember pm = new ProjectMember();
                    pm.setProject(p);
                    pm.setUser(user);
                    pm.setProjectRole(ProjectMember.ProjectRole.MANAGER);
                    p.getMembers().add(pm);
                    assigned.add(id);
                }
            }

            if (req.getMemberIds() != null) {
                for (Long id : req.getMemberIds()) {
                    if (id == null || assigned.contains(id)) continue;
                    User user = userRepository.findById(id)
                            .orElseThrow(() -> new RuntimeException("Membre non trouvé: " + id));
                    ProjectMember pm = new ProjectMember();
                    pm.setProject(p);
                    pm.setUser(user);
                    pm.setProjectRole(ProjectMember.ProjectRole.MEMBER);
                    p.getMembers().add(pm);
                    assigned.add(id);
                }
            }

            if (req.getManagerIds() != null && !req.getManagerIds().isEmpty()) {
                User primary = userRepository.findById(req.getManagerIds().get(0))
                        .orElseThrow(() -> new RuntimeException("Manager principal non trouvé"));
                p.setManager(primary);
            } else if (req.getManagerIds() != null && req.getManagerIds().isEmpty()) {
                p.setManager(null);
            }
        } else if (req.getManagerId() != null) {
            User manager = userRepository.findById(req.getManagerId())
                    .orElseThrow(() -> new RuntimeException("Manager non trouvé"));
            p.setManager(manager);

            boolean alreadyMember = p.getMembers().stream()
                    .anyMatch(m -> m.getUser().getId().equals(req.getManagerId()));
            if (!alreadyMember) {
                ProjectMember pm = new ProjectMember();
                pm.setProject(p);
                pm.setUser(manager);
                pm.setProjectRole(ProjectMember.ProjectRole.MANAGER);
                p.getMembers().add(pm);
            }
        }
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
    }
}
