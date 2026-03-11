package com.inventory.repository;

import com.inventory.model.entity.Building;
import com.inventory.model.entity.Room;
import com.inventory.model.entity.Tenant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers
class RoomRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    TenantRepository tenantRepository;

    @Autowired
    BuildingRepository buildingRepository;

    @Autowired
    RoomRepository roomRepository;

    private Tenant tenant;
    private Building building;

    @BeforeEach
    void setUp() {
        tenant = tenantRepository.save(Tenant.builder().name("Acme").slug("acme").build());
        building = buildingRepository.save(Building.builder().tenant(tenant).name("Bat 1").build());

        roomRepository.save(Room.builder()
                .tenant(tenant)
                .building(building)
                .name("Zone Active")
                .build());

        roomRepository.save(Room.builder()
                .tenant(tenant)
                .building(building)
                .name("Zone Archivée")
                .deletedAt(LocalDateTime.now())
                .build());
    }

    @Test
    void findByTenantAndBuildingAndDeletedAtIsNull_returnsOnlyActiveRooms() {
        List<Room> results = roomRepository.findByTenantAndBuildingAndDeletedAtIsNull(tenant, building);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("Zone Active");
    }
}