package com.inventory.model.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Ce que l'API renvoie. On ne retourne jamais l'entité directement —
 * ça évite d'exposer des champs sensibles (tenant_id, relations JPA lazy, etc.)
 */
public record CategoryResponse(
        UUID id,
        String name,
        String color,
        Instant createdAt,
        Instant updatedAt
) {}
