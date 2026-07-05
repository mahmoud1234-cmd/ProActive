package com.proactive.demo.service;

import com.proactive.demo.dto.permission.FeaturePermissionView;
import com.proactive.demo.dto.permission.PermissionAssignmentRequest;
import com.proactive.demo.dto.permission.RolePermissionResponse;
import com.proactive.demo.dto.permission.UserPermissionViewResponse;
import com.proactive.demo.model.AccessLevel;
import com.proactive.demo.model.PermissionFeature;
import com.proactive.demo.model.User;
import com.proactive.demo.model.UserPermission;
import com.proactive.demo.repository.UserPermissionRepository;
import com.proactive.demo.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service("permissionService")
@RequiredArgsConstructor
public class PermissionService {

    private final UserRepository userRepository;
    private final UserPermissionRepository userPermissionRepository;

    // ── Vérification d'accès ──────────────────────────────────────────────

    public boolean hasAtLeast(String feature, String requiredLevel) {
        try {
            return hasAtLeast(PermissionFeature.valueOf(feature), AccessLevel.valueOf(requiredLevel));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public boolean hasAtLeast(PermissionFeature feature, AccessLevel requiredLevel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;
        return resolveUser(auth)
                .map(u -> getEffectiveAccess(u, feature).ordinal() >= requiredLevel.ordinal())
                .orElse(false);
    }

    /** ADMIN ou droit PERMISSIONS suffisant (contrôle via la base, pas seulement les authorities JWT). */
    public boolean canManagePermissions(String requiredLevel) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) return false;

        if (hasAdminAuthority(auth)) return true;

        return resolveUser(auth)
                .map(user -> {
                    if (user.getRole() == User.Role.ADMIN) return true;
                    try {
                        AccessLevel min = AccessLevel.valueOf(requiredLevel);
                        return getEffectiveAccess(user, PermissionFeature.PERMISSIONS).ordinal() >= min.ordinal();
                    } catch (IllegalArgumentException ex) {
                        return false;
                    }
                })
                .orElse(false);
    }

    private boolean hasAdminAuthority(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }

