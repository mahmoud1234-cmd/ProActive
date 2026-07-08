package com.proactive.demo.controller;

import com.proactive.demo.dto.task.TaskRequest;
import com.proactive.demo.dto.task.TaskResponse;
import com.proactive.demo.model.Task;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.TaskService;
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
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskService taskService;
    private final UserRepository userRepository;

    /** Toutes les tâches d'un projet */
    @GetMapping("/projects/{projectId}/tasks")
    public ResponseEntity<List<TaskResponse>> getByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getByProject(projectId));
    }

    /** Statistiques d'un projet */
    @GetMapping("/projects/{projectId}/tasks/stats")
    public ResponseEntity<Map<String, Long>> getStats(@PathVariable Long projectId) {
        return ResponseEntity.ok(taskService.getStats(projectId));
    }

    /** Mes tâches (assignées ou créées) */
    @GetMapping("/tasks/mine")
    public ResponseEntity<List<TaskResponse>> getMyTasks() {
        return ResponseEntity.ok(taskService.getMyTasks());
    }

    /** Détail d'une tâche */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TaskResponse> getById(@PathVariable Long taskId) {
        return ResponseEntity.ok(taskService.getById(taskId));
    }

    /** Créer une tâche dans un projet */
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> create(@PathVariable Long projectId,
                                    @Valid @RequestBody TaskRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(projectId, req));
    }

    /** Modifier une tâche */
    @PutMapping("/tasks/{taskId}")
    public ResponseEntity<?> update(@PathVariable Long taskId,
                                    @Valid @RequestBody TaskRequest req) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.ok(taskService.update(taskId, req));
    }

    /** Changer uniquement le statut (drag & drop) */
    @PutMapping("/tasks/{taskId}/status")
    public ResponseEntity<?> updateStatus(@PathVariable Long taskId,
                                           @RequestParam Task.Status status) {
        if (!canWrite()) return forbidden();
        return ResponseEntity.ok(taskService.updateStatus(taskId, status));
    }

    /** Supprimer une tâche */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<?> delete(@PathVariable Long taskId) {
        if (!canWrite()) return forbidden();
        taskService.delete(taskId);
        return ResponseEntity.ok(Map.of("message", "Tâche supprimée"));
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
