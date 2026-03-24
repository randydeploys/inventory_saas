package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.BuildingRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private BuildingService buildingService;

    private UUID tenantId;
    private UUID userId;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        // Arrange commun à tous les tests
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Corp");
        tenant.setSlug("test-corp");

        user = new User();
        user.setId(userId);
    }

    // ─── getAll ──────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnActiveBuildings() {
        // Arrange
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Building building = createBuilding("Entrepôt A");
        when(buildingRepository.findByTenantIdAndDeletedAtIsNull(tenantId))
                .thenReturn(List.of(building));

        // Act
        List<BuildingResponse> result = buildingService.getAll(false);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Entrepôt A", result.get(0).name());
    }

    @Test
    void getAll_archived_shouldReturnArchivedBuildings() {
        // Arrange
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Building building = createBuilding("Entrepôt Archivé");
        building.setDeletedAt(Instant.now());
        when(buildingRepository.findByTenantIdAndDeletedAtIsNotNull(tenantId))
                .thenReturn(List.of(building));

        // Act
        List<BuildingResponse> result = buildingService.getAll(true);

        // Assert
        assertEquals(1, result.size());
        assertEquals("Entrepôt Archivé", result.get(0).name());
    }

    // ─── getById ─────────────────────────────────────────────────

    @Test
    void getById_withValidId_shouldReturnBuilding() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Building building = createBuilding("Entrepôt A");
        building.setId(buildingId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.of(building));

        // Act
        BuildingResponse result = buildingService.getById(buildingId);

        // Assert
        assertEquals("Entrepôt A", result.name());
        assertEquals(buildingId, result.id());
    }

    @Test
    void getById_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> buildingService.getById(buildingId));
    }

    // ─── create ──────────────────────────────────────────────────

    @Test
    void create_shouldReturnCreatedBuilding() {
        // Arrange
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        // when(save) retourne le building qu'on lui passe (simule la DB)
        when(buildingRepository.save(any(Building.class)))
                .thenAnswer(invocation -> {
                    Building b = invocation.getArgument(0);
                    b.setId(UUID.randomUUID());
                    b.setCreatedAt(Instant.now());
                    b.setUpdatedAt(Instant.now());
                    return b;
                });

        // Act
        BuildingRequest request = new BuildingRequest("Nouveau", "10 rue test");
        BuildingResponse result = buildingService.create(request);

        // Assert
        assertEquals("Nouveau", result.name());
        assertEquals("10 rue test", result.address());
        assertNotNull(result.id());

        // Vérifie que save a bien été appelé une fois
        verify(buildingRepository, times(1)).save(any(Building.class));
    }

    // ─── update ──────────────────────────────────────────────────

    @Test
    void update_shouldReturnUpdatedBuilding() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Building existing = createBuilding("Ancien nom");
        existing.setId(buildingId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.of(existing));
        when(buildingRepository.save(any(Building.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BuildingRequest request = new BuildingRequest("Nouveau nom", "Nouvelle adresse");
        BuildingResponse result = buildingService.update(buildingId, request);

        // Assert
        assertEquals("Nouveau nom", result.name());
        assertEquals("Nouvelle adresse", result.address());
    }

    @Test
    void update_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> buildingService.update(buildingId, new BuildingRequest("Nom", "Adresse")));
    }

    // ─── delete ──────────────────────────────────────────────────

    @Test
    void delete_shouldSoftDelete() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Building building = createBuilding("À supprimer");
        building.setId(buildingId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.of(building));
        when(buildingRepository.save(any(Building.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        buildingService.delete(buildingId);

        // Assert — deletedAt doit être rempli
        assertNotNull(building.getDeletedAt());
        verify(buildingRepository, times(1)).save(building);
    }

    @Test
    void delete_withInvalidId_shouldThrowNotFoundException() {
        // Arrange
        UUID buildingId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class,
                () -> buildingService.delete(buildingId));
    }

    // ─── Helper ──────────────────────────────────────────────────

    private Building createBuilding(String name) {
        Building building = new Building();
        building.setId(UUID.randomUUID());
        building.setTenant(tenant);
        building.setName(name);
        building.setAddress("Adresse test");
        building.setCreatedAt(Instant.now());
        building.setUpdatedAt(Instant.now());
        return building;
    }
}