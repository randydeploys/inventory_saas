package com.inventory.model.dto;

import com.inventory.model.enums.MovementType;

import java.time.Instant;
import java.util.UUID;

public record MovementResponse(
        UUID id,
        UUID productId,
        String productName,
        String productSku,
        MovementType type,
        int quantity,
        UUID fromRoomId,       // null si IN
        String fromRoomName,   // null si IN
        UUID toRoomId,         // null si OUT
        String toRoomName,     // null si OUT
        String reason,
        UUID performedBy,
        String performedByName,
        Instant createdAt
) {}
