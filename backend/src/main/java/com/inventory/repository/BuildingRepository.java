package com.inventory.repository;

import com.inventory.model.entity.Building;
import com.inventory.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BuildingRepository extends JpaRepository<Building, UUID> {

    // Liste des bâtiments actifs
    List<Building> findByTenantAndDeletedAtIsNull(Tenant tenant);

    // Liste des bâtiments archivés
    List<Building> findByTenantAndDeletedAtIsNotNull(Tenant tenant);

    // Trouver un bâtiment actif précis pour ce tenant
    Optional<Building> findByIdAndTenantAndDeletedAtIsNull(UUID id, Tenant tenant);
}