package com.inventory.mapper;

import com.inventory.model.dto.MovementResponse;
import com.inventory.model.entity.StockMovement;

/**
 * Convertit entre l'entité StockMovement et les DTOs.
 * Classe utilitaire pure (pas de state) → final + constructeur privé.
 */
public final class StockMovementMapper {

    private StockMovementMapper() {}

    /** Entité → DTO de réponse (ce qu'on envoie au client) */
    public static MovementResponse toResponse(StockMovement movement) {
        return new MovementResponse(
                movement.getId(),
                movement.getProduct().getId(),
                movement.getProduct().getName(),
                movement.getProduct().getSku(),
                movement.getType(),
                movement.getQuantity(),
                movement.getFromRoom() != null ? movement.getFromRoom().getId() : null,
                movement.getFromRoom() != null ? movement.getFromRoom().getName() : null,
                movement.getToRoom() != null ? movement.getToRoom().getId() : null,
                movement.getToRoom() != null ? movement.getToRoom().getName() : null,
                movement.getReason(),
                movement.getPerformedBy().getId(),
                movement.getPerformedBy().getFirstName() + " " + movement.getPerformedBy().getLastName(),
                movement.getCreatedAt()
        );
    }
}
