package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.ProductRequest;
import com.inventory.model.dto.ProductResponse;
import com.inventory.model.entity.*;
import com.inventory.model.enums.TrackingType;
import com.inventory.repository.*;
import com.inventory.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock private ProductRepository productRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private ProductService productService;

    private UUID tenantId;
    private UUID userId;
    private Tenant tenant;
    private User user;
    private Category category;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Corp");

        user = new User();
        user.setId(userId);

        category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Électronique");
        category.setColor("#3498DB");
    }

    // ─── getAll ─────────────────────────────────────────────────

    @Test
    void getAll_activeProducts_shouldReturnPage() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product product = createProduct("Widget", "WDG-001", TrackingType.QUANTITY);
        Page<Product> page = new PageImpl<>(List.of(product), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productStockRepository.findByProductId(product.getId())).thenReturn(List.of());

        Page<ProductResponse> result = productService.getAll(false, 0, 20, null, null, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Widget", result.getContent().get(0).name());
        assertEquals("WDG-001", result.getContent().get(0).sku());
    }

    @Test
    void getAll_archivedProducts_shouldReturnArchivedPage() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product archived = createProduct("Old Widget", "OLD-001", TrackingType.QUANTITY);
        archived.setDeletedAt(Instant.now());
        Page<Product> page = new PageImpl<>(List.of(archived), PageRequest.of(0, 20), 1);

        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);
        when(productStockRepository.findByProductId(archived.getId())).thenReturn(List.of());

        Page<ProductResponse> result = productService.getAll(true, 0, 20, null, null, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals("Old Widget", result.getContent().get(0).name());
    }

    @Test
    void getAll_withSizeTooLarge_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> productService.getAll(false, 0, 101, null, null, null, null, null));
    }

    @Test
    void getAll_withDefaultSize_shouldUsePageable() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        Page<Product> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(productRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(emptyPage);

        Page<ProductResponse> result = productService.getAll(false, 0, 20, null, null, null, null, null);

        assertEquals(0, result.getTotalElements());
    }

    // ─── getById ─────────────────────────────────────────────────

    @Test
    void getById_withValidId_shouldReturnProductWithStocks() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product product = createProduct("Laptop", "LPT-001", TrackingType.UNIQUE);
        product.setId(productId);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(product));

        Room room = new Room();
        room.setId(UUID.randomUUID());
        room.setName("Zone A");

        ProductStock stock = new ProductStock();
        stock.setId(UUID.randomUUID());
        stock.setProduct(product);
        stock.setRoom(room);
        stock.setQuantity(1);

        when(productStockRepository.findByProductId(productId)).thenReturn(List.of(stock));
        when(productStockRepository.sumQuantityByProductId(productId)).thenReturn(1);

        ProductResponse result = productService.getById(productId);

        assertEquals(productId, result.id());
        assertEquals("Laptop", result.name());
        assertEquals(1, result.totalQuantity());
        assertEquals(1, result.stocks().size());
        assertEquals("Zone A", result.stocks().get(0).roomName());
    }

    @Test
    void getById_withInvalidId_shouldThrowNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.getById(productId));
    }

    @Test
    void getById_withNoStock_shouldReturnZeroTotalQuantity() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product product = createProduct("Empty Box", "EMT-001", TrackingType.QUANTITY);
        product.setId(productId);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(product));
        when(productStockRepository.findByProductId(productId)).thenReturn(List.of());
        when(productStockRepository.sumQuantityByProductId(productId)).thenReturn(0);

        ProductResponse result = productService.getById(productId);

        assertEquals(0, result.totalQuantity());
        assertTrue(result.stocks().isEmpty());
    }

    // ─── create ──────────────────────────────────────────────────

    @Test
    void create_shouldReturnCreatedProduct() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "WDG-001"))
                .thenReturn(Optional.empty());
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    p.setId(UUID.randomUUID());
                    p.setCreatedAt(Instant.now());
                    p.setUpdatedAt(Instant.now());
                    return p;
                });

        ProductRequest request = new ProductRequest(
                "Widget", "WDG-001", "A widget", TrackingType.QUANTITY,
                null, null, 10, "pcs"
        );
        ProductResponse result = productService.create(request);

        assertNotNull(result.id());
        assertEquals("Widget", result.name());
        assertEquals("WDG-001", result.sku());
        assertEquals(0, result.totalQuantity());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void create_withDuplicateSku_shouldThrowConflictException() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "WDG-001"))
                .thenReturn(Optional.of(createProduct("Existing", "WDG-001", TrackingType.QUANTITY)));

        ProductRequest request = new ProductRequest(
                "Widget", "WDG-001", null, TrackingType.QUANTITY,
                null, null, 5, "pcs"
        );

        assertThrows(ConflictException.class, () -> productService.create(request));
        verify(productRepository, never()).save(any());
    }

    @Test
    void create_withCategory_shouldResolveCategory() {
        UUID categoryId = category.getId();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(userRepository.getReferenceById(userId)).thenReturn(user);
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "LPT-001"))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByIdAndTenantIdAndDeletedAtIsNull(categoryId, tenantId))
                .thenReturn(Optional.of(category));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> {
                    Product p = invocation.getArgument(0);
                    p.setId(UUID.randomUUID());
                    p.setCreatedAt(Instant.now());
                    p.setUpdatedAt(Instant.now());
                    return p;
                });

        ProductRequest request = new ProductRequest(
                "Laptop", "LPT-001", null, TrackingType.UNIQUE,
                categoryId, "SN-12345", null, null
        );
        ProductResponse result = productService.create(request);

        assertEquals("Laptop", result.name());
        assertEquals(categoryId, result.categoryId());
        assertEquals("Électronique", result.categoryName());
    }

    // ─── update ──────────────────────────────────────────────────

    @Test
    void update_shouldReturnUpdatedProduct() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Product existing = createProduct("Old Name", "OLD-001", TrackingType.QUANTITY);
        existing.setId(productId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(existing));
        // SKU not changed → no conflict
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "OLD-001"))
                .thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productStockRepository.findByProductId(productId)).thenReturn(List.of());
        when(productStockRepository.sumQuantityByProductId(productId)).thenReturn(0);

        ProductRequest request = new ProductRequest(
                "New Name", "OLD-001", "Updated desc", TrackingType.QUANTITY,
                null, null, 15, "kg"
        );
        ProductResponse result = productService.update(productId, request);

        assertEquals("New Name", result.name());
        assertEquals(productId, result.id());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void update_withInvalidId_shouldThrowNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.empty());

        ProductRequest request = new ProductRequest(
                "Name", "SKU-001", null, TrackingType.QUANTITY,
                null, null, 5, "pcs"
        );
        assertThrows(ResourceNotFoundException.class,
                () -> productService.update(productId, request));
    }

    @Test
    void update_withSkuChangedToExisting_shouldThrowConflictException() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product existing = createProduct("My Product", "MY-001", TrackingType.QUANTITY);
        existing.setId(productId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(existing));

        // Another product already has the new SKU
        Product other = createProduct("Other Product", "OTHER-002", TrackingType.QUANTITY);
        other.setId(UUID.randomUUID());
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "OTHER-002"))
                .thenReturn(Optional.of(other));

        ProductRequest request = new ProductRequest(
                "My Product", "OTHER-002", null, TrackingType.QUANTITY,
                null, null, 5, "pcs"
        );
        assertThrows(ConflictException.class,
                () -> productService.update(productId, request));
    }

    @Test
    void update_skuUnchanged_shouldNotCheckConflict() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(securityHelper.getCurrentUserId()).thenReturn(userId);
        when(userRepository.getReferenceById(userId)).thenReturn(user);

        Product existing = createProduct("Product", "SKU-001", TrackingType.QUANTITY);
        existing.setId(productId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(existing));
        // SKU lookup returns the same product → no conflict
        when(productRepository.findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, "SKU-001"))
                .thenReturn(Optional.of(existing));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(productStockRepository.findByProductId(productId)).thenReturn(List.of());
        when(productStockRepository.sumQuantityByProductId(productId)).thenReturn(0);

        ProductRequest request = new ProductRequest(
                "Product Updated", "SKU-001", null, TrackingType.QUANTITY,
                null, null, 5, "pcs"
        );

        assertDoesNotThrow(() -> productService.update(productId, request));
    }

    // ─── softDelete ──────────────────────────────────────────────

    @Test
    void softDelete_shouldSetDeletedAt() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product product = createProduct("To Delete", "DEL-001", TrackingType.QUANTITY);
        product.setId(productId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        productService.softDelete(productId);

        assertNotNull(product.getDeletedAt());
        verify(productRepository, times(1)).save(product);
    }

    @Test
    void softDelete_withInvalidId_shouldThrowNotFoundException() {
        UUID productId = UUID.randomUUID();
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> productService.softDelete(productId));
    }

    // ─── getLowStock ─────────────────────────────────────────────

    @Test
    void getLowStock_shouldReturnProductsBelowMinQuantity() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product p1 = createProduct("Low Stock Item", "LSI-001", TrackingType.QUANTITY);
        p1.setMinQuantity(10);
        p1.setId(UUID.randomUUID());

        Product p2 = createProduct("OK Stock Item", "OSI-001", TrackingType.QUANTITY);
        p2.setMinQuantity(5);
        p2.setId(UUID.randomUUID());

        when(productRepository.findByTenantIdAndTrackingTypeAndDeletedAtIsNull(tenantId, TrackingType.QUANTITY))
                .thenReturn(List.of(p1, p2));

        when(productStockRepository.sumQuantityByProductId(p1.getId())).thenReturn(3);  // 3 < 10 → low
        when(productStockRepository.sumQuantityByProductId(p2.getId())).thenReturn(8);  // 8 >= 5 → ok

        when(productStockRepository.findByProductId(p1.getId())).thenReturn(List.of());

        List<ProductResponse> result = productService.getLowStock();

        assertEquals(1, result.size());
        assertEquals("Low Stock Item", result.get(0).name());
    }

    @Test
    void getLowStock_shouldSkipProductsWithNullMinQuantity() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product p = createProduct("No Min", "NM-001", TrackingType.QUANTITY);
        p.setMinQuantity(null);
        p.setId(UUID.randomUUID());

        when(productRepository.findByTenantIdAndTrackingTypeAndDeletedAtIsNull(tenantId, TrackingType.QUANTITY))
                .thenReturn(List.of(p));

        List<ProductResponse> result = productService.getLowStock();

        assertTrue(result.isEmpty());
        verify(productStockRepository, never()).sumQuantityByProductId(any());
    }

    @Test
    void getLowStock_allAboveMin_shouldReturnEmpty() {
        when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);

        Product p = createProduct("Good Stock", "GS-001", TrackingType.QUANTITY);
        p.setMinQuantity(5);
        p.setId(UUID.randomUUID());

        when(productRepository.findByTenantIdAndTrackingTypeAndDeletedAtIsNull(tenantId, TrackingType.QUANTITY))
                .thenReturn(List.of(p));
        when(productStockRepository.sumQuantityByProductId(p.getId())).thenReturn(10);

        List<ProductResponse> result = productService.getLowStock();

        assertTrue(result.isEmpty());
    }

    // ─── Helper ──────────────────────────────────────────────────

    private Product createProduct(String name, String sku, TrackingType trackingType) {
        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setTenant(tenant);
        product.setName(name);
        product.setSku(sku);
        product.setTrackingType(trackingType);
        product.setCreatedAt(Instant.now());
        product.setUpdatedAt(Instant.now());
        return product;
    }
}
