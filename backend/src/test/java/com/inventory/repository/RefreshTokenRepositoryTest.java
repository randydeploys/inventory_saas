package com.inventory.repository;

import com.inventory.model.entity.RefreshToken;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class RefreshTokenRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    UserRepository userRepository;

    @Autowired
    RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void setUp() {
        Tenant tenant = tenantRepository.save(Tenant.builder().name("Acme").slug("acme").build());
        User user = userRepository.save(User.builder()
                .tenant(tenant)
                .email("admin@acme.com")
                .passwordHash("hashed")
                .firstName("Alice")
                .lastName("Admin")
                .role(Role.ADMIN)
                .active(true)
                .build());

        refreshTokenRepository.save(RefreshToken.builder()
                .user(user)
                .token("my-secret-token")
                .expiresAt(Instant.now().plus(Duration.ofDays(7)))
                .build());
    }

    @Test
    void findByToken_returnsToken_whenExists() {
        Optional<RefreshToken> result = refreshTokenRepository.findByTokenAndRevokedFalse("my-secret-token");
        assertThat(result).isPresent();
        assertThat(result.get().isRevoked()).isFalse();
    }

    @Test
    void revokeToken_persistsStateSuccessfully() {
        RefreshToken token = refreshTokenRepository.findByTokenAndRevokedFalse("my-secret-token").orElseThrow();
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        Optional<RefreshToken> updated = refreshTokenRepository.findByTokenAndRevokedFalse("my-secret-token");
        assertThat(updated).isPresent();
        assertThat(updated.get().isRevoked()).isTrue();
    }
}