    private Optional<User> resolveUser(Authentication auth) {
        if (auth.getPrincipal() instanceof User user) {
            return Optional.of(user);
        }
        String email = auth.getPrincipal() instanceof UserDetails ud
                ? ud.getUsername()
                : auth.getName();
        if (email == null || email.isBlank()) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    // ── Lecture ───────────────────────────────────────────────────────────

    public UserPermissionViewResponse getPermissionsForCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated())
            throw new RuntimeException("Utilisateur non authentifié");
        User user = resolveUser(auth)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return buildView(user);
    }

    public UserPermissionViewResponse getPermissionsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        return buildView(user);
    }

    /**
     * Retourne les permissions effectives du rôle.
     * Si des permissions custom existent en base pour ce rôle, on les retourne.
     * Sinon on retourne les valeurs par défaut hardcodées.
     */
    public RolePermissionResponse getDefaultPermissionsForRole(User.Role role) {
        if (role == User.Role.ADMIN)
            throw new RuntimeException("Les permissions ADMIN ne sont pas modifiables");

        // Chercher un utilisateur du rôle qui a des permissions custom
        List<User> usersOfRole = userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();

        // Si au moins un user du rôle a des permissions custom, on les utilise comme référence
        // (on prend le premier user du rôle — les permissions par rôle sont identiques pour tous)
        List<FeaturePermissionView> features;
        if (!usersOfRole.isEmpty()) {
            User referenceUser = usersOfRole.get(0);
            Map<PermissionFeature, UserPermission> explicit = userPermissionRepository
                    .findAllByUser(referenceUser)
                    .stream()
                    .collect(Collectors.toMap(UserPermission::getFeature, Function.identity()));

            features = Arrays.stream(PermissionFeature.values())
                    .map(f -> {
                        UserPermission custom = explicit.get(f);
                        AccessLevel level = custom != null ? custom.getAccessLevel()
                                                           : getRoleDefaultAccess(role, f);
                        return new FeaturePermissionView(f, level, custom != null);
                    })
                    .toList();
        } else {
            // Aucun user de ce rôle → valeurs par défaut
            features = Arrays.stream(PermissionFeature.values())
                    .map(f -> new FeaturePermissionView(f, getRoleDefaultAccess(role, f), false))
                    .toList();
        }

        return new RolePermissionResponse(role, features, usersOfRole.size());
    }

    // ── Mise à jour ───────────────────────────────────────────────────────

    @Transactional
    public UserPermissionViewResponse updatePermissions(Long userId, List<PermissionAssignmentRequest> assignments) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        if (target.getRole() == User.Role.ADMIN)
            throw new RuntimeException("Les permissions d'un ADMIN ne peuvent pas être modifiées");

        applyAssignmentsToUser(target, assignments);
        return buildView(target);
    }

    /** Applique un template de permissions à TOUS les utilisateurs d'un rôle */
    @Transactional
    public RolePermissionResponse applyPermissionsToRole(User.Role role, List<PermissionAssignmentRequest> assignments) {
        if (role == User.Role.ADMIN)
            throw new RuntimeException("Les permissions ADMIN ne sont pas modifiables");

        List<User> targets = userRepository.findAll().stream()
                .filter(u -> u.getRole() == role)
                .toList();

        for (User user : targets) {
            applyAssignmentsToUser(user, assignments);
        }

        // Retourner le template appliqué (basé sur les valeurs demandées, pas sur un user)
        List<FeaturePermissionView> features = assignments.stream()
                .map(a -> new FeaturePermissionView(a.getFeature(), a.getAccessLevel(), true))
                .toList();

        return new RolePermissionResponse(role, features, targets.size());
    }

    // ── Helpers ───────────────────────────────────────────────────────────

    private void applyAssignmentsToUser(User user, List<PermissionAssignmentRequest> assignments) {
        Map<PermissionFeature, AccessLevel> requested = assignments.stream()
                .collect(Collectors.toMap(
                        PermissionAssignmentRequest::getFeature,
                        PermissionAssignmentRequest::getAccessLevel,
                        (a, b) -> b));

        Map<PermissionFeature, UserPermission> existing = userPermissionRepository.findAllByUser(user)
                .stream()
                .collect(Collectors.toMap(UserPermission::getFeature, Function.identity()));

        for (Map.Entry<PermissionFeature, AccessLevel> entry : requested.entrySet()) {
            UserPermission perm = existing.getOrDefault(entry.getKey(), null);
            if (perm == null) {
                perm = new UserPermission();
                perm.setUser(user);
                perm.setFeature(entry.getKey());
            }
            perm.setAccessLevel(entry.getValue());
            userPermissionRepository.save(perm);
        }
    }

    public AccessLevel getEffectiveAccess(User user, PermissionFeature feature) {
        if (user.getRole() == User.Role.ADMIN) return AccessLevel.FULL_ACCESS;
        return userPermissionRepository.findByUserAndFeature(user, feature)
                .map(UserPermission::getAccessLevel)
                .orElseGet(() -> getRoleDefaultAccess(user.getRole(), feature));
    }

    private AccessLevel getRoleDefaultAccess(User.Role role, PermissionFeature feature) {
        if (role == User.Role.MANAGER) {
            return switch (feature) {
                case DASHBOARD, PROJECTS, TASKS, REPORTS -> AccessLevel.READ_WRITE;
                case USERS    -> AccessLevel.READ_ONLY;
                case APPROVALS -> AccessLevel.READ_WRITE;  // Manager peut approuver par défaut
                case PERMISSIONS -> AccessLevel.NO_ACCESS;
            };
        }
        // USER
        return switch (feature) {
            case DASHBOARD, PROJECTS, TASKS, REPORTS -> AccessLevel.READ_ONLY;
            case USERS, APPROVALS, PERMISSIONS -> AccessLevel.NO_ACCESS;
        };
    }

    private UserPermissionViewResponse buildView(User user) {
        Map<PermissionFeature, UserPermission> explicit = userPermissionRepository.findAllByUser(user)
                .stream()
                .collect(Collectors.toMap(UserPermission::getFeature, Function.identity()));

        List<FeaturePermissionView> featureViews = Arrays.stream(PermissionFeature.values())
                .map(feature -> {
                    UserPermission custom = explicit.get(feature);
                    AccessLevel effective = custom != null ? custom.getAccessLevel()
                                                           : getEffectiveAccess(user, feature);
                    return new FeaturePermissionView(feature, effective, custom != null);
                })
                .toList();

        return new UserPermissionViewResponse(user.getId(), user.getEmail(), user.getRole(), featureViews);
    }
}
