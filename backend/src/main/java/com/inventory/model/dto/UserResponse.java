package com.inventory.model.dto;

import java.util.UUID;

public record UserResponse(UUID id, String email, String firstName, String lastName, String role, boolean isActive, UUID tenantId) {

}
