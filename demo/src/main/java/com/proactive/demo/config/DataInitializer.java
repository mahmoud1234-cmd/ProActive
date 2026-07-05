package com.proactive.demo.config;

import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {

        // ── ÉTAPE 1 : Migration colonne status ──────────────────────────────
        try {
            jdbcTemplate.execute(
                "ALTER TABLE \"user\" ADD COLUMN IF NOT EXISTS status VARCHAR(20)"
            );
            int fixed = jdbcTemplate.update(
                "UPDATE \"user\" SET status = 'APPROVED' WHERE status IS NULL"
            );
            if (fixed > 0) log.info("Migration : {} compte(s) → APPROVED", fixed);
        } catch (Exception e) {
            log.debug("Migration status : {}", e.getMessage());
        }

        // ── ÉTAPE 2 : Migration contrainte CHECK user_permissions.feature ───
        // Supprimer l'ancienne contrainte qui ne contient pas APPROVALS et la recréer
        try {
            jdbcTemplate.execute(
                "ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS user_permissions_feature_check"
            );
            jdbcTemplate.execute(
                "ALTER TABLE user_permissions ADD CONSTRAINT user_permissions_feature_check " +
                "CHECK (feature IN ('DASHBOARD','PROJECTS','TASKS','REPORTS','USERS','APPROVALS','PERMISSIONS'))"
            );
            log.info("Contrainte user_permissions_feature_check mise à jour avec APPROVALS.");
        } catch (Exception e) {
            log.debug("Migration contrainte feature : {}", e.getMessage());
        }

        // ── ÉTAPE 3 : Mettre à jour la contrainte CHECK user.status pour inclure BANNED ──
        try {
            // Supprimer l'ancienne contrainte si elle existe
            jdbcTemplate.execute("ALTER TABLE \"user\" DROP CONSTRAINT IF EXISTS user_status_check");
            log.debug("Ancienne contrainte user_status_check supprimée");
        } catch (Exception e) {
            log.debug("Pas de contrainte user_status_check à supprimer : {}", e.getMessage());
        }

        // ── ÉTAPE 3 : Reset les comptes de test (mot de passe + status) ─────
        resetOrCreate("admin@proactive.com",   "admin123",   "Admin",  "ProActive", User.Role.ADMIN);
        resetOrCreate("manager@proactive.com", "manager123", "Marie",  "Martin",    User.Role.MANAGER);
        resetOrCreate("user@proactive.com",    "user123",    "Jean",   "Dupont",    User.Role.USER);

        log.info("Initialisation terminée. {} utilisateurs en base.", userRepository.count());
    }

    /**
     * Crée l'utilisateur s'il n'existe pas,
     * ou remet son mot de passe et son statut à APPROVED s'il existe.
     */
    private void resetOrCreate(String email, String rawPassword, String fn, String ln, User.Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
            u -> {
                // Réinitialiser le mot de passe et le statut
                u.setPassword(passwordEncoder.encode(rawPassword));
                u.setStatus(User.Status.APPROVED);
                u.setRole(role);
                userRepository.save(u);
                log.info("Compte {} → mot de passe réinitialisé, statut APPROVED", email);
            },
            () -> {
                User u = new User();
                u.setEmail(email);
                u.setPassword(passwordEncoder.encode(rawPassword));
                u.setFirstName(fn);
                u.setLastName(ln);
                u.setRole(role);
                u.setStatus(User.Status.APPROVED);
                userRepository.save(u);
                log.info("Compte {} créé (APPROVED)", email);
            }
        );
    }
}
