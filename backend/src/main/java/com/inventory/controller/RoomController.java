package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.RoomRequest;
import com.inventory.model.dto.RoomResponse;
import com.inventory.service.RoomService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAll(
            @RequestParam(defaultValue = "false") boolean archived
    ) {
        List<RoomResponse> rooms = roomService.getAll(archived);
        return ResponseEntity.ok(ApiResponse.success("Zones récupérées", rooms));
    }

    @GetMapping("/buildings/{buildingId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> getAllByBuilding(
            @PathVariable UUID buildingId,
            @RequestParam(defaultValue = "false") boolean archived
    ) {
        List<RoomResponse> rooms = roomService.getAllByBuilding(buildingId, archived);
        return ResponseEntity.ok(ApiResponse.success("Zones récupérées", rooms));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(@PathVariable UUID id) {
        RoomResponse room = roomService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Zone récupérée", room));
    }

    @PostMapping("/buildings/{buildingId}/rooms")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomResponse>> create(
            @PathVariable UUID buildingId,
            @Valid @RequestBody RoomRequest request
    ) {
        RoomResponse room = roomService.create(buildingId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Zone créée", room));
    }

    @PutMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<RoomResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody RoomRequest request
    ) {
        RoomResponse room = roomService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Zone modifiée", room));
    }

    @DeleteMapping("/rooms/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        roomService.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Zone archivée"));
    }
}