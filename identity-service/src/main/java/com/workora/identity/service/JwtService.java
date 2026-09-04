package com.workora.identity.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration-ms:900000}")
    private long accessTokenExpirationMs;

    @Value("${jwt.refresh-token-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    public String generateAccessToken(com.workora.identity.entity.User user) {
        return buildToken(user, accessTokenExpirationMs, "access");
    }

    public String generateRefreshToken(com.workora.identity.entity.User user) {
        return buildToken(user, refreshTokenExpirationMs, "refresh");
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValidToken(String token, String expectedType) {
        try {
            Claims claims = parse(token);
            return expectedType.equals(claims.get("type")) && !claims.getExpiration().before(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private String buildToken(com.workora.identity.entity.User user, long expirationMs, String type) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(user.getUsername())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(getSigningKey())
                .compact();
    }

    private Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
