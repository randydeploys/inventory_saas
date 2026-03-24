package com.inventory.mapper;

import com.inventory.model.dto.CategoryRequest;
import com.inventory.model.dto.CategoryResponse;
import com.inventory.model.entity.Category;
import com.inventory.model.entity.Tenant;

/**
 * Convertit entre l'entité Category et les DTOs.
 * Classe utilitaire pure (pas de state) → final + constructeur privé.
 */
public final class CategoryMapper {

    private CategoryMapper() {}

    /** Entité → DTO de réponse (ce qu'on envoie au client) */
    public static CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getColor(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }

    /** DTO de création → nouvelle entité (sans id ni timestamps — gérés par JPA) */
    public static Category toEntity(CategoryRequest request, Tenant tenant) {
        Category category = new Category();
        category.setTenant(tenant);
        category.setName(request.name());
        category.setColor(request.color());
        return category;
    }

    /** Met à jour une entité existante avec les nouvelles valeurs */
    public static void updateEntity(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setColor(request.color());
    }
}
