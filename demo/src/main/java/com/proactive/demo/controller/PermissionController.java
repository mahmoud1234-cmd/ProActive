package com.proactive.demo.controller;

import com.proactive.demo.dto.permission.PermissionBulkUpdateRequest;
import com.proactive.demo.dto.permission.RolePermissionResponse;
import com.proactive.demo.dto.permission.UserPermissionViewResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PermissionController {

    private final PermissionService permissionService;
    private final UserRepository userRepository;

    /** Permissions de l'utilisateur connecté */
    @GetMapping("/me")
    public ResponseEntity<UserPermissionViewResponse> getMyPermissions() {
        return ResponseEntity.ok(permissionService.getPermissionsForCurrentUser());
    }

    /** Permissions d'un utilisateur spécifique (ADMIN) */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserPermissions(@PathVariable Long userId) {
        if (!isAdmin()) return forbidden();
        return ResponseEntity.ok(permissionService.getPermissionsByUserId(userId));
    }

    /** Mise à jour des permissions d'un utilisateur spécifique (ADMIN) */
    @PutMapping("/users/{userId}")
    public ResponseEntity<?> updateUserPermissions(
            @PathVariable Long userId,
            @Valid @RequestBody PermissionBulkUpdateRequest request) {
        if (!isAdmin()) return forbidden();
        return ResponseEntity.ok(permissionService.updatePermissions(userId, request.getPermissions()));
    }

    /** Permissions effectives d'un rôle (ADMIN) */
    @GetMapping("/roles/{role}")
    public ResponseEntity<?> getRolePermissions(@PathVariable User.Role role) {
        if (!isAdmin()) return forbidden();
        return ResponseEntity.ok(permissionService.getDefaultPermissionsForRole(role));
    }

    /** Appliquer un template de permissions à tous les users d'un rôle (ADMIN) */
    @PutMapping("/roles/{role}")
    public ResponseEntity<?> applyPermissionsToRole(
            @PathVariable User.Role role,
            @Valid @RequestBody PermissionBulkUpdateRequest request) {
        if (!isAdmin()) return forbidden();
        return ResponseEntity.ok(permissionService.applyPermissionsToRole(role, request.getPermissions()));
    }

    // ── Helpers ──────────────────────────────────────────────────
    private boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .map(u -> u.getRole() == User.Role.ADMIN)
                .orElse(false);
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Accès réservé aux administrateurs"));
    }
}
