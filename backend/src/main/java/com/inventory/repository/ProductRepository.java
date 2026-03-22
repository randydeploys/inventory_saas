package com.inventory.repository;

import com.inventory.model.entity.Product;
import com.inventory.model.enums.TrackingType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByTenantIdAndSkuAndDeletedAtIsNull(UUID tenantId, String sku);

    Optional<Product> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Page<Product> findByTenantIdAndDeletedAtIsNull(UUID tenantId, Pageable pageable);

    Page<Product> findByTenantIdAndDeletedAtIsNotNull(UUID tenantId, Pageable pageable);

    List<Product> findByTenantIdAndTrackingTypeAndDeletedAtIsNull(UUID tenantId, TrackingType trackingType);
}
