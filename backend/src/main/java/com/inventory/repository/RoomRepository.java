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

 // Rooms actives d'un building
    List<Room> findByBuildingIdAndTenantIdAndDeletedAtIsNull(UUID buildingId, UUID tenantId);

    // Rooms archivées d'un building
    List<Room> findByBuildingIdAndTenantIdAndDeletedAtIsNotNull(UUID buildingId, UUID tenantId);

    // Toutes les rooms actives d'un tenant (toutes les buildings)
    List<Room> findByTenantIdAndDeletedAtIsNull(UUID tenantId);

    // Toutes les rooms archivées d'un tenant
    List<Room> findByTenantIdAndDeletedAtIsNotNull(UUID tenantId);

    // Une room active par id et tenant
    Optional<Room> findByIdAndTenantIdAndDeletedAtIsNull(UUID id, UUID tenantId);
}