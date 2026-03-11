package com.inventory.model.dto;

import java.util.List;

public record ApiResponse<T>(boolean success, String message, T data, List<String> errors) {

    // Succès sans data (ex: "Connexion réussie", "Déconnexion réussie")
    public static ApiResponse<Void> success(String message) {
        return new ApiResponse<>(true, message, null, List.of());
    }

    // Succès avec data (ex: retourner un produit après création)
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(true, message, data, List.of());
    }

    // Erreur simple (un seul message)
    public static ApiResponse<Void> error(String message) {
        return new ApiResponse<>(false, message, null, List.of(message));
    }

    // Erreur avec plusieurs messages (ex: validation qui échoue sur 3 champs)
    public static ApiResponse<Void> error(String message, List<String> errors) {
        return new ApiResponse<>(false, message, null, errors);
    }
}