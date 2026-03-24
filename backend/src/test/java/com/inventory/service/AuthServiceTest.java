package com.inventory.service;

import com.inventory.config.JwtConfig;
import com.inventory.model.dto.LoginRequest;
import com.inventory.model.dto.RegisterRequest;
import com.inventory.model.entity.RefreshToken;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.Role;
import com.inventory.repository.RefreshTokenRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtConfig jwtConfig;

    @InjectMocks
    private AuthService authService;

    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setName("Acme Corp");
        tenant.setSlug("acme-corp");

        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("admin@acme.com");
        user.setPasswordHash("$2a$10$hashedpassword");
        user.setFirstName("Alice");
        user.setLastName("Admin");
        user.setRole(Role.ADMIN);
        user.setActive(true);
        user.setTenant(tenant);
    }

    // ─── register ────────────────────────────────────────────────

    @Test
    void register_ShouldCreateTenantAndUserAndReturnTokens() {
        RegisterRequest request = new RegisterRequest(
                "admin@acme.com", "securePass1", "Alice", "Admin", "Acme Corp"
        );
        when(userRepository.existsByEmail("admin@acme.com")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);
        when(passwordEncoder.encode("securePass1")).thenReturn("$2a$10$hashedPass");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtConfig.refreshTokenExpiration()).thenReturn(604800L);

        AuthService.AuthTokens result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        assertThat(result.user().email()).isEqualTo("admin@acme.com");
        verify(tenantRepository).save(any(Tenant.class));
        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void register_ShouldThrow_WhenEmailAlreadyUsed() {
        RegisterRequest request = new RegisterRequest(
                "admin@acme.com", "securePass1", "Alice", "Admin", "Acme Corp"
        );
        when(userRepository.existsByEmail("admin@acme.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email déjà utilisé");

        verify(tenantRepository, never()).save(any());
        verify(userRepository, never()).save(any());
    }

    // ─── login ───────────────────────────────────────────────────

    @Test
    void login_ShouldReturnTokens_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("admin@acme.com", "myPassword");
        when(userRepository.findByEmailAndActiveTrue("admin@acme.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("myPassword", user.getPasswordHash())).thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyString(), anyString()))
                .thenReturn("access-token");
        when(jwtConfig.refreshTokenExpiration()).thenReturn(604800L);

        AuthService.AuthTokens result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.rawRefreshToken()).isNotBlank();
        verify(refreshTokenRepository).revokeAllByUserId(user.getId());
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_ShouldThrow_WhenUserNotFound() {
        when(userRepository.findByEmailAndActiveTrue("unknown@acme.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("unknown@acme.com", "pass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou mot de passe incorrect");
    }

    @Test
    void login_ShouldThrow_WhenPasswordIsWrong() {
        when(userRepository.findByEmailAndActiveTrue("admin@acme.com"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPass", user.getPasswordHash())).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("admin@acme.com", "wrongPass")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email ou mot de passe incorrect");
    }

    // ─── refresh ─────────────────────────────────────────────────

    @Test
    void refresh_ShouldReturnNewAccessToken_WhenTokenIsValid() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setUser(user);
        storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
        storedToken.setRevoked(false);

        // Le service hache le rawToken → on ne peut pas prédire le hash, on matche avec anyString()
        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(storedToken));
        when(jwtTokenProvider.generateAccessToken(any(), any(), anyString(), anyString()))
                .thenReturn("new-access-token");

        String result = authService.refresh("any-raw-refresh-token");

        assertThat(result).isEqualTo("new-access-token");
    }

    @Test
    void refresh_ShouldThrow_WhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("bad-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token invalide");
    }

    @Test
    void refresh_ShouldThrow_AndRevokeToken_WhenTokenIsExpired() {
        RefreshToken expiredToken = new RefreshToken();
        expiredToken.setUser(user);
        expiredToken.setExpiresAt(Instant.now().minusSeconds(1)); // expiré
        expiredToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(expiredToken));

        assertThatThrownBy(() -> authService.refresh("expired-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Refresh token expiré");

        assertThat(expiredToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(expiredToken);
    }

    // ─── logout ──────────────────────────────────────────────────

    @Test
    void logout_ShouldRevokeToken_WhenFound() {
        RefreshToken storedToken = new RefreshToken();
        storedToken.setRevoked(false);

        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.of(storedToken));

        authService.logout("raw-refresh-token");

        assertThat(storedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(storedToken);
    }

    @Test
    void logout_ShouldDoNothing_WhenTokenNotFound() {
        when(refreshTokenRepository.findByTokenAndRevokedFalse(anyString()))
                .thenReturn(Optional.empty());

        // Pas d'exception attendue
        assertThatCode(() -> authService.logout("unknown-token")).doesNotThrowAnyException();
        verify(refreshTokenRepository, never()).save(any());
    }
}
