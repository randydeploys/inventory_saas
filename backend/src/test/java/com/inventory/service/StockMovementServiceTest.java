package com.inventory.service;

import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.MovementRequest;
import com.inventory.model.dto.MovementResponse;
import com.inventory.model.entity.*;
import com.inventory.model.enums.MovementType;
import com.inventory.model.enums.TrackingType;
import com.inventory.repository.*;
import com.inventory.security.SecurityHelper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock private StockMovementRepository movementRepository;
    @Mock private ProductStockRepository productStockRepository;
    @Mock private ProductRepository productRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private UserRepository userRepository;
    @Mock private SecurityHelper securityHelper;

    @InjectMocks
    private StockMovementService stockMovementService;

    private UUID tenantId;
    private UUID userId;
    private Tenant tenant;
    private User user;
    private Product quantityProduct;
    private Product uniqueProduct;
    private Room fromRoom;
    private Room toRoom;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        userId = UUID.randomUUID();

        tenant = new Tenant();
        tenant.setId(tenantId);
        tenant.setName("Test Corp");

        user = new User();
        user.setId(userId);
        user.setFirstName("Alice");
        user.setLastName("Dupont");

        quantityProduct = new Product();
        quantityProduct.setId(UUID.randomUUID());
        quantityProduct.setName("Clé USB");
        quantityProduct.setSku("USB-001");
        quantityProduct.setTrackingType(TrackingType.QUANTITY);
        quantityProduct.setTenant(tenant);

        uniqueProduct = new Product();
        uniqueProduct.setId(UUID.randomUUID());
        uniqueProduct.setName("Laptop");
        uniqueProduct.setSku("LAP-001");
        uniqueProduct.setTrackingType(TrackingType.UNIQUE);
        uniqueProduct.setTenant(tenant);

        fromRoom = new Room();
        fromRoom.setId(UUID.randomUUID());
        fromRoom.setName("Zone A");
        fromRoom.setTenant(tenant);

        toRoom = new Room();
        toRoom.setId(UUID.randomUUID());
        toRoom.setName("Zone B");
        toRoom.setTenant(tenant);

        // lenient pour éviter UnnecessaryStubbingException sur les tests d'erreur précoce
        lenient().when(securityHelper.getCurrentTenantId()).thenReturn(tenantId);
        lenient().when(securityHelper.getCurrentUserId()).thenReturn(userId);
    }

    // ─── recordMovement — IN ─────────────────────────────────────

    @Test
    void recordMovement_IN_newStock_shouldCreateProductStock() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.IN, 5, null, toRoom.getId(), null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(toRoom.getId(), tenantId))
                .thenReturn(Optional.of(toRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), toRoom.getId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, null, toRoom, 5, MovementType.IN));

        MovementResponse response = stockMovementService.recordMovement(request);

        ArgumentCaptor<ProductStock> stockCaptor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockRepository).save(stockCaptor.capture());
        assertEquals(5, stockCaptor.getValue().getQuantity());
        assertEquals(toRoom, stockCaptor.getValue().getRoom());
        assertNotNull(response);
        assertEquals(MovementType.IN, response.type());
    }

    @Test
    void recordMovement_IN_existingStock_shouldIncrementQuantity() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.IN, 3, null, toRoom.getId(), null);
        ProductStock existingStock = buildStock(quantityProduct, toRoom, 10);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(toRoom.getId(), tenantId))
                .thenReturn(Optional.of(toRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), toRoom.getId()))
                .thenReturn(Optional.of(existingStock));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, null, toRoom, 3, MovementType.IN));

        stockMovementService.recordMovement(request);

        assertEquals(13, existingStock.getQuantity());
        verify(productStockRepository).save(existingStock);
    }

    @Test
    void recordMovement_IN_withInvalidProduct_shouldThrowNotFoundException() {
        var request = new MovementRequest(UUID.randomUUID(), MovementType.IN, 1, null, toRoom.getId(), null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, request.productId()))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockMovementService.recordMovement(request));
        verify(productStockRepository, never()).save(any());
    }

    @Test
    void recordMovement_IN_withInvalidToRoom_shouldThrowNotFoundException() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.IN, 1, null, UUID.randomUUID(), null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.toRoomId(), tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockMovementService.recordMovement(request));
        verify(productStockRepository, never()).save(any());
    }

    @Test
    void recordMovement_IN_uniqueProduct_quantityNotOne_shouldThrowIllegalArgumentException() {
        var request = new MovementRequest(uniqueProduct.getId(), MovementType.IN, 2, null, toRoom.getId(), null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, uniqueProduct.getId()))
                .thenReturn(Optional.of(uniqueProduct));

        assertThrows(IllegalArgumentException.class, () -> stockMovementService.recordMovement(request));
        verify(productStockRepository, never()).save(any());
    }

    // ─── recordMovement — OUT ────────────────────────────────────

    @Test
    void recordMovement_OUT_withSufficientStock_shouldDecrementProductStock() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.OUT, 3, fromRoom.getId(), null, "Livraison");
        ProductStock stock = buildStock(quantityProduct, fromRoom, 10);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(stock));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, fromRoom, null, 3, MovementType.OUT));

        stockMovementService.recordMovement(request);

        assertEquals(7, stock.getQuantity());
        verify(productStockRepository).save(stock);
        verify(productStockRepository, never()).delete(any());
    }

    @Test
    void recordMovement_OUT_withExactStock_shouldDeleteProductStock() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.OUT, 5, fromRoom.getId(), null, null);
        ProductStock stock = buildStock(quantityProduct, fromRoom, 5);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(stock));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, fromRoom, null, 5, MovementType.OUT));

        stockMovementService.recordMovement(request);

        verify(productStockRepository).delete(stock);
        verify(productStockRepository, never()).save(any(ProductStock.class));
    }

    @Test
    void recordMovement_OUT_withInsufficientStock_shouldThrowConflictException() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.OUT, 10, fromRoom.getId(), null, null);
        ProductStock stock = buildStock(quantityProduct, fromRoom, 3);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(stock));

        assertThrows(ConflictException.class, () -> stockMovementService.recordMovement(request));
        verify(movementRepository, never()).save(any());
    }

    @Test
    void recordMovement_OUT_withNoStock_shouldThrowConflictException() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.OUT, 1, fromRoom.getId(), null, null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.empty());

        assertThrows(ConflictException.class, () -> stockMovementService.recordMovement(request));
    }

    @Test
    void recordMovement_OUT_withInvalidFromRoom_shouldThrowNotFoundException() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.OUT, 1, UUID.randomUUID(), null, null);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(request.fromRoomId(), tenantId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> stockMovementService.recordMovement(request));
    }

    // ─── recordMovement — TRANSFER ───────────────────────────────

    @Test
    void recordMovement_TRANSFER_shouldDecrementFromAndIncrementTo() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.TRANSFER, 4, fromRoom.getId(), toRoom.getId(), null);
        ProductStock fromStock = buildStock(quantityProduct, fromRoom, 10);
        ProductStock toStock = buildStock(quantityProduct, toRoom, 2);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(toRoom.getId(), tenantId))
                .thenReturn(Optional.of(toRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(fromStock));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), toRoom.getId()))
                .thenReturn(Optional.of(toStock));
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, fromRoom, toRoom, 4, MovementType.TRANSFER));

        stockMovementService.recordMovement(request);

        assertEquals(6, fromStock.getQuantity());
        assertEquals(6, toStock.getQuantity());
    }

    @Test
    void recordMovement_TRANSFER_fromStockBecomesZero_shouldDeleteFromStockAndCreateToStock() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.TRANSFER, 5, fromRoom.getId(), toRoom.getId(), null);
        ProductStock fromStock = buildStock(quantityProduct, fromRoom, 5);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(toRoom.getId(), tenantId))
                .thenReturn(Optional.of(toRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(fromStock));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), toRoom.getId()))
                .thenReturn(Optional.empty());
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(movementRepository.save(any())).thenReturn(buildMovement(quantityProduct, fromRoom, toRoom, 5, MovementType.TRANSFER));

        stockMovementService.recordMovement(request);

        verify(productStockRepository).delete(fromStock);
        ArgumentCaptor<ProductStock> captor = ArgumentCaptor.forClass(ProductStock.class);
        verify(productStockRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getQuantity());
        assertEquals(toRoom, captor.getValue().getRoom());
    }

    @Test
    void recordMovement_TRANSFER_withInsufficientFromStock_shouldThrowConflictException() {
        var request = new MovementRequest(quantityProduct.getId(), MovementType.TRANSFER, 10, fromRoom.getId(), toRoom.getId(), null);
        ProductStock fromStock = buildStock(quantityProduct, fromRoom, 3);

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(fromRoom.getId(), tenantId))
                .thenReturn(Optional.of(fromRoom));
        when(roomRepository.findByIdAndTenantIdAndDeletedAtIsNull(toRoom.getId(), tenantId))
                .thenReturn(Optional.of(toRoom));
        when(productStockRepository.findByTenantIdAndProductIdAndRoomId(tenantId, quantityProduct.getId(), fromRoom.getId()))
                .thenReturn(Optional.of(fromStock));

        assertThrows(ConflictException.class, () -> stockMovementService.recordMovement(request));
        verify(movementRepository, never()).save(any());
    }

    // ─── getHistory ──────────────────────────────────────────────

    @Test
    void getHistory_shouldReturnPagedMovements() {
        StockMovement movement = buildMovement(quantityProduct, fromRoom, null, 2, MovementType.OUT);
        Page<StockMovement> page = new PageImpl<>(List.of(movement));

        when(movementRepository.findAll(any(Specification.class), any(PageRequest.class))).thenReturn(page);

        Page<MovementResponse> result = stockMovementService.getHistory(0, 20, null, null, null, null, null);

        assertEquals(1, result.getTotalElements());
        assertEquals(MovementType.OUT, result.getContent().get(0).type());
        assertEquals(quantityProduct.getId(), result.getContent().get(0).productId());
    }

    @Test
    void getHistory_withSizeTooLarge_shouldThrowIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class,
                () -> stockMovementService.getHistory(0, 101, null, null, null, null, null));
    }

    // ─── getHistoryByProduct ─────────────────────────────────────

    @Test
    void getHistoryByProduct_shouldReturnProductMovements() {
        StockMovement movement = buildMovement(quantityProduct, fromRoom, null, 2, MovementType.OUT);
        Page<StockMovement> page = new PageImpl<>(List.of(movement));

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quantityProduct.getId()))
                .thenReturn(Optional.of(quantityProduct));
        when(movementRepository.findByTenantIdAndProductId(tenantId, quantityProduct.getId(), PageRequest.of(0, 20)))
                .thenReturn(page);

        Page<MovementResponse> result = stockMovementService.getHistoryByProduct(quantityProduct.getId(), 0, 20);

        assertEquals(1, result.getTotalElements());
        assertEquals(quantityProduct.getId(), result.getContent().get(0).productId());
    }

    @Test
    void getHistoryByProduct_withInvalidProduct_shouldThrowNotFoundException() {
        UUID unknownId = UUID.randomUUID();

        when(productRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, unknownId))
                .thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> stockMovementService.getHistoryByProduct(unknownId, 0, 20));
    }

    // ─── Helpers ─────────────────────────────────────────────────

    private ProductStock buildStock(Product product, Room room, int qty) {
        ProductStock stock = new ProductStock();
        stock.setId(UUID.randomUUID());
        stock.setProduct(product);
        stock.setRoom(room);
        stock.setTenant(tenant);
        stock.setQuantity(qty);
        return stock;
    }

    private StockMovement buildMovement(Product product, Room from, Room to, int qty, MovementType type) {
        StockMovement movement = new StockMovement();
        movement.setId(UUID.randomUUID());
        movement.setProduct(product);
        movement.setFromRoom(from);
        movement.setToRoom(to);
        movement.setQuantity(qty);
        movement.setType(type);
        movement.setTenant(tenant);
        movement.setPerformedBy(user);
        movement.setCreatedAt(Instant.now());
        return movement;
    }
}
