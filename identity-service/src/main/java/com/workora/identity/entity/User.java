package com.workora.identity.entity;

import jakarta.persistence.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.time.Instant;

@Entity
@Table(name = "users")
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role = Role.USER;

    @Column(nullable = false)
    private boolean emailVerified = false;

    @Column(length = 512)
    private String emailVerificationToken;

    private Instant emailVerificationTokenExpiresAt;

    private Integer emailVerificationAttempts = 0;

    @Column(length = 512)
    private String passwordResetToken;

    private Instant passwordResetTokenExpiresAt;

    private Integer passwordResetAttempts = 0;

    private Instant passwordResetRequestedAt;

    @Column(length = 512)
    private String refreshTokenHash;

    protected User() {
    }

    public User(String email, String password, String firstName, String lastName, Role role) {
        this.email = email;
        this.password = password;
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
    }

    public Instant getEmailVerificationTokenExpiresAt() {
        return emailVerificationTokenExpiresAt;
    }

    public void setEmailVerificationTokenExpiresAt(Instant emailVerificationTokenExpiresAt) {
        this.emailVerificationTokenExpiresAt = emailVerificationTokenExpiresAt;
    }

    public int getEmailVerificationAttempts() {
        return emailVerificationAttempts == null ? 0 : emailVerificationAttempts;
    }

    public void setEmailVerificationAttempts(int emailVerificationAttempts) {
        this.emailVerificationAttempts = emailVerificationAttempts;
    }

    public String getPasswordResetToken() {
        return passwordResetToken;
    }

    public void setPasswordResetToken(String passwordResetToken) {
        this.passwordResetToken = passwordResetToken;
    }

    public Instant getPasswordResetTokenExpiresAt() {
        return passwordResetTokenExpiresAt;
    }

    public void setPasswordResetTokenExpiresAt(Instant passwordResetTokenExpiresAt) {
        this.passwordResetTokenExpiresAt = passwordResetTokenExpiresAt;
    }

    public int getPasswordResetAttempts() {
        return passwordResetAttempts == null ? 0 : passwordResetAttempts;
    }

    public void setPasswordResetAttempts(int passwordResetAttempts) {
        this.passwordResetAttempts = passwordResetAttempts;
    }

    public Instant getPasswordResetRequestedAt() {
        return passwordResetRequestedAt;
    }

    public void setPasswordResetRequestedAt(Instant passwordResetRequestedAt) {
        this.passwordResetRequestedAt = passwordResetRequestedAt;
    }

    public String getRefreshTokenHash() {
        return refreshTokenHash;
    }

    public void setRefreshTokenHash(String refreshTokenHash) {
        this.refreshTokenHash = refreshTokenHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return emailVerified;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
