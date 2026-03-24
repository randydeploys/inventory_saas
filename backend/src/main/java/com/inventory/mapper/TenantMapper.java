package com.inventory.mapper;

import com.inventory.model.dto.TenantResponse;
import com.inventory.model.entity.Tenant;

public final class TenantMapper {

    private TenantMapper() {}

    public static TenantResponse toResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getSlug(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
