package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.RoomRequest;
import com.inventory.model.dto.RoomResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Room;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.BuildingRepository;
import com.inventory.repository.RoomRepository;
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
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private RoomService roomService;

    private UUID tenantId;
    private UUID userId;
    private UUID buildingId;
    private Tenant tenant;
    private User user;
    private Building building;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();
        buildingId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Corp");

        user = new User();
        user.setId(userId);

        building = new Building();
        building.setId(buildingId);
        building.setTenant(tenant);
        building.setName("Entrepôt A");
    }

    // ─── getAllByBuilding ────────────────────────────────────────

    @Test
    void getAllByBuilding_shouldReturnActiveRooms() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Room room = createRoom("Zone A");
        when(roomRepository.findByBuildingIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getAllByBuilding(buildingId, false);

        assertEquals(1, result.size());
        assertEquals("Zone A", result.get(0).name());
    }

    @Test
    void getAllByBuilding_archived_shouldReturnArchivedRooms() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Room room = createRoom("Zone Archivée");
        room.setDeletedAt(Instant.now());
        when(roomRepository.findByBuildingIdAndTenantIdAndDeletedAtIsNotNull(buildingId, tenantId))
                .thenReturn(List.of(room));

        List<RoomResponse> result = roomService.getAllByBuilding(buildingId, true);

        assertEquals(1, result.size());
        assertEquals("Zone Archivée", result.get(0).name());
    }

    // ─── getById ─────────────────────────────────────────────────

    @Test
    void getById_withValidId_shouldReturnRoom() {
        UUID roomId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Room room = createRoom("Zone A");
        room.setId(roomId);
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(roomId, tenantId))
                .thenReturn(Optional.of(room));

        RoomResponse result = roomService.getById(roomId);

        assertEquals("Zone A", result.name());
        assertEquals(roomId, result.id());
    }

    @Test
    void getById_withInvalidId_shouldThrowNotFoundException() {
        UUID roomId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(roomId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roomService.getById(roomId));
    }

    // ─── create ──────────────────────────────────────────────────

    @Test
    void create_shouldReturnCreatedRoom() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.of(building));
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> {
                    Room r = invocation.getArgument(0);
                    r.setId(UUID.randomUUID());
                    r.setCreatedAt(Instant.now());
                    r.setUpdatedAt(Instant.now());
                    return r;
                });

        RoomRequest request = new RoomRequest("Nouvelle Zone", "Description test");
        RoomResponse result = roomService.create(buildingId, request);

        assertEquals("Nouvelle Zone", result.name());
        assertEquals("Description test", result.description());
        assertNotNull(result.id());
        verify(roomRepository, times(1)).save(any(Room.class));
    }

    @Test
    void create_withInvalidBuilding_shouldThrowNotFoundException() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(buildingRepository.findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId))
                .thenReturn(Optional.empty());

        RoomRequest request = new RoomRequest("Zone", "Description");

        assertThrows(ResourceNotFoundException.class,
                () -> roomService.create(buildingId, request));
    }

    // ─── update ──────────────────────────────────────────────────

    @Test
    void update_shouldReturnUpdatedRoom() {
        UUID roomId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Room existing = createRoom("Ancien nom");
        existing.setId(roomId);
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(roomId, tenantId))
                .thenReturn(Optional.of(existing));
        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        RoomRequest request = new RoomRequest("Nouveau nom", "Nouvelle description");
        RoomResponse result = roomService.update(roomId, request);

        assertEquals("Nouveau nom", result.name());
        assertEquals("Nouvelle description", result.description());
    }

    // ─── delete ──────────────────────────────────────────────────

    @Test
    void delete_shouldSoftDelete() {
        UUID roomId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Room room = createRoom("À supprimer");
        room.setId(roomId);
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(roomId, tenantId))
                .thenReturn(Optional.of(room));
        when(roomRepository.save(any(Room.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        roomService.delete(roomId);

        assertNotNull(room.getDeletedAt());
        verify(roomRepository, times(1)).save(room);
    }

    @Test
    void delete_withInvalidId_shouldThrowNotFoundException() {
        UUID roomId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(roomId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> roomService.delete(roomId));
    }

    // ─── Helper ──────────────────────────────────────────────────

    private Room createRoom(String name) {
        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setTenant(tenant);
        room.setBuilding(building);
        room.setName(name);
        room.setDescription("Description test");
        room.setCreatedAt(Instant.now());
        room.setUpdatedAt(Instant.now());
        return room;
    }
}