package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.RoomMapper;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final BuildingRepository buildingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public RoomService(
            RoomRepository roomRepository,
            BuildingRepository buildingRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.roomRepository = roomRepository;
        this.buildingRepository = buildingRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    public List<RoomResponse> getAllByBuilding(UUID buildingId, boolean archived) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        List<Room> rooms = archived
                ? roomRepository.findByBuildingIdAndTenantIdAndDeletedAtIsNotNull(buildingId, tenantId)
                : roomRepository.findByBuildingIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId);

        return rooms.stream()
                .map(RoomMapper::toResponse)
                .toList();
    }

    public RoomResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Room room = roomRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));

        return RoomMapper.toResponse(room);
    }

    @Transactional
    public RoomResponse create(UUID buildingId, RoomRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        // Vérifier que le building existe, est actif, et appartient au tenant
        Building building = buildingRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(buildingId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable"));

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        Room room = RoomMapper.toEntity(request, building, tenant);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        room.setUpdatedBy(currentUser);
        room = roomRepository.save(room);

        return RoomMapper.toResponse(room);
    }

    @Transactional
    public RoomResponse update(UUID id, RoomRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Room room = roomRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
.orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));

        RoomMapper.updateEntity(room, request);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        room.setUpdatedBy(currentUser);
        room = roomRepository.save(room);

        return RoomMapper.toResponse(room);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Room room = roomRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Zone introuvable"));

        // TODO: vérifier si des produits actifs existent (quand ProductStock sera créé)

        room.setDeletedAt(Instant.now());
        roomRepository.save(room);
    }
}