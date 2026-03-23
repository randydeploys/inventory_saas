package com.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Size(max = 100) @Pattern(regexp = "^[a-z0-9-]+$", message = "Le slug ne peut contenir que des lettres minuscules, chiffres et tirets") String slug
) {}
