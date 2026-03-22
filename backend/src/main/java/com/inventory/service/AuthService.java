package com.inventory.service;

import com.inventory.config.JwtConfig;
import com.inventory.model.dto.RegisterRequest;
import com.inventory.model.dto.LoginRequest;
import com.inventory.model.dto.UserResponse;
import com.inventory.model.entity.RefreshToken;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.Role;
import com.inventory.repository.RefreshTokenRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final JwtConfig jwtConfig;

    public AuthService(
            UserRepository userRepository,
            TenantRepository tenantRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            PasswordEncoder passwordEncoder,
            JwtConfig jwtConfig
    ) {
        this.userRepository = userRepository;
        this.tenantRepository = tenantRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        this.jwtConfig = jwtConfig;
    }

    public record AuthTokens(String accessToken, String rawRefreshToken, UserResponse user) {}

    // ─── Register ────────────────────────────────────────────────

    @Transactional
    public AuthTokens register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("Email déjà utilisé");
        }

        Tenant tenant = new Tenant();
        tenant.setName(request.tenantName());
        tenant.setSlug(generateSlug(request.tenantName()));
        tenant = tenantRepository.save(tenant);

        User user = new User();
        user.setTenant(tenant);
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setRole(Role.ADMIN);
        user.setActive(true);
        user = userRepository.save(user);

        return generateTokens(user);
    }

    // ─── Login ───────────────────────────────────────────────────

    @Transactional
    public AuthTokens login(LoginRequest request) {
        // 1. Chercher le user actif par email
        User user = userRepository.findByEmailAndActiveTrue(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Email ou mot de passe incorrect"));

        // 2. Vérifier le mot de passe
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Email ou mot de passe incorrect");
        }

        // 3. Révoquer les anciens refresh tokens (sécurité)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        // 4. Générer les nouveaux tokens
        return generateTokens(user);
    }

    // ─── Refresh ─────────────────────────────────────────────────

    @Transactional
    public String refresh(String rawRefreshToken) {
        // 1. Hasher le token reçu pour le comparer avec la base
        String hashedToken = hashToken(rawRefreshToken);

        // 2. Chercher en base (non révoqué)
        RefreshToken storedToken = refreshTokenRepository
                .findByTokenAndRevokedFalse(hashedToken)
                .orElseThrow(() -> new IllegalArgumentException("Refresh token invalide"));

        // 3. Vérifier l'expiration
        if (storedToken.getExpiresAt().isBefore(Instant.now())) {
            storedToken.setRevoked(true);
            refreshTokenRepository.save(storedToken);
            throw new IllegalArgumentException("Refresh token expiré");
        }

        // 4. Générer un nouvel access token seulement
        User user = storedToken.getUser();
        return jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getRole().name()
        );
    }

    // ─── Logout ──────────────────────────────────────────────────

    @Transactional
    public void logout(String rawRefreshToken) {
        String hashedToken = hashToken(rawRefreshToken);
        refreshTokenRepository.findByTokenAndRevokedFalse(hashedToken)
                .ifPresent(token -> {
                    token.setRevoked(true);
                    refreshTokenRepository.save(token);
                });
    }

    // ─── Méthodes privées ────────────────────────────────────────

    private AuthTokens generateTokens(User user) {
        String accessToken = jwtTokenProvider.generateAccessToken(
                user.getId(),
                user.getTenant().getId(),
                user.getEmail(),
                user.getRole().name()
        );

        String rawRefreshToken = generateSecureRandomToken();

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUser(user);
        refreshToken.setToken(hashToken(rawRefreshToken));
        refreshToken.setExpiresAt(
                Instant.now().plusSeconds(jwtConfig.refreshTokenExpiration())
        );
        refreshTokenRepository.save(refreshToken);

        UserResponse userResponse = new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole().name(),
                user.isActive(),
                user.getTenant().getId()
        );

        return new AuthTokens(accessToken, rawRefreshToken, userResponse);
    }

    private String generateSecureRandomToken() {
        byte[] bytes = new byte[64];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 non disponible", e);
        }
    }

    private String generateSlug(String name) {
        return name.toLowerCase()
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
    }
}