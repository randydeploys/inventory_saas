package com.inventory.repository;

import com.inventory.model.entity.Building;
import com.inventory.model.entity.Room;
import com.inventory.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoomRepository extends JpaRepository<Room, UUID> {

    // Liste des zones actives d'un bâtiment spécifique
    List<Room> findByTenantAndBuildingAndDeletedAtIsNull(Tenant tenant, Building building);

    // Liste des zones archivées d'un bâtiment spécifique
    List<Room> findByTenantAndBuildingAndDeletedAtIsNotNull(Tenant tenant, Building building);

    // Trouver une zone active précise
    Optional<Room> findByIdAndTenantAndDeletedAtIsNull(UUID id, Tenant tenant);
}