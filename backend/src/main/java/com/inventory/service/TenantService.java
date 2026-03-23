package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.TenantMapper;
import com.inventory.model.dto.TenantRequest;
import com.inventory.model.dto.TenantResponse;
import com.inventory.model.entity.Tenant;
import com.inventory.repository.TenantRepository;
import com.inventory.security.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantService {

    private final TenantRepository tenantRepository;
    private final SecurityHelper securityHelper;

    public TenantService(TenantRepository tenantRepository, SecurityHelper securityHelper) {
        this.tenantRepository = tenantRepository;
        this.securityHelper = securityHelper;
    }

    public TenantResponse get() {
        UUID tenantId = securityHelper.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));
        return TenantMapper.toResponse(tenant);
    }

    @Transactional
    public TenantResponse update(TenantRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        if (!tenant.getSlug().equals(request.slug()) && tenantRepository.existsBySlug(request.slug())) {
            throw new ConflictException("Ce slug est déjà utilisé");
        }

        tenant.setName(request.name());
        tenant.setSlug(request.slug());
        return TenantMapper.toResponse(tenantRepository.save(tenant));
    }
}
