package com.inventory.specification;

import com.inventory.model.entity.StockMovement;
import com.inventory.model.enums.MovementType;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

public final class StockMovementSpecification {

    private StockMovementSpecification() {}

    public static Specification<StockMovement> build(UUID tenantId, UUID productId, MovementType type,
                                                     Instant fromDate, Instant toDate, UUID performedBy) {
        Specification<StockMovement> spec = (root, query, cb) ->
                cb.equal(root.get("tenant").get("id"), tenantId);

        if (productId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("product").get("id"), productId));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("type"), type));
        }
        if (fromDate != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
        }
        if (toDate != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
        }
        if (performedBy != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("performedBy").get("id"), performedBy));
        }

        return spec;
    }
}
