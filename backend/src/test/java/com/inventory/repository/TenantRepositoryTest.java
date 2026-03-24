package com.inventory.repository;

import com.inventory.model.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class TenantRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TenantRepository tenantRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder()
                .name("Acme Corp")
                .slug("acme-corp")
                .build());
    }

    @Test
    void findBySlug_returnsPresent_whenSlugExists() {
        Optional<Tenant> result = tenantRepository.findBySlug("acme-corp");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Acme Corp");
    }

    @Test
    void findBySlug_returnsEmpty_whenSlugNotFound() {
        Optional<Tenant> result = tenantRepository.findBySlug("unknown-slug");
        assertThat(result).isEmpty();
    }

    @Test
    void existsBySlug_returnsTrue_whenSlugExists() {
        assertThat(tenantRepository.existsBySlug("acme-corp")).isTrue();
    }

    @Test
    void existsBySlug_returnsFalse_whenSlugNotFound() {
        assertThat(tenantRepository.existsBySlug("unknown")).isFalse();
    }
}
