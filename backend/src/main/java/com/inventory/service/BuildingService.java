package com.inventory.service;

import com.inventory.mapper.BuildingMapper;
import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.BuildingRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public BuildingService(
            BuildingRepository buildingRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.buildingRepository = buildingRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    public List<BuildingResponse> getAll(boolean archived) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        List<Building> buildings = archived
                ? buildingRepository.findByTenantIdAndDeletedAtIsNotNull(tenantId)
                : buildingRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        return buildings.stream()
                .map(BuildingMapper::toResponse)
                .toList();
    }

    public BuildingResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Building building = buildingRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Bâtiment introuvable"));

        return BuildingMapper.toResponse(building);
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant introuvable"));

        Building building = BuildingMapper.toEntity(request, tenant);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        building.setUpdatedBy(currentUser);
        building = buildingRepository.save(building);

        return BuildingMapper.toResponse(building);
    }

    @Transactional
    public BuildingResponse update(UUID id, BuildingRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Building building = buildingRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Bâtiment introuvable"));

        BuildingMapper.updateEntity(building, request);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        building.setUpdatedBy(currentUser);
        building = buildingRepository.save(building);

        return BuildingMapper.toResponse(building);
    }

    @Transactional
    public void delete(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Building building = buildingRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Bâtiment introuvable"));

        building.setDeletedAt(Instant.now());
        buildingRepository.save(building);
    }
}