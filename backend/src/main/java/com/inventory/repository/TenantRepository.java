package com.inventory.repository;

import com.inventory.model.entity.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    // Récupérer un tenant via son slug (ex: acme-corp)
    Optional<Tenant> findBySlug(String slug);

    // Vérifier si un tenant existe via son slug
    boolean existsBySlug(String slug);
}
