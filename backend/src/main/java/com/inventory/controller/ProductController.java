package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.ProductRequest;
import com.inventory.model.dto.ProductResponse;
import com.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Tous les rôles peuvent consulter les produits
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean archived,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) UUID roomId,
            @RequestParam(required = false) UUID buildingId,
            @RequestParam(required = false) String trackingType,
            @RequestParam(required = false) String search
    ) {
        return ResponseEntity.ok(ApiResponse.success("Produits récupérés",
                productService.getAll(archived, page, size, categoryId, roomId, buildingId, trackingType, search)));
    }

    // low-stock doit être déclaré avant /{id} pour éviter le conflit de routing
    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.success("Produits en stock faible", productService.getLowStock()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success("Produit récupéré", productService.getById(id)));
    }

    // Création et modification réservées à Admin et Manager
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Produit créé", productService.create(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<ProductResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ProductRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success("Produit modifié", productService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> softDelete(@PathVariable UUID id) {
        productService.softDelete(id);
        return ResponseEntity.ok(ApiResponse.success("Produit archivé"));
    }
}
