package com.inventory.mapper;

import com.inventory.model.dto.RoomRequest;
import com.inventory.model.dto.RoomResponse;
import com.inventory.model.entity.Building;
import com.inventory.model.entity.Room;
import com.inventory.model.entity.Tenant;

public final class RoomMapper {

    private RoomMapper() {}

    public static RoomResponse toResponse(Room room, long activeProductCount) {
        return new RoomResponse(
                room.getId(),
                room.getBuilding().getId(),
                room.getBuilding().getName(),
                room.getName(),
                room.getDescription(),
                (int) activeProductCount,
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public static RoomResponse toResponse(Room room) {
        return toResponse(room, 0);
    }

    public static Room toEntity(RoomRequest request, Building building, Tenant tenant) {
        Room room = new Room();
        room.setTenant(tenant);
        room.setBuilding(building);
        room.setName(request.name());
        room.setDescription(request.description());
        return room;
    }

    public static void updateEntity(Room room, RoomRequest request) {
        room.setName(request.name());
        room.setDescription(request.description());
    }
}