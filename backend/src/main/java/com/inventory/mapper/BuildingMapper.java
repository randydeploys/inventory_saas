package com.inventory.mapper;

import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;

public final class BuildingMapper {

    private BuildingMapper() {}

    public static BuildingResponse toResponse(Building building, long activeProductCount) {
        return new BuildingResponse(
                building.getId(),
                building.getName(),
                building.getAddress(),
                (int) activeProductCount,
                building.getCreatedAt(),
                building.getUpdatedAt()
        );
    }

    public static BuildingResponse toResponse(Building building) {
        return toResponse(building, 0);
    }

    public static Building toEntity(BuildingRequest request, Tenant tenant) {
        Building building = new Building();
        building.setTenant(tenant);
        building.setName(request.name());
        building.setAddress(request.address());
        return building;
    }

    public static void updateEntity(Building building, BuildingRequest request) {
        building.setName(request.name());
        building.setAddress(request.address());
    }
}