package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.BuildingMapper;
import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.BuildingRepository;
import com.inventory.repository.ProductStockRepository;
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
    private final ProductStockRepository productStockRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public BuildingService(
            BuildingRepository buildingRepository,
            ProductStockRepository productStockRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.buildingRepository = buildingRepository;
        this.productStockRepository = productStockRepository;
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
                .map(b -> BuildingMapper.toResponse(b, productStockRepository.countByBuildingId(b.getId())))
                .toList();
    }

    public BuildingResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Building building = buildingRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable"));

        return BuildingMapper.toResponse(building, productStockRepository.countByBuildingId(id));
    }

    @Transactional
    public BuildingResponse create(BuildingRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

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
.orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable"));

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
.orElseThrow(() -> new ResourceNotFoundException("Bâtiment introuvable"));

        long activeStockCount = productStockRepository.countByBuildingId(id);
        if (activeStockCount > 0) {
            throw new ConflictException(
                    "Ce bâtiment contient " + activeStockCount + " produit(s) actif(s). " +
                    "Utilisez la réaffectation pour déplacer les produits avant d'archiver."
            );
        }

        building.setDeletedAt(Instant.now());
        buildingRepository.save(building);
    }
}