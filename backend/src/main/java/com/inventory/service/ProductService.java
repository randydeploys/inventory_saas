package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.ProductMapper;
import com.inventory.model.dto.ProductRequest;
import com.inventory.model.dto.ProductResponse;
import com.inventory.model.entity.Category;
import com.inventory.model.entity.Product;
import com.inventory.model.entity.ProductStock;
import com.inventory.model.entity.Tenant;
import com.inventory.model.entity.User;
import com.inventory.model.enums.TrackingType;
import com.inventory.repository.*;
import com.inventory.security.SecurityHelper;
import com.inventory.specification.ProductSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductStockRepository productStockRepository;
    private final CategoryRepository categoryRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public ProductService(
            ProductRepository productRepository,
            ProductStockRepository productStockRepository,
            CategoryRepository categoryRepository,
            TenantRepository tenantRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.productRepository = productRepository;
        this.productStockRepository = productStockRepository;
        this.categoryRepository = categoryRepository;
        this.tenantRepository = tenantRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    /**
     * Returns a paginated list of products with optional filters.
     * archived=false → active products (deletedAt IS NULL)
     * archived=true  → archived products (deletedAt IS NOT NULL)
     * size max 100, throws IllegalArgumentException if exceeded.
     */
    public Page<ProductResponse> getAll(
            boolean archived,
            int page,
            int size,
            UUID categoryId,
            UUID roomId,
            UUID buildingId,
            String trackingType,
            String search
    ) {
        if (size > 100) {
            throw new IllegalArgumentException("Page size must not exceed 100");
        }

        UUID tenantId = securityHelper.getCurrentTenantId();

        Specification<Product> spec = ProductSpecification.buildFilter(
                tenantId, archived, categoryId, roomId, buildingId, trackingType, search
        );

        Pageable pageable = PageRequest.of(page, size);
        Page<Product> products = productRepository.findAll(spec, pageable);

        return products.map(product -> {
            List<ProductStock> stocks = productStockRepository.findByProductId(product.getId());
            int total = stocks.stream().mapToInt(ProductStock::getQuantity).sum();
            return ProductMapper.toResponse(product, stocks, total);
        });
    }

    /**
     * Returns a single active product by id, with its stock breakdown.
     * 404 if not found or soft-deleted.
     */
    public ProductResponse getById(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Product product = productRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        List<ProductStock> stocks = productStockRepository.findByProductId(id);
        int total = productStockRepository.sumQuantityByProductId(id);

        return ProductMapper.toResponse(product, stocks, total);
    }

    /**
     * Creates a new product.
     * Validates SKU uniqueness on active products.
     * Does NOT create ProductStock — stock is added via a movement IN.
     */
    @Transactional
    public ProductResponse create(ProductRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Optional<Product> existing = productRepository
                .findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, request.sku());
        if (existing.isPresent()) {
            throw new ConflictException("Un produit actif avec le SKU '" + request.sku() + "' existe déjà");
        }

        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant introuvable"));

        Product product = ProductMapper.toEntity(request, tenant);

        if (request.categoryId() != null) {
            Category category = categoryRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(request.categoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
            product.setCategory(category);
        }

        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        product.setUpdatedBy(currentUser);

        product = productRepository.save(product);

        return ProductMapper.toResponse(product, List.of(), 0);
    }

    /**
     * Updates mutable fields of a product.
     * trackingType is NOT updated (structural invariant).
     * Validates SKU uniqueness if the SKU changed (excludes current product).
     */
    @Transactional
    public ProductResponse update(UUID id, ProductRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Product product = productRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        // Check SKU conflict only if SKU actually changed
        if (!product.getSku().equals(request.sku())) {
            productRepository
                    .findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, request.sku())
                    .ifPresent(conflict -> {
                        throw new ConflictException("Un produit actif avec le SKU '" + request.sku() + "' existe déjà");
                    });
        } else {
            // SKU not changed — still verify no other product owns it (edge-case safety)
            productRepository
                    .findByTenantIdAndSkuAndDeletedAtIsNull(tenantId, request.sku())
                    .ifPresent(found -> {
                        if (!found.getId().equals(id)) {
                            throw new ConflictException("Un produit actif avec le SKU '" + request.sku() + "' existe déjà");
                        }
                    });
        }

        ProductMapper.updateEntity(product, request);

        if (request.categoryId() != null) {
            Category category = categoryRepository
                    .findByIdAndTenantIdAndDeletedAtIsNull(request.categoryId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Catégorie introuvable"));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        User currentUser = userRepository.getReferenceById(securityHelper.getCurrentUserId());
        product.setUpdatedBy(currentUser);

        product = productRepository.save(product);

        List<ProductStock> stocks = productStockRepository.findByProductId(id);
        int total = productStockRepository.sumQuantityByProductId(id);

        return ProductMapper.toResponse(product, stocks, total);
    }

    /**
     * Soft-deletes a product by setting deletedAt = now().
     * 404 if not found or already deleted.
     */
    @Transactional
    public void softDelete(UUID id) {
        UUID tenantId = securityHelper.getCurrentTenantId();

        Product product = productRepository
                .findByTenantIdAndIdAndDeletedAtIsNull(tenantId, id)
                .orElseThrow(() -> new ResourceNotFoundException("Produit introuvable"));

        product.setDeletedAt(Instant.now());
        productRepository.save(product);
    }

    /**
     * Returns all QUANTITY products where the total stock is below minQuantity.
     * Skips products where minQuantity is null.
     */
    public List<ProductResponse> getLowStock() {
        UUID tenantId = securityHelper.getCurrentTenantId();

        List<Product> quantityProducts = productRepository
                .findByTenantIdAndTrackingTypeAndDeletedAtIsNull(tenantId, TrackingType.QUANTITY);

        return quantityProducts.stream()
                .filter(p -> p.getMinQuantity() != null)
                .filter(p -> {
                    int total = productStockRepository.sumQuantityByProductId(p.getId());
                    return total < p.getMinQuantity();
                })
                .map(p -> {
                    List<ProductStock> stocks = productStockRepository.findByProductId(p.getId());
                    int total = productStockRepository.sumQuantityByProductId(p.getId());
                    return ProductMapper.toResponse(p, stocks, total);
                })
                .toList();
    }
}
