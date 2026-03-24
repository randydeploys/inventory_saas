package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.TenantRequest;
import com.inventory.model.dto.TenantResponse;
import com.inventory.service.TenantService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<TenantResponse>> get() {
        return ResponseEntity.ok(ApiResponse.success("Tenant récupéré", tenantService.get()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<TenantResponse>> update(@Valid @RequestBody TenantRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tenant mis à jour", tenantService.update(request)));
    }
}
