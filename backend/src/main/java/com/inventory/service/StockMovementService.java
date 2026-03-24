package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.mapper.StockMovementMapper;
import com.inventory.model.dto.MovementRequest;
import com.inventory.model.dto.MovementResponse;
import com.inventory.model.entity.*;
import com.inventory.model.enums.MovementType;
import com.inventory.model.enums.TrackingType;
import com.inventory.repository.*;
import com.inventory.security.SecurityHelper;
import com.inventory.specification.StockMovementSpecification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class StockMovementService {

    private final StockMovementRepository movementRepository;
    private final ProductStockRepository productStockRepository;
    private final ProductRepository productRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final SecurityHelper securityHelper;

    public StockMovementService(
            StockMovementRepository movementRepository,
            ProductStockRepository productStockRepository,
            ProductRepository productRepository,
            RoomRepository roomRepository,
            UserRepository userRepository,
            SecurityHelper securityHelper
    ) {
        this.movementRepository = movementRepository;
        this.productStockRepository = productStockRepository;
        this.productRepository = productRepository;
        this.roomRepository = roomRepository;
        this.userRepository = userRepository;
        this.securityHelper = securityHelper;
    }

    @Transactional
    public MovementResponse recordMovement(MovementRequest request) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        UUID userId = securityHelper.getCurrentUserId();

        Product product = productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        if (product.getTrackingType() == TrackingType.UNIQUE && request.quantity() != 1) {
            throw new IllegalArgumentException("Quantity must be 1 for UNIQUE products");
        }

        Room fromRoom = null;
        Room toRoom = null;

        if (request.fromRoomId() != null) {
            fromRoom = roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.fromRoomId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Source room not found"));
        }
        if (request.toRoomId() != null) {
            toRoom = roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.toRoomId(), tenantId)
                    .orElseThrow(() -> new ResourceNotFoundException("Destination room not found"));
        }

        switch (request.type()) {
            case IN -> applyIN(tenantId, product, toRoom, request.quantity());
            case OUT -> applyOUT(tenantId, product, fromRoom, request.quantity());
            case TRANSFER -> applyTRANSFER(tenantId, product, fromRoom, toRoom, request.quantity());
        }

        User performedBy = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        StockMovement movement = StockMovement.builder()
                .tenant(product.getTenant())
                .product(product)
                .fromRoom(fromRoom)
                .toRoom(toRoom)
                .quantity(request.quantity())
                .type(request.type())
                .reason(request.reason())
                .performedBy(performedBy)
                .build();

        return StockMovementMapper.toResponse(movementRepository.save(movement));
    }

    public Page<MovementResponse> getHistory(int page, int size, UUID productId, MovementType type,
                                             Instant fromDate, Instant toDate, UUID performedBy) {
        if (size > 100) {
            throw new IllegalArgumentException("Page size cannot exceed 100");
        }
        UUID tenantId = securityHelper.getCurrentTenantId();
        Specification<StockMovement> spec = StockMovementSpecification.build(
                tenantId, productId, type, fromDate, toDate, performedBy);
        return movementRepository.findAll(spec, PageRequest.of(page, size))
                .map(StockMovementMapper::toResponse);
    }

    public Page<MovementResponse> getHistoryByProduct(UUID productId, int page, int size) {
        UUID tenantId = securityHelper.getCurrentTenantId();
        productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
        return movementRepository.findByTenantIdAndProductId(tenantId, productId, PageRequest.of(page, size))
                .map(StockMovementMapper::toResponse);
    }

    // ─── Private helpers ─────────────────────────────────────────

    private void applyIN(UUID tenantId, Product product, Room toRoom, int quantity) {
        Optional<ProductStock> existing = productStockRepository
                .findByTenantIdAndProductIdAndRoomId(tenantId, product.getId(), toRoom.getId());
        if (existing.isPresent()) {
            ProductStock stock = existing.get();
            stock.setQuantity(stock.getQuantity() + quantity);
            productStockRepository.save(stock);
        } else {
            ProductStock newStock = new ProductStock();
            newStock.setTenant(product.getTenant());
            newStock.setProduct(product);
            newStock.setRoom(toRoom);
            newStock.setQuantity(quantity);
            productStockRepository.save(newStock);
        }
    }

    private void applyOUT(UUID tenantId, Product product, Room fromRoom, int quantity) {
        ProductStock stock = productStockRepository
                .findByTenantIdAndProductIdAndRoomId(tenantId, product.getId(), fromRoom.getId())
                .orElseThrow(() -> new ConflictException("No stock available for this product in this room"));
        if (stock.getQuantity() < quantity) {
            throw new ConflictException(
                    "Insufficient stock: available " + stock.getQuantity() + ", requested " + quantity);
        }
        if (stock.getQuantity() == quantity) {
            productStockRepository.delete(stock);
        } else {
            stock.setQuantity(stock.getQuantity() - quantity);
            productStockRepository.save(stock);
        }
    }

    private void applyTRANSFER(UUID tenantId, Product product, Room fromRoom, Room toRoom, int quantity) {
        ProductStock fromStock = productStockRepository
                .findByTenantIdAndProductIdAndRoomId(tenantId, product.getId(), fromRoom.getId())
                .orElseThrow(() -> new ConflictException("No stock available for this product in source room"));
        if (fromStock.getQuantity() < quantity) {
            throw new ConflictException(
                    "Insufficient stock: available " + fromStock.getQuantity() + ", requested " + quantity);
        }
        if (fromStock.getQuantity() == quantity) {
            productStockRepository.delete(fromStock);
        } else {
            fromStock.setQuantity(fromStock.getQuantity() - quantity);
            productStockRepository.save(fromStock);
        }
        applyIN(tenantId, product, toRoom, quantity);
    }
}
