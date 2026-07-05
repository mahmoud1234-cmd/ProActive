package com.proactive.demo.controller;

import com.proactive.demo.dto.PendingUserResponse;
import com.proactive.demo.dto.UserSummaryResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.service.EmailService;
import com.proactive.demo.service.PermissionService;
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
    private final PermissionService permissionService;
    private final EmailService emailService;

    /** Liste tous les utilisateurs approuvés */
    @GetMapping
    public ResponseEntity<?> getAllUsers() {
        if (!isAdmin()) return forbidden();
        List<UserSummaryResponse> users = userRepository.findAllByStatus(User.Status.APPROVED)
                .stream().map(UserSummaryResponse::from).toList();
        return ResponseEntity.ok(users);
    }

    /** Comptes en attente */
    @GetMapping("/pending")
    public ResponseEntity<?> getPendingUsers() {
        if (!canManageApprovals()) return forbidden();
        List<PendingUserResponse> pending = userRepository.findAllByStatus(User.Status.PENDING)
                .stream().map(PendingUserResponse::from).toList();
        return ResponseEntity.ok(pending);
    }

    /** Approuver */
    @PutMapping("/{userId}/approve")
    public ResponseEntity<?> approveUser(@PathVariable Long userId) {
        if (!canManageApprovals()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(User.Status.APPROVED);
        userRepository.save(user);
        // Email asynchrone
        getCurrentUser().ifPresent(admin -> emailService.sendApprovalEmail(user, admin));
        return ResponseEntity.ok(PendingUserResponse.from(user));
    }

    /** Rejeter */
    @PutMapping("/{userId}/reject")
    public ResponseEntity<?> rejectUser(@PathVariable Long userId) {
        if (!canManageApprovals()) return forbidden();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatus(User.Status.REJECTED);
        userRepository.save(user);
        // Email asynchrone
        getCurrentUser().ifPresent(admin -> emailService.sendRejectionEmail(user, admin));
        return ResponseEntity.ok(PendingUserResponse.from(user));
    }

    /** Supprimer */
    @DeleteMapping("/{userId}")
    public ResponseEntity<?> deleteUser(@PathVariable Long userId) {
        if (!isAdmin()) return forbidden();
        userRepository.deleteById(userId);
        return ResponseEntity.ok(Map.of("message", "Utilisateur supprimé"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────

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

    private boolean isAdmin() {
        return currentRole() == User.Role.ADMIN;
    }

    private boolean canManageApprovals() {
        if (isAdmin()) return true;
        return permissionService.hasAtLeast("APPROVALS", "READ_WRITE");
    }

    private ResponseEntity<Map<String, String>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("message", "Accès refusé"));
    }
}
