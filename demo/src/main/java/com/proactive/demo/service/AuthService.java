package com.proactive.demo.service;

import com.proactive.demo.dto.AuthRequest;
import com.proactive.demo.dto.AuthResponse;
import com.proactive.demo.dto.RegisterRequest;
import com.proactive.demo.model.User;
import com.proactive.demo.repository.UserRepository;
import com.proactive.demo.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;

    public AuthResponse login(AuthRequest request) {
        // Vérifier le statut avant l'authentification Spring Security
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (user.getStatus() == null || user.getStatus() == User.Status.PENDING) {
            throw new RuntimeException("Votre compte est en attente d'approbation par un administrateur.");
        }
        if (user.getStatus() == User.Status.REJECTED) {
            throw new RuntimeException("Votre demande d'inscription a été refusée.");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
        } catch (DisabledException e) {
            throw new RuntimeException("Votre compte est désactivé.");
        } catch (BadCredentialsException e) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String token        = jwtUtil.generateToken(userDetails);
        String refreshToken = jwtUtil.generateRefreshToken(userDetails);

        return new AuthResponse(
                token, refreshToken,
                user.getEmail(), user.getRole().name(),
                user.getFirstName(), user.getLastName(), user.getId()
        );
    }

    public String register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(request.getRole() != null ? request.getRole() : User.Role.USER);
        user.setStatus(User.Status.PENDING);

        userRepository.save(user);
        return "Votre demande d'inscription a été soumise. En attente d'approbation.";
    }

    public AuthResponse refreshToken(String refreshToken) {
        String username   = jwtUtil.extractUsername(refreshToken);
        UserDetails ud    = userDetailsService.loadUserByUsername(username);
        User user         = userRepository.findByEmail(username).orElseThrow();

        if (user.getStatus() != User.Status.APPROVED) {
            throw new RuntimeException("Compte non approuvé");
        }
        if (jwtUtil.validateToken(refreshToken, ud)) {
            String newToken = jwtUtil.generateToken(ud);
            return new AuthResponse(newToken, refreshToken,
                    user.getEmail(), user.getRole().name(),
                    user.getFirstName(), user.getLastName(), user.getId());
        }
        throw new RuntimeException("Token invalide ou expiré");
    }
}
