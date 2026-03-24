package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.MovementRequest;
import com.inventory.model.dto.MovementResponse;
import com.inventory.model.enums.MovementType;
import com.inventory.service.StockMovementService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    // Tous les rôles peuvent consulter l'historique
    @GetMapping("/api/movements")
    public ResponseEntity<ApiResponse<Page<MovementResponse>>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID productId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) Instant fromDate,
            @RequestParam(required = false) Instant toDate,
            @RequestParam(required = false) UUID performedBy
    ) {
        return ResponseEntity.ok(ApiResponse.success("Historique récupéré",
                stockMovementService.getHistory(page, size, productId, type, fromDate, toDate, performedBy)));
    }

    // Enregistrement d'un mouvement réservé à Admin et Manager
    @PostMapping("/api/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<MovementResponse>> recordMovement(
            @Valid @RequestBody MovementRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mouvement enregistré", stockMovementService.recordMovement(request)));
    }

    // Historique des mouvements d'un produit — tous rôles
    @GetMapping("/api/products/{id}/movements")
    public ResponseEntity<ApiResponse<Page<MovementResponse>>> getHistoryByProduct(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Historique récupéré",
                stockMovementService.getHistoryByProduct(id, page, size)));
    }
}
