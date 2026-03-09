package com.inventory.model.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity                         // Dit à JPA : cette classe = une table
@Table(name = "tenant")         // Dit à JPA : le nom de la table en base = "tenant"
@Getter                         // Lombok : génère les getters
@Setter                         // Lombok : génère les setters
@NoArgsConstructor              // Lombok : génère le constructeur sans arguments (obligatoire pour JPA)
@AllArgsConstructor              // Constructeur avec tous les champs
@Builder                         // Permet de créer : Tenant.builder().name("X").build()
public class Tenant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
