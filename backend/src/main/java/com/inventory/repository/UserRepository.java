package com.inventory.repository;

import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmailAndTenant(String email, Tenant tenant);

    Optional<User> findByIdAndTenant(UUID id, Tenant tenant);

    List<User> findByTenant(Tenant tenant);

    // Filtre pour ne récupérer que les utilisateurs dont le booléen active est true
    List<User> findByTenantAndActiveTrue(Tenant tenant);

    boolean existsByEmailAndTenant(String email, Tenant tenant);

}
