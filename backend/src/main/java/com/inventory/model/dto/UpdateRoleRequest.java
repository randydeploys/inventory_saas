package com.inventory.model.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRoleRequest(@NotNull String role) {}