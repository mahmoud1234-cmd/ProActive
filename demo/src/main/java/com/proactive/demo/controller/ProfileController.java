package com.proactive.demo.controller;

import com.proactive.demo.dto.UpdateProfileRequest;
import com.proactive.demo.dto.UserSummaryResponse;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.security.JwtUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    /** Récupérer son propre profil */
    @GetMapping
    public ResponseEntity<?> getProfile() {
        return currentUser()
                .map(u -> (ResponseEntity<?>) ResponseEntity.ok(UserSummaryResponse.from(u)))
                .orElse(ResponseEntity.status(401).build());
    }

    /** Mettre à jour prénom, nom, email et/ou mot de passe */
    @PatchMapping
    public ResponseEntity<?> updateProfile(@Valid @RequestBody UpdateProfileRequest req) {
        User user = currentUser()
                .orElse(null);
        if (user == null) return ResponseEntity.status(401).build();

        // ── Changer le mot de passe ──────────────────────────────────────
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank())
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Le mot de passe actuel est requis pour en définir un nouveau."));

            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword()))
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Mot de passe actuel incorrect."));

            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }

        // ── Changer l'email ──────────────────────────────────────────────
        if (req.getEmail() != null && !req.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail()))
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "Cet email est déjà utilisé."));
            user.setEmail(req.getEmail());
        }

        // ── Changer prénom / nom ──────────────────────────────────────────
        if (req.getFirstName() != null && !req.getFirstName().isBlank())
            user.setFirstName(req.getFirstName());
        if (req.getLastName() != null && !req.getLastName().isBlank())
            user.setLastName(req.getLastName());

        userRepository.save(user);

        // Générer un nouveau token avec les infos mises à jour
        var userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String newToken = jwtUtil.generateToken(userDetails);

        return ResponseEntity.ok(Map.of(
            "user",  UserSummaryResponse.from(user),
            "token", newToken
        ));
    }

    private java.util.Optional<User> currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return java.util.Optional.empty();
        return userRepository.findByEmail(auth.getName());
    }
}
