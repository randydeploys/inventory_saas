package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.SecurityConfig;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.MovementRequest;
import com.inventory.model.dto.MovementResponse;
import com.inventory.model.enums.MovementType;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.StockMovementService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(StockMovementController.class)
@Import(SecurityConfig.class)
class StockMovementControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private StockMovementService stockMovementService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private UUID productId;
    private UUID toRoomId;
    private UUID fromRoomId;
    private MovementResponse mockMovement;
    private MovementRequest validInRequest;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        toRoomId = UUID.randomUUID();
        fromRoomId = UUID.randomUUID();

        mockMovement = new MovementResponse(
                UUID.randomUUID(), productId, "Clé USB", "USB-001",
                MovementType.IN, 5,
                null, null, toRoomId, "Zone B",
                null, UUID.randomUUID(), "Alice Dupont", Instant.now()
        );

        validInRequest = new MovementRequest(productId, MovementType.IN, 5, null, toRoomId, null);
    }

    // ─── GET /api/movements ──────────────────────────────────────

    @Test
    @WithMockUser
    void getHistory_authenticated_shouldReturn200() throws Exception {
        Page<MovementResponse> page = new PageImpl<>(List.of(mockMovement));
        when(stockMovementService.getHistory(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].type").value("IN"))
                .andExpect(jsonPath("$.data.content[0].quantity").value(5));
    }

    @Test
    void getHistory_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/movements"))
                .andExpect(status().isForbidden());

        verify(stockMovementService, never()).getHistory(anyInt(), anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void getHistory_sizeTooLarge_shouldReturn400() throws Exception {
        when(stockMovementService.getHistory(anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Page size cannot exceed 100"));

        mockMvc.perform(get("/api/movements").param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── POST /api/movements ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void recordMovement_asAdmin_shouldReturn201() throws Exception {
        when(stockMovementService.recordMovement(any(MovementRequest.class))).thenReturn(mockMovement);

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.type").value("IN"))
                .andExpect(jsonPath("$.data.quantity").value(5));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void recordMovement_asManager_shouldReturn201() throws Exception {
        when(stockMovementService.recordMovement(any(MovementRequest.class))).thenReturn(mockMovement);

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "READER")
    void recordMovement_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInRequest)))
                .andExpect(status().isForbidden());

        verify(stockMovementService, never()).recordMovement(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recordMovement_withNullProductId_shouldReturn400() throws Exception {
        // productId est @NotNull dans MovementRequest
        MovementRequest bad = new MovementRequest(null, MovementType.IN, 5, null, toRoomId, null);

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(stockMovementService, never()).recordMovement(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recordMovement_invalidMovementConstraint_shouldReturn400() throws Exception {
        // IN avec fromRoomId renseigné → violation @ValidMovement
        MovementRequest bad = new MovementRequest(productId, MovementType.IN, 5, fromRoomId, toRoomId, null);

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(stockMovementService, never()).recordMovement(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recordMovement_withInsufficientStock_shouldReturn409() throws Exception {
        when(stockMovementService.recordMovement(any(MovementRequest.class)))
                .thenThrow(new ConflictException("Insufficient stock: available 2, requested 5"));

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void recordMovement_withInvalidProduct_shouldReturn404() throws Exception {
        when(stockMovementService.recordMovement(any(MovementRequest.class)))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(post("/api/movements")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validInRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Product not found"));
    }

    // ─── GET /api/products/{id}/movements ────────────────────────

    @Test
    @WithMockUser
    void getHistoryByProduct_shouldReturn200() throws Exception {
        Page<MovementResponse> page = new PageImpl<>(List.of(mockMovement));
        when(stockMovementService.getHistoryByProduct(eq(productId), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products/{id}/movements", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.content[0].type").value("IN"));
    }

    @Test
    @WithMockUser
    void getHistoryByProduct_productNotFound_shouldReturn404() throws Exception {
        when(stockMovementService.getHistoryByProduct(any(UUID.class), anyInt(), anyInt()))
                .thenThrow(new ResourceNotFoundException("Product not found"));

        mockMvc.perform(get("/api/products/{id}/movements", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void getHistoryByProduct_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/products/{id}/movements", productId))
                .andExpect(status().isForbidden());
    }
}
