package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.SecurityConfig;
import com.inventory.exception.ConflictException;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.ProductRequest;
import com.inventory.model.dto.ProductResponse;
import com.inventory.model.enums.TrackingType;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.ProductService;
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

@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private ProductService productService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private UUID productId;
    private ProductResponse mockProduct;
    private ProductRequest validRequest;

    @BeforeEach
    void setUp() {
        productId = UUID.randomUUID();
        mockProduct = new ProductResponse(
                productId, "Clé USB", "USB-001", null,
                TrackingType.QUANTITY, null, null, null,
                10, "pièces", 0, List.of(), Instant.now(), Instant.now()
        );
        validRequest = new ProductRequest("Clé USB", "USB-001", null, TrackingType.QUANTITY,
                null, null, 10, "pièces");
    }

    // ─── GET /api/products ───────────────────────────────────────

    @Test
    @WithMockUser
    void getAll_authenticated_shouldReturn200() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(mockProduct));
        when(productService.getAll(anyBoolean(), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].name").value("Clé USB"))
                .andExpect(jsonPath("$.data.content[0].sku").value("USB-001"));
    }

    @Test
    void getAll_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/products"))
                .andExpect(status().isForbidden());

        verify(productService, never()).getAll(anyBoolean(), anyInt(), anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void getAll_withArchivedFlag_shouldPassToService() throws Exception {
        Page<ProductResponse> page = new PageImpl<>(List.of(mockProduct));
        when(productService.getAll(eq(true), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenReturn(page);

        mockMvc.perform(get("/api/products").param("archived", "true"))
                .andExpect(status().isOk());

        verify(productService).getAll(eq(true), anyInt(), anyInt(), any(), any(), any(), any(), any());
    }

    @Test
    @WithMockUser
    void getAll_sizeTooLarge_shouldReturn400() throws Exception {
        when(productService.getAll(anyBoolean(), anyInt(), anyInt(), any(), any(), any(), any(), any()))
                .thenThrow(new IllegalArgumentException("Page size must not exceed 100"));

        mockMvc.perform(get("/api/products").param("size", "200"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── GET /api/products/low-stock ─────────────────────────────

    @Test
    @WithMockUser
    void getLowStock_authenticated_shouldReturn200() throws Exception {
        when(productService.getLowStock()).thenReturn(List.of(mockProduct));

        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Clé USB"));
    }

    @Test
    void getLowStock_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/products/low-stock"))
                .andExpect(status().isForbidden());
    }

    // ─── GET /api/products/{id} ──────────────────────────────────

    @Test
    @WithMockUser
    void getById_shouldReturn200() throws Exception {
        when(productService.getById(productId)).thenReturn(mockProduct);

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Clé USB"))
                .andExpect(jsonPath("$.data.sku").value("USB-001"))
                .andExpect(jsonPath("$.data.trackingType").value("QUANTITY"));
    }

    @Test
    @WithMockUser
    void getById_notFound_shouldReturn404() throws Exception {
        when(productService.getById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Produit introuvable"));

        mockMvc.perform(get("/api/products/{id}", productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Produit introuvable"));
    }

    // ─── POST /api/products ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_shouldReturn201() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(mockProduct);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Clé USB"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_asManager_shouldReturn201() throws Exception {
        when(productService.create(any(ProductRequest.class))).thenReturn(mockProduct);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "READER")
    void create_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(productService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_shouldReturn400() throws Exception {
        ProductRequest bad = new ProductRequest("", "USB-001", null, TrackingType.QUANTITY,
                null, null, 10, "pièces");

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withNullTrackingType_shouldReturn400() throws Exception {
        ProductRequest bad = new ProductRequest("Clé USB", "USB-001", null, null,
                null, null, null, null);

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(bad)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(productService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_duplicateSku_shouldReturn409() throws Exception {
        when(productService.create(any(ProductRequest.class)))
                .thenThrow(new ConflictException("Un produit actif avec le SKU 'USB-001' existe déjà"));

        mockMvc.perform(post("/api/products")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── PUT /api/products/{id} ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_shouldReturn200() throws Exception {
        when(productService.update(eq(productId), any(ProductRequest.class))).thenReturn(mockProduct);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Clé USB"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_asManager_shouldReturn200() throws Exception {
        when(productService.update(eq(productId), any(ProductRequest.class))).thenReturn(mockProduct);

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "READER")
    void update_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());

        verify(productService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_shouldReturn404() throws Exception {
        when(productService.update(eq(productId), any(ProductRequest.class)))
                .thenThrow(new ResourceNotFoundException("Produit introuvable"));

        mockMvc.perform(put("/api/products/{id}", productId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/products/{id} ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void softDelete_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", productId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Produit archivé"));

        verify(productService, times(1)).softDelete(productId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void softDelete_asManager_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", productId).with(csrf()))
                .andExpect(status().isOk());

        verify(productService, times(1)).softDelete(productId);
    }

    @Test
    @WithMockUser(roles = "READER")
    void softDelete_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/products/{id}", productId).with(csrf()))
                .andExpect(status().isForbidden());

        verify(productService, never()).softDelete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void softDelete_notFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Produit introuvable"))
                .when(productService).softDelete(productId);

        mockMvc.perform(delete("/api/products/{id}", productId).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
