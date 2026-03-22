package com.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuildingRequest(
        @NotBlank @Size(max = 255) String name,
        String address
) {}