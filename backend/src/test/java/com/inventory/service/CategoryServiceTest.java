package com.inventory.service;

import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.CategoryRequest;
import com.inventory.model.dto.CategoryResponse;
import com.inventory.model.entity.Category;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.repository.CategoryRepository;
import com.inventory.repository.TenantRepository;
import com.inventory.repository.UserRepository;
import com.inventory.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock private CategoryRepository categoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private CategoryService categoryService;

    private UUID tenantId;
    private UUID userId;
    private Tenant tenant;
    private User user;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Corp");

        user = new User();
        user.setId(userId);
    }

    // ─── getAll ──────────────────────────────────────────────────

    @Test
    void getAll_shouldReturnActiveCategories() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(categoryRepository.findByTenantIdAndDeletedAtIsNull(tenantId))
                .thenReturn(List.of(createCategory("Électronique", "#3498DB")));

        List<CategoryResponse> result = categoryService.getAll(false);

        assertEquals(1, result.size());
        assertEquals("Électronique", result.get(0).name());
        assertEquals("#3498DB", result.get(0).color());
    }

    @Test
    void getAll_archived_shouldReturnArchivedCategories() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        Category archived = createCategory("Ancien stock", "#999999");
        archived.setDeletedAt(Instant.now());
        when(categoryRepository.findByTenantIdAndDeletedAtIsNotNull(tenantId))
                .thenReturn(List.of(archived));

        List<CategoryResponse> result = categoryService.getAll(true);

        assertEquals(1, result.size());
        assertEquals("Ancien stock", result.get(0).name());
    }

    // ─── getById ─────────────────────────────────────────────────

    @Test
    void getById_withValidId_shouldReturnCategory() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        Category category = createCategory("Électronique", "#3498DB");
        category.setId(categoryId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.of(category));

        CategoryResponse result = categoryService.getById(categoryId);

        assertEquals("Électronique", result.name());
        assertEquals(categoryId, result.id());
    }

    @Test
    void getById_withInvalidId_shouldThrowNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.getById(categoryId));
    }

    // ─── create ──────────────────────────────────────────────────

    @Test
    void create_shouldReturnCreatedCategory() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> {
                    Category c = invocation.getArgument(0);
                    c.setId(UUID.randomUUID());
                    c.setCreatedAt(Instant.now());
                    c.setUpdatedAt(Instant.now());
                    return c;
                });

        CategoryRequest request = new CategoryRequest("Électronique", "#3498DB");
        CategoryResponse result = categoryService.create(request);

        assertEquals("Électronique", result.name());
        assertEquals("#3498DB", result.color());
        assertNotNull(result.id());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    // ─── update ──────────────────────────────────────────────────

    @Test
    void update_shouldReturnUpdatedCategory() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Category existing = createCategory("Ancien nom", "#111111");
        existing.setId(categoryId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        CategoryRequest request = new CategoryRequest("Nouveau nom", "#FF5733");
        CategoryResponse result = categoryService.update(categoryId, request);

        assertEquals("Nouveau nom", result.name());
        assertEquals("#FF5733", result.color());
    }

    @Test
    void update_withInvalidId_shouldThrowNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.update(categoryId, new CategoryRequest("Nom", "#FF5733")));
    }

    // ─── delete ──────────────────────────────────────────────────

    @Test
    void delete_shouldSoftDelete() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Category category = createCategory("À supprimer", "#000000");
        category.setId(categoryId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.of(category));
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        categoryService.delete(categoryId);

        // deletedAt doit être rempli (soft delete, pas une vraie suppression)
        assertNotNull(category.getDeletedAt());
        verify(categoryRepository, times(1)).save(category);
    }

    @Test
    void delete_withInvalidId_shouldThrowNotFoundException() {
        UUID categoryId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> categoryService.delete(categoryId));
    }

    // ─── Helper ──────────────────────────────────────────────────

    private Category createCategory(String name, String color) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setTenant(tenant);
        category.setName(name);
        category.setColor(color);
        category.setCreatedAt(Instant.now());
        category.setUpdatedAt(Instant.now());
        return category;
    }
}
