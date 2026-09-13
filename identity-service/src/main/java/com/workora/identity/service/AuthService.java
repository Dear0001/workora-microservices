package com.workora.identity.service;

import com.workora.identity.dto.*;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final KeycloakService keycloakService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            KeycloakService keycloakService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.keycloakService = keycloakService;
    }

    @Transactional
    public Map<String, String> register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AppException("Email already registered");
        }

        keycloakService.register(request);
        User user = new User(
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                request.firstName(),
                request.lastName(),
                Role.USER
        );
        userRepository.save(user);

        return Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "message", "Registration successful. Keycloak sent a verification email."
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        Map<String, Object> tokens = keycloakService.login(new LoginRequest(email, request.password()));
        User user = profileFor(email);
        user.setEmailVerified(true);
        userRepository.save(user);
        return authResponse(tokens, user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        Map<String, Object> tokens = keycloakService.refresh(request);
        String email = emailFromToken(tokens);
        return authResponse(tokens, profileFor(email));
    }

    public String verifyEmail(VerifyEmailRequest request) {
        throw new AppException("Keycloak uses a verification link. Check your email to complete verification.");
    }

    public String logout(LogoutRequest request) {
        keycloakService.logout(request.refreshToken());
        return "Logged out successfully";
    }

    public String requestPasswordReset(PasswordResetRequest request) {
        keycloakService.requestPasswordReset(normalizeEmail(request.email()));
        return "A password reset email will be sent if the account exists.";
    }

    public String resetPassword(ResetPasswordRequest request) {
        throw new AppException("Password reset is completed through the Keycloak email link.");
    }

    private User profileFor(String email) {
        return userRepository.findByEmail(email).orElseGet(() -> userRepository.save(new User(
                email,
                passwordEncoder.encode(UUID.randomUUID().toString()),
                "",
                "",
                Role.USER
        )));
    }

    private AuthResponse authResponse(Map<String, Object> tokens, User user) {
        String accessToken = (String) tokens.get("access_token");
        String refreshToken = (String) tokens.get("refresh_token");
        if (accessToken == null || refreshToken == null) {
            throw new AppException("Keycloak did not return a complete token response");
        }
        return new AuthResponse(
                accessToken,
                refreshToken,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name()
        );
    }

    private String emailFromToken(Map<String, Object> tokens) {
        String accessToken = (String) tokens.get("access_token");
        if (accessToken == null) {
            throw new AppException("Keycloak did not return an access token");
        }
        String[] parts = accessToken.split("\\.");
        if (parts.length != 3) {
            throw new AppException("Keycloak returned an invalid access token");
        }
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        Matcher matcher = Pattern.compile("\"email\"\\s*:\\s*\"([^\"]+)\"").matcher(payload);
        if (!matcher.find()) {
            throw new AppException("Keycloak token does not contain an email claim");
        }
        return normalizeEmail(matcher.group(1));
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(String email) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new AppException("User profile not found"));
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isEmailVerified()
        );
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(String email, String firstName, String lastName) {
        User user = userRepository.findByEmail(normalizeEmail(email))
                .orElseThrow(() -> new AppException("User profile not found"));
        user.setFirstName(firstName);
        user.setLastName(lastName);
        userRepository.save(user);
        return getCurrentProfile(email);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
}
