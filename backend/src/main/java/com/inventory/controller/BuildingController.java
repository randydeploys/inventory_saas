package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.service.BuildingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/buildings")
public class BuildingController {

    private final BuildingService buildingService;

    public BuildingController(BuildingService buildingService) {
        this.buildingService = buildingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<BuildingResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean archived
    ) {
        List<BuildingResponse> buildings = buildingService.getAll(archived);
        return ResponseEntity.ok(ApiResponse.success("Bâtiments récupérés", buildings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BuildingResponse>> getById(@PathVariable UUID id) {
        BuildingResponse building = buildingService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Bâtiment récupéré", building));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BuildingResponse>> create(
            @Valid @RequestBody BuildingRequest request
    ) {
        BuildingResponse building = buildingService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Bâtiment créé", building));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<BuildingResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody BuildingRequest request
    ) {
        BuildingResponse building = buildingService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Bâtiment modifié", building));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        buildingService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Bâtiment archivé"));
    }
}