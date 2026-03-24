package com.inventory.specification;

import com.inventory.model.entity.Product;
import com.inventory.model.entity.ProductStock;
import com.inventory.model.enums.TrackingType;
import jakarta.persistence.criteria.*;
import org.springframework.data.jpa.domain.Specification;

import java.util.UUID;

/**
 * Builds JPA Specifications for dynamic Product filtering.
 * Combines tenant isolation, active/archived filter and optional search criteria.
 */
public final class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Creates a Specification combining all supported filters.
     *
     * @param tenantId     mandatory — ensures tenant isolation
     * @param archived     true = deleted_at IS NOT NULL, false = IS NULL
     * @param categoryId   optional category filter
     * @param roomId       optional room filter via ProductStock subquery
     * @param buildingId   optional building filter via ProductStock → Room → Building
     * @param trackingType optional tracking type filter (raw string, case-insensitive)
     * @param search       optional search on name OR sku (contains, case-insensitive)
     */
    public static Specification<Product> buildFilter(
            UUID tenantId,
            boolean archived,
            UUID categoryId,
            UUID roomId,
            UUID buildingId,
            String trackingType,
            String search
    ) {
        return (root, query, cb) -> {
            Predicate predicate = cb.equal(root.get("tenant").get("id"), tenantId);

            if (archived) {
                predicate = cb.and(predicate, cb.isNotNull(root.get("deletedAt")));
            } else {
                predicate = cb.and(predicate, cb.isNull(root.get("deletedAt")));
            }

            if (categoryId != null) {
                predicate = cb.and(predicate, cb.equal(root.get("category").get("id"), categoryId));
            }

            if (trackingType != null && !trackingType.isBlank()) {
                try {
                    TrackingType type = TrackingType.valueOf(trackingType.toUpperCase());
                    predicate = cb.and(predicate, cb.equal(root.get("trackingType"), type));
                } catch (IllegalArgumentException ignored) {
                    // unknown enum value → no filter applied
                }
            }

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.toLowerCase() + "%";
                Predicate nameMatch = cb.like(cb.lower(root.get("name")), pattern);
                Predicate skuMatch  = cb.like(cb.lower(root.get("sku")), pattern);
                predicate = cb.and(predicate, cb.or(nameMatch, skuMatch));
            }

            if (roomId != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<ProductStock> stockRoot = sub.from(ProductStock.class);
                sub.select(stockRoot.get("product").get("id"))
                   .where(cb.equal(stockRoot.get("room").get("id"), roomId));
                predicate = cb.and(predicate, root.get("id").in(sub));
            }

            if (buildingId != null) {
                Subquery<UUID> sub = query.subquery(UUID.class);
                Root<ProductStock> stockRoot = sub.from(ProductStock.class);
                sub.select(stockRoot.get("product").get("id"))
                   .where(cb.equal(stockRoot.get("room").get("building").get("id"), buildingId));
                predicate = cb.and(predicate, root.get("id").in(sub));
            }

            return predicate;
        };
    }
}
