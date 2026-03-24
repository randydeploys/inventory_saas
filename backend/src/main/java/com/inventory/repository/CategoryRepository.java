package com.inventory.repository;

import com.inventory.model.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data génère automatiquement les requêtes SQL à partir des noms de méthodes.
 * Ex: findByTenantIdAndDeletedAtIsNull → SELECT * FROM category WHERE tenant_id = ? AND deleted_at IS NULL
 */
public interface CategoryRepository extends JpaRepository<Category, UUID> {

    // Toutes les catégories actives du tenant
    List<Category> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // Toutes les catégories archivées du tenant
    List<Category> findByTenantIdAndDeletedAtIsNotNull(UUID tenantId);

    // Une catégorie active par id + tenant (évite les fuites cross-tenant)
    Optional<Category> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}
