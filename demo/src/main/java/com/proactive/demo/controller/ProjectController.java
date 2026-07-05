package com.proactive.demo.controller;

import com.proactive.demo.dto.project.ProjectRequest;
import com.proactive.demo.dto.project.ProjectResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectController {

    private final ProjectService projectService;
    private final UserRepository userRepository;

    /** Liste les projets (tous pour ADMIN, les siens pour MANAGER/USER) */
    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getProjects() {
        return ResponseEntity.ok(projectService.getProjectsForCurrentUser());
    }

    /** Statistiques des projets */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Long>> getStats() {
        return ResponseEntity.ok(projectService.getStats());
    }

    /** Détail d'un projet */
    @GetMapping("/{id}")
    public ResponseEntity<ProjectResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getById(id));
    }

    /** Créer un projet (ADMIN ou MANAGER) */
    @PostMapping
    public ResponseEntity<?> create(@Valid @RequestBody ProjectRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.create(req));
    }

    /** Modifier un projet */
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody ProjectRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.ok(projectService.update(id, req));
    }

    /** Supprimer un projet (ADMIN uniquement) */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!isAdmin()) return forbidden();
        projectService.delete(id);
        return ResponseEntity.ok(Map.of("message", "Projet supprimé"));
    }

    // ── Helpers ───────────────────────────────────────────────────────────
    private User.Role currentRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return null;
        return userRepository.findByEmail(auth.getName()).map(User::getRole).orElse(null);
    }

    private boolean isAdmin()  { return currentRole() == User.Role.ADMIN; }
    private boolean canWrite() {
        User.Role r = currentRole();
        return r == User.Role.ADMIN || r == User.Role.MANAGER;
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Accès refusé"));
    }
}
