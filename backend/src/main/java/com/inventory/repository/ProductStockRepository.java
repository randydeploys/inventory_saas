package com.inventory.repository;

import com.inventory.model.entity.ProductStock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductStockRepository extends JpaRepository<ProductStock, UUID> {

    Optional<ProductStock> findByTenantIdAndProductIdAndRoomId(UUID tenantId, UUID productId, UUID roomId);

    List<ProductStock> findByProductId(UUID productId);

    List<ProductStock> findByTenantIdAndRoomId(UUID tenantId, UUID roomId);

    @Query("SELECT COALESCE(SUM(ps.quantity), 0) FROM ProductStock ps WHERE ps.product.id = :productId")
    int sumQuantityByProductId(@Param("productId") UUID productId);

    // Compter les produits actifs dans une room (pour le DELETE building/room avec 409)
    long countByRoomId(UUID roomId);

    // Compter les produits actifs dans un building (via les rooms)
    @Query("SELECT COUNT(ps) FROM ProductStock ps WHERE ps.room.building.id = :buildingId")
    long countByBuildingId(@Param("buildingId") UUID buildingId);
}
