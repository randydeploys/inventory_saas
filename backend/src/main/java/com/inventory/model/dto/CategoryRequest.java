package com.inventory.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ce que l'API reçoit quand on crée ou modifie une catégorie.
 * Les annotations de validation sont vérifiées automatiquement par Spring (@Valid).
 */
public record CategoryRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @Pattern(regexp = "#[0-9A-Fa-f]{6}", message = "La couleur doit être un code hex valide (ex: #FF5733)")
        String color
) {}
