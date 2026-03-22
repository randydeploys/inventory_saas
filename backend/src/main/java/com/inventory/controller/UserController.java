package com.inventory.controller;

import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.CreateUserRequest;
import com.inventory.model.dto.UpdateRoleRequest;
import com.inventory.model.dto.UserResponse;
import com.inventory.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAll() {
        List<UserResponse> users = userService.getAll();
        return ResponseEntity.ok(ApiResponse.success("Utilisateurs récupérés", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(@PathVariable UUID id) {
        UserResponse user = userService.getById(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur récupéré", user));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody CreateUserRequest request) {
        UserResponse user = userService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Utilisateur créé", user));
    }

    @PutMapping("/{id}/role")
    public ResponseEntity<ApiResponse<UserResponse>> updateRole(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request
    ) {
        UserResponse user = userService.updateRole(id, request.role());
        return ResponseEntity.ok(ApiResponse.success("Rôle modifié", user));
    }

    @PutMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivate(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur désactivé"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        userService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur désactivé"));
    }
}