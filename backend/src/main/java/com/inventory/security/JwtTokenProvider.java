package com.inventory.security;

import com.inventory.config.JwtConfig;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
@Slf4j
public class JwtTokenProvider {

    private final SecretKey key;
    private final long accessTokenExpiration;

    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.key = Keys.hmacShaKeyFor(
            jwtConfig.secret().getBytes(StandardCharsets.UTF_8)
        );
        // Stocke en secondes, on convertira en millis à l'utilisation
        this.accessTokenExpiration = jwtConfig.accessTokenExpiration();
    }

    // Génère un access token avec toutes les infos nécessaires
    public String generateAccessToken(UUID userId, UUID tenantId, String email, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration * 1000);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("tenantId", tenantId.toString())
                .claim("email", email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    // Valide et extrait les claims — retourne null si invalide
    public Claims extractClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException e) {
            log.debug("Token expiré : {}", e.getMessage());
            return null;
        } catch (JwtException e) {
            log.warn("Token invalide : {}", e.getMessage());
            return null;
        }
    }

    // Helpers pour lire les claims facilement
    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID getTenantId(Claims claims) {
        return UUID.fromString(claims.get("tenantId", String.class));
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public String getEmail(Claims claims) {
        return claims.get("email", String.class);
    }
}