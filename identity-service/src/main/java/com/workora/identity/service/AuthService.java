package com.workora.identity.service;

import com.workora.identity.dto.*;
import com.workora.identity.entity.Role;
import com.workora.identity.entity.User;
import com.workora.identity.exception.AppException;
import com.workora.identity.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService, EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.emailService = emailService;
    }

    @Transactional
    public Map<String, String> register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new AppException("Email already registered");
        }

        User user = new User(email, passwordEncoder.encode(request.password()), request.firstName(), request.lastName(), Role.USER);
        String verificationToken = generateOtp();
        user.setEmailVerificationToken(passwordEncoder.encode(verificationToken));
        user.setEmailVerificationTokenExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        user.setEmailVerified(false);
        userRepository.save(user);
        emailService.sendVerificationOtp(user.getEmail(), verificationToken);

        return Map.of(
                "userId", user.getId().toString(),
                "email", user.getEmail(),
                "message", "Registration successful. A verification code was sent to your email."
        );
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AppException("Invalid credentials"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AppException("Invalid credentials");
        }

        if (!user.isEmailVerified()) {
            throw new AppException("Email not verified");
        }

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);
        user.setRefreshTokenHash(passwordEncoder.encode(refreshToken));
        userRepository.save(user);

        return new AuthResponse(accessToken, refreshToken, user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        String refreshToken = request.refreshToken();
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Invalid refresh token"));

        if (!jwtService.isValidToken(refreshToken, "refresh") || !passwordEncoder.matches(refreshToken, user.getRefreshTokenHash())) {
            throw new AppException("Refresh token is invalid or expired");
        }

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);
        user.setRefreshTokenHash(passwordEncoder.encode(newRefreshToken));
        userRepository.save(user);

        return new AuthResponse(newAccessToken, newRefreshToken, user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name());
    }

    @Transactional
    public String verifyEmail(VerifyEmailRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AppException("Verification token is invalid"));
        if (user.getEmailVerificationToken() == null
                || user.getEmailVerificationTokenExpiresAt() == null
                || user.getEmailVerificationTokenExpiresAt().isBefore(Instant.now())
                || !passwordEncoder.matches(request.token(), user.getEmailVerificationToken())) {
            throw new AppException("Verification token is invalid");
        }
        user.setEmailVerified(true);
        user.setEmailVerificationToken(null);
        user.setEmailVerificationTokenExpiresAt(null);
        userRepository.save(user);
        return "Email verified successfully";
    }

    @Transactional
    public String logout(LogoutRequest request) {
        String refreshToken = request.refreshToken();
        String email = jwtService.extractUsername(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("Invalid refresh token"));

        if (!jwtService.isValidToken(refreshToken, "refresh")) {
            throw new AppException("Refresh token is invalid or expired");
        }

        user.setRefreshTokenHash(null);
        userRepository.save(user);
        return "Logged out successfully";
    }

    @Transactional
    public String requestPasswordReset(PasswordResetRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AppException("User not found"));
        String resetToken = generateOtp();
        user.setPasswordResetToken(passwordEncoder.encode(resetToken));
        user.setPasswordResetTokenExpiresAt(Instant.now().plus(Duration.ofMinutes(10)));
        userRepository.save(user);
        emailService.sendPasswordResetOtp(user.getEmail(), resetToken);
        return "A password reset code was sent to your email.";
    }

    @Transactional
    public String resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(normalizeEmail(request.email()))
                .orElseThrow(() -> new AppException("Password reset token is invalid"));
        if (user.getPasswordResetToken() == null
                || user.getPasswordResetTokenExpiresAt() == null
                || user.getPasswordResetTokenExpiresAt().isBefore(Instant.now())
                || !passwordEncoder.matches(request.token(), user.getPasswordResetToken())) {
            throw new AppException("Password reset token is invalid");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setPasswordResetToken(null);
        user.setPasswordResetTokenExpiresAt(null);
        userRepository.save(user);
        return "Password reset successful";
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getCurrentProfile(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found"));
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName(), user.getRole().name(), user.isEmailVerified());
    }

    @Transactional
    public UserProfileResponse updateCurrentProfile(String email, String firstName, String lastName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException("User not found"));
        if (firstName != null) {
            user.setFirstName(firstName);
        }
        if (lastName != null) {
            user.setLastName(lastName);
        }
        userRepository.save(user);
        return getCurrentProfile(email);
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }
}
