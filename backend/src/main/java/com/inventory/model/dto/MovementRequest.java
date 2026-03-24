package com.inventory.model.dto;

import com.inventory.model.enums.MovementType;
import com.inventory.validation.ValidMovement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@ValidMovement
public record MovementRequest(
        @NotNull UUID productId,
        @NotNull MovementType type,
        @NotNull @Min(1) Integer quantity,
        UUID fromRoomId,   // null si IN
        UUID toRoomId,     // null si OUT
        String reason
) {}
