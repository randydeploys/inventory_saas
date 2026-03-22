package com.inventory.model.dto;

import java.time.Instant;
import java.util.UUID;

public record RoomResponse(
        UUID id,
        UUID buildingId,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {}