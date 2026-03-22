package com.inventory.model.dto;

import java.time.Instant;
import java.util.UUID;

public record BuildingResponse(
        UUID id,
        String name,
        String address,
        Instant createdAt,
        Instant updatedAt
) {}