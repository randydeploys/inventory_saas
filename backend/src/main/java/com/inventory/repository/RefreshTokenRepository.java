package com.inventory.repository;

import com.inventory.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    // Optional<RefreshToken> findByToken(String token);
    //  cherche un token valide (non révoqué)
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    // Pour le login/logout/sécurité : révoque tous les tokens d'un user
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user.id = :userId AND rt.revoked = false")
    void revokeAllByUserId(@Param("userId") UUID userId);
}