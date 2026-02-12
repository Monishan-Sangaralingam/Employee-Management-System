package com.employeesystem.emsbackend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;

@Component
public class JwtUtils {

    private final SecretKey signingKey;
    private final long ttlMs;

    public JwtUtils(@Value("${app.jwt.secret}") String secret,
                    @Value("${app.jwt.ttl-ms}") long ttlMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.ttlMs = ttlMs;
    }

    public String generateToken(String username, Set<String> roles) {
        Instant now = Instant.now();
        Date issuedAt = Date.from(now);
        Date expiry = Date.from(now.plusMillis(ttlMs));

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(issuedAt)
                .expiration(expiry)
                .signWith(signingKey)
                .compact();
    }

    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (Exception ex) {
            return null;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
