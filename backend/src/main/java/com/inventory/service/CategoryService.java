package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.CategoryMapper;
import com.inventory.model.dto.CategoryRequest;
import com.inventory.model.dto.CategoryResponse;
import com.inventory.model.entity.Category;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public CategoryService(
            CategoryRepository categoryRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    /** Retourne les catégories actives ou archivées selon le paramètre */
    public List<CategoryResponse> getAll(boolean archived) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        List<Category> categories = archived
                ? categoryRepository.findByTenantIdAndDeletedAtIsNotNull(tenantId)
                : categoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId);

        return categories.stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    /** Retourne une catégorie active par son id, 404 si introuvable */
    public CategoryResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Category category = categoryRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));

        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        Category category = CategoryMapper.toEntity(request, tenant);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        category.setUpdatedBy(currentUser);
        category = categoryRepository.save(category);

        return CategoryMapper.toResponse(category);
    }

    @Transactional
    public CategoryResponse update(UUID id, CategoryRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Category category = categoryRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));

        CategoryMapper.updateEntity(category, request);
        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        category.setUpdatedBy(currentUser);
        category = categoryRepository.save(category);

        return CategoryMapper.toResponse(category);
    }

    /**
     * Soft delete : on remplit deleted_at au lieu de supprimer.
     * Les produits liés gardent leur category_id — aucun cascade.
     */
    @Transactional
    public void delete(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Category category = categoryRepository
                .findByIdAndTenantIdAndDeletedAtIsNull(id, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));

        category.setDeletedAt(Instant.now());
        categoryRepository.save(category);
    }
}
