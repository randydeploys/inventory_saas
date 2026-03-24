package com.inventory.model.dto;

import com.inventory.model.enums.TrackingType;
import com.inventory.validation.ValidProduct;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

@ValidProduct
public record ProductRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) String sku,
        String description,
        @NotNull TrackingType trackingType,
        UUID categoryId,          // nullable — un produit peut ne pas avoir de catégorie
        String serialNumber,      // requis si UNIQUE, null si QUANTITY
        Integer minQuantity,      // requis si QUANTITY, null si UNIQUE
        @Size(max = 50) String unit // requis si QUANTITY, null si UNIQUE
) {}