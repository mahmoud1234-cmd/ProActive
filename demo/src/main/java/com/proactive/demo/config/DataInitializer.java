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

        // ── 1 : Colonne status sur user ───────────────────────────────────
        migrateColumn("\"user\"", "status", "VARCHAR(20)");
        try {
            jdbcTemplate.update("UPDATE \"user\" SET status = 'APPROVED' WHERE status IS NULL");
        } catch (Exception e) {
            log.debug("status update: {}", e.getMessage());
        }
        try {
            jdbcTemplate.execute("ALTER TABLE \"user\" DROP CONSTRAINT IF EXISTS user_status_check");
        } catch (Exception e) {
            log.debug("drop user_status_check: {}", e.getMessage());
        }

        // ── 2 : Contrainte feature permissions ───────────────────────────
        try {
            jdbcTemplate.execute(
                "ALTER TABLE user_permissions DROP CONSTRAINT IF EXISTS user_permissions_feature_check"
            );
            jdbcTemplate.execute(
                "ALTER TABLE user_permissions ADD CONSTRAINT user_permissions_feature_check " +
                "CHECK (feature IN ('DASHBOARD','PROJECTS','TASKS','REPORTS','USERS','APPROVALS','PERMISSIONS'))"
            );
        } catch (Exception e) {
            log.debug("feature constraint: {}", e.getMessage());
        }

        // ── 3 : Nouvelles colonnes table projects ────────────────────────
        migrateColumn("projects", "type",        "VARCHAR(30) DEFAULT 'OTHER'");
        migrateColumn("projects", "methodology", "VARCHAR(20) DEFAULT 'AGILE'");
        migrateColumn("projects", "tools",       "TEXT");

        // Mettre à jour les projets existants avec des valeurs par défaut
        try {
            jdbcTemplate.update("UPDATE projects SET type = 'OTHER' WHERE type IS NULL");
            jdbcTemplate.update("UPDATE projects SET methodology = 'AGILE' WHERE methodology IS NULL");
        } catch (Exception e) {
            log.debug("projects defaults: {}", e.getMessage());
        }

        // ── 4 : Comptes de test ──────────────────────────────────────────
        resetOrCreate("admin@proactive.com",   "admin123",   "Admin", "ProActive", User.Role.ADMIN);
        resetOrCreate("manager@proactive.com", "manager123", "Marie", "Martin",    User.Role.MANAGER);
        resetOrCreate("user@proactive.com",    "user123",    "Jean",  "Dupont",    User.Role.USER);

        log.info("Initialisation OK. {} utilisateurs.", userRepository.count());
    }

    /** Ajoute une colonne si elle n'existe pas encore */
    private void migrateColumn(String table, String column, String definition) {
        try {
            String checkSql = "SELECT COUNT(*) FROM information_schema.columns " +
                              "WHERE table_name = ? AND column_name = ?";
            // Pour les tables entre guillemets, on enlève les guillemets pour la comparaison
            String tableName = table.replace("\"", "");
            Integer count = jdbcTemplate.queryForObject(checkSql, Integer.class, tableName, column);
            if (count == null || count == 0) {
                jdbcTemplate.execute("ALTER TABLE " + table + " ADD COLUMN " + column + " " + definition);
                log.info("Colonne {}.{} ajoutée.", table, column);
            }
        } catch (Exception e) {
            log.debug("migrateColumn {}.{}: {}", table, column, e.getMessage());
        }
    }

    private void resetOrCreate(String email, String rawPwd, String fn, String ln, User.Role role) {
        userRepository.findByEmail(email).ifPresentOrElse(
            u -> {
                u.setPassword(passwordEncoder.encode(rawPwd));
                u.setStatus(User.Status.APPROVED);
                u.setRole(role);
                userRepository.save(u);
            },
            () -> {
                User u = new User();
                u.setEmail(email);
                u.setPassword(passwordEncoder.encode(rawPwd));
                u.setFirstName(fn);
                u.setLastName(ln);
                u.setRole(role);
                u.setStatus(User.Status.APPROVED);
                userRepository.save(u);
                log.info("Compte {} créé.", email);
            }
        );
    }
}
