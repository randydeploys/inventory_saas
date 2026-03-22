package com.inventory.mapper;

import com.inventory.model.dto.ProductRequest;
import com.inventory.model.dto.ProductResponse;
import com.inventory.model.dto.ProductStockResponse;
import com.inventory.model.entity.Product;
import com.inventory.model.entity.ProductStock;
import com.inventory.model.entity.Tenant;

import java.util.List;

/**
 * Convertit entre l'entité Product (+ stocks) et les DTOs.
 * Classe utilitaire pure (pas de state) → final + constructeur privé.
 */
public final class ProductMapper {

    private ProductMapper() {}

    /** Entité + stocks → DTO de réponse (ce qu'on envoie au client) */
    public static ProductResponse toResponse(Product product, List<ProductStock> stocks, int totalQuantity) {
        List<ProductStockResponse> stockResponses = stocks.stream()
                .map(s -> new ProductStockResponse(
                        s.getRoom().getId(),
                        s.getRoom().getName(),
                        s.getQuantity()
                ))
                .toList();

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getDescription(),
                product.getTrackingType(),
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getSerialNumber(),
                product.getMinQuantity(),
                product.getUnit(),
                totalQuantity,
                stockResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    /**
     * DTO de création → nouvelle entité (sans id ni timestamps — gérés par JPA).
     * Note : category n'est pas renseignée ici — le service fait la résolution via categoryId.
     */
    public static Product toEntity(ProductRequest request, Tenant tenant) {
        Product product = new Product();
        product.setTenant(tenant);
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setTrackingType(request.trackingType());
        product.setSerialNumber(request.serialNumber());
        product.setMinQuantity(request.minQuantity());
        product.setUnit(request.unit());
        return product;
    }

    /**
     * Met à jour les champs mutables d'une entité existante.
     * Note : trackingType est intentionnellement exclu — c'est un invariant structurel
     * (changer le type d'un produit avec du stock en base produirait des données incohérentes).
     * category n'est pas renseignée ici — le service fait la résolution via categoryId.
     */
    public static void updateEntity(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setSku(request.sku());
        product.setDescription(request.description());
        product.setSerialNumber(request.serialNumber());
        product.setMinQuantity(request.minQuantity());
        product.setUnit(request.unit());
    }
}
