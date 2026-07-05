package com.proactive.demo.controller;

import com.proactive.demo.dto.EditUserRequest;
import com.proactive.demo.dto.PendingUserResponse;
import com.proactive.demo.dto.UserSummaryResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserPermissionRepository;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.EmailService;
import com.proactive.demo.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;
    private final PermissionService permissionService;
    private final EmailService emailService;

    /** Tous les utilisateurs (non-PENDING) avec filtre optionnel par rôle et statut */
    @GetMapping
    public ResponseEntity<?> getAllUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status) {
        if (!canViewUsers()) return forbidden();

        List<UserSummaryResponse> users = userRepository.findAll()
                .stream()
                .filter(u -> u.getStatus() != User.Status.PENDING)
                .filter(u -> role == null || u.getRole().name().equalsIgnoreCase(role))
                .filter(u -> status == null || (u.getStatus() != null && u.getStatus().name().equalsIgnoreCase(status)))
                .map(UserSummaryResponse::from)
                .toList();
        return ResponseEntity.ok(users);
    }

    /** Détail d'un utilisateur */
    @GetMapping("/{userId}")
    public ResponseEntity<?> getUserById(@PathVariable Long userId) {
        if (!canViewUsers()) return forbidden();
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(UserSummaryResponse.from(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    /** Modifier un utilisateur (nom, email, rôle) */
    @PatchMapping("/{userId}")
    public ResponseEntity<?> updateUser(@PathVariable Long userId,
                                         @Valid @RequestBody EditUserRequest req) {
        if (!isAdmin()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (user.getRole() == User.Role.ADMIN && !isCurrentUser(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Impossible de modifier un autre administrateur"));
        }
        if (req.getFirstName() != null) user.setFirstName(req.getFirstName());
        if (req.getLastName()  != null) user.setLastName(req.getLastName());
        if (req.getEmail()     != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail()))
                return ResponseEntity.badRequest().body(Map.of("message", "Cet email est déjà utilisé"));
            user.setEmail(req.getEmail());
        }
        if (req.getRole() != null) user.setRole(req.getRole());
        userRepository.save(user);
        return ResponseEntity.ok(UserSummaryResponse.from(user));
    }

    /** Bannir un utilisateur */
    @PutMapping("/{userId}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long userId) {
        if (!isAdmin()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (user.getRole() == User.Role.ADMIN)
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("message", "Impossible de bannir un administrateur"));
        user.setStatus(User.Status.BANNED);
        userRepository.save(user);
        return ResponseEntity.ok(UserSummaryResponse.from(user));
    }

    /** Débannir un utilisateur */
    @PutMapping("/{userId}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long userId) {
        if (!isAdmin()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(User.Status.APPROVED);
        userRepository.save(user);
        return ResponseEntity.ok(UserSummaryResponse.from(user));
    }

    /** Supprimer un utilisateur */
    @DeleteMapping("/{userId}")
    @jakarta.transaction.Transactional
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        if (!isAdmin()) return forbidden();
        if (isCurrentUser(userId))
            return ResponseEntity.badRequest().body(Map.of("message", "Impossible de supprimer votre propre compte"));
        // Supprimer d'abord les permissions liées (contrainte FK)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        userPermissionRepository.deleteAllByUser(user);
        userRepository.delete(user);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }

    // ── Approval endpoints ───────────────────────────────────────────────

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingUsers() {
        // READ_ONLY suffit pour voir les pending
        if (!isAdmin() && !permissionService.hasAtLeast("APPROVALS", "READ_ONLY")) return forbidden();
        List<PendingUserResponse> pending = userRepository.findAllByStatus(User.Status.PENDING)
                .stream().map(PendingUserResponse::from).toList();
        return ResponseEntity.ok(pending);
    }

    @PutMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        // READ_WRITE requis pour approuver
        if (!canManageApprovals()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(User.Status.APPROVED);
        userRepository.save(user);
        getCurrentUser().ifPresent(admin -> emailService.sendApprovalEmail(user, admin));
        return ResponseEntity.ok(PendingUserResponse.from(user));
    }

    @PutMapping("/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        if (!canManageApprovals()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(User.Status.REJECTED);
        userRepository.save(user);
        getCurrentUser().ifPresent(admin -> emailService.sendRejectionEmail(user, admin));
        return ResponseEntity.ok(PendingUserResponse.from(user));
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    private String currentEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : null;
    }

    private Optional<User> getCurrentUser() {
        String email = currentEmail();
        if (email == null) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    private User.Role currentRole() {
        return getCurrentUser().map(User::getRole).orElse(null);
    }

    private boolean isAdmin() { return currentRole() == User.Role.ADMIN; }

    private boolean isCurrentUser(Long userId) {
        return getCurrentUser().map(u -> u.getId().equals(userId)).orElse(false);
    }

    private boolean canViewUsers() {
        if (isAdmin()) return true;
        return permissionService.hasAtLeast("USERS", "READ_ONLY");
    }

    private boolean canManageApprovals() {
        // READ_WRITE requis pour approuver/rejeter
        if (isAdmin()) return true;
        return permissionService.hasAtLeast("APPROVALS", "READ_WRITE");
    }

    private boolean canViewApprovals() {
        if (isAdmin()) return true;
        return permissionService.hasAtLeast("APPROVALS", "READ_ONLY");
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Accès refusé"));
    }
}
