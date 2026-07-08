package com.proactive.demo.controller;

import com.proactive.demo.dto.module.ModuleRequest;
import com.proactive.demo.dto.module.ModuleResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.ProjectModuleService;
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
@RequestMapping("/projects/{projectId}/modules")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProjectModuleController {

    private final ProjectModuleService moduleService;
    private final UserRepository userRepository;

    /** Lister les modules d'un projet */
    @GetMapping
    public ResponseEntity<List<ModuleResponse>> getModules(@PathVariable Long projectId) {
        return ResponseEntity.ok(moduleService.getModules(projectId));
    }

    /** Créer un module (racine ou sous-module) */
    @PostMapping
    public ResponseEntity<?> createModule(@PathVariable Long projectId,
                                           @Valid @RequestBody ModuleRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(moduleService.create(projectId, req));
    }

    /** Modifier un module */
    @PutMapping("/{moduleId}")
    public ResponseEntity<?> updateModule(@PathVariable Long projectId,
                                           @PathVariable Long moduleId,
                                           @Valid @RequestBody ModuleRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.ok(moduleService.update(moduleId, req));
    }

    /** Cocher un module (avancement 100%) ou décocher (0%) */
    @PutMapping("/{moduleId}/tick")
    public ResponseEntity<?> tickModule(@PathVariable Long projectId,
                                         @PathVariable Long moduleId,
                                         @RequestParam(defaultValue = "true") boolean done) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.ok(moduleService.tick(moduleId, done));
    }

    /** Supprimer un module (et ses sous-modules) */
    @DeleteMapping("/{moduleId}")
    public ResponseEntity<?> deleteModule(@PathVariable Long projectId,
                                           @PathVariable Long moduleId) {
        if (!canWrite()) return forbidden();
        moduleService.delete(moduleId);
        return ResponseEntity.ok(Map.of("message", "Module supprimé"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────
    private boolean canWrite() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return false;
        return userRepository.findByEmail(auth.getName())
                .map(u -> u.getRole() == User.Role.ADMIN || u.getRole() == User.Role.MANAGER)
                .orElse(false);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Accès refusé"));
    }
}
