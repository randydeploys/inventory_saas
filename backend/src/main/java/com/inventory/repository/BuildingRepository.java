package com.inventory.repository;

import com.inventory.model.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    // Buildings actifs d'un tenant
    List<Building> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // Buildings archivés d'un tenant
    List<Building> findByTenantIdAndDeletedAtIsNotNull(UUID tenantId);

    // Un building actif par id et tenant
    Optional<Building> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);

    // Un building (actif ou archivé) par id et tenant
    Optional<Building> findByIdAndTenantId(UUID id, UUID tenantId);
}