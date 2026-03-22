package com.inventory.model.dto;

import java.util.UUID;

public record ProductStockResponse(
        UUID roomId,
        String roomName,
        int quantity
) {}
