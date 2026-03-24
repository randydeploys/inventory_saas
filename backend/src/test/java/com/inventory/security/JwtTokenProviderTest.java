package com.inventory.security;

import com.inventory.config.JwtConfig;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    // Secret d'au moins 32 caractères (256 bits) pour HMAC-SHA256
    private static final String SECRET =
            "uneCleSecreteTresLonguePourLeTestQuiDoitFaireAuMoins256Bits!!xx";

    @BeforeEach
    void setUp() {
        // JwtTokenProvider prend un JwtConfig dans son constructeur
        JwtConfig config = new JwtConfig(SECRET, 900L, 604800L);
        jwtTokenProvider = new JwtTokenProvider(config);
    }

    // ─── generateAccessToken ──────────────────────────────────────

    @Test
    void generateAccessToken_ShouldProduceValidJwt() {
        UUID userId   = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        String token = jwtTokenProvider.generateAccessToken(userId, tenantId, "admin@tenant1.com", "ADMIN");

        assertNotNull(token);
        assertFalse(token.isBlank());
        // Un JWT valide est composé de 3 parties séparées par des points
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void generateAccessToken_ShouldEmbedCorrectClaims() {
        UUID userId   = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        String email  = "admin@tenant1.com";
        String role   = "ADMIN";

        String token = jwtTokenProvider.generateAccessToken(userId, tenantId, email, role);
        Claims claims = jwtTokenProvider.extractClaims(token);

        assertNotNull(claims, "Les claims doivent être extraits sans erreur");
        assertEquals(userId.toString(),   claims.getSubject(),                       "subject = userId");
        assertEquals(tenantId.toString(), claims.get("tenantId", String.class),       "claim tenantId");
        assertEquals(email,               jwtTokenProvider.getEmail(claims),          "claim email");
        assertEquals(role,                jwtTokenProvider.getRole(claims),           "claim role");
        assertEquals(userId,              jwtTokenProvider.getUserId(claims),         "getUserId()");
        assertEquals(tenantId,            jwtTokenProvider.getTenantId(claims),       "getTenantId()");
    }

    // ─── extractClaims ────────────────────────────────────────────

    @Test
    void extractClaims_ShouldReturnNull_ForInvalidToken() {
        Claims claims = jwtTokenProvider.extractClaims("invalid.token.string");
        assertNull(claims, "Un token corrompu doit retourner null");
    }

    @Test
    void extractClaims_ShouldReturnNull_ForTokenSignedWithDifferentKey() {
        // Token signé avec une autre clé secrète
        JwtConfig otherConfig = new JwtConfig(
                "uneAutreCleSecreteCompleteementDifferenteAuMoins256Bitsxx",
                900L, 604800L
        );
        JwtTokenProvider otherProvider = new JwtTokenProvider(otherConfig);
        String foreignToken = otherProvider.generateAccessToken(
                UUID.randomUUID(), UUID.randomUUID(), "x@x.com", "READER"
        );

        assertNull(jwtTokenProvider.extractClaims(foreignToken),
                "Un token signé avec une autre clé doit être rejeté");
    }
}