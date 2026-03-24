package com.inventory.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID buildingId,
        String buildingName,
        String name,
        String description,
        int activeProductCount,
        Instant createdAt,
        Instant updatedAt
) {}