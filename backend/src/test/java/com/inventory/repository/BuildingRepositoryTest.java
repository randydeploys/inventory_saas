package com.inventory.repository;

import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class BuildingRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    BuildingRepository buildingRepository;

    private Tenant tenant;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder().name("Acme").slug("acme").build());

        buildingRepository.save(Building.builder()
                .tenant(tenant)
                .name("Entrepôt Actif")
                .build());

        buildingRepository.save(Building.builder()
                .tenant(tenant)
                .name("Entrepôt Archivé")
                .deletedAt(Instant.now()) // Soft delete
                .build());
    }

    @Test
    void findByTenantAndDeletedAtIsNull_returnsOnlyActiveBuildings() {
        List<Building> results = buildingRepository.findByTenantAndDeletedAtIsNull(tenant);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Entrepôt Actif");
    }

    @Test
    void findByTenantAndDeletedAtIsNotNull_returnsOnlyArchivedBuildings() {
        List<Building> results = buildingRepository.findByTenantAndDeletedAtIsNotNull(tenant);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Entrepôt Archivé");
    }
}