package com.inventory.model.dto;

import com.inventory.model.enums.TrackingType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String description,
        TrackingType trackingType,
        UUID categoryId,
        String categoryName,
        String serialNumber,       // null si QUANTITY
        Integer minQuantity,       // null si UNIQUE
        String unit,               // null si UNIQUE
        int totalQuantity,
        List<ProductStockResponse> stocks,
        Instant createdAt,
        Instant updatedAt
) {}
