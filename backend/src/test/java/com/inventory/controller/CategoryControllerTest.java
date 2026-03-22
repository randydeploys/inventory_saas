package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.SecurityConfig;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.CategoryRequest;
import com.inventory.model.dto.CategoryResponse;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private CategoryService categoryService;
    @MockitoBean private JwtTokenProvider jwtTokenProvider;

    private UUID categoryId;
    private CategoryResponse mockCategory;

    @BeforeEach
    void setUp() {
        categoryId = UUID.randomUUID();
        mockCategory = new CategoryResponse(categoryId, "Électronique", "#3498DB", Instant.now(), Instant.now());
    }

    // ─── GET /api/categories ──────────────────────────────────────

    @Test
    @WithMockUser
    void getAll_authenticated_shouldReturn200() throws Exception {
        when(categoryService.getAll(false)).thenReturn(List.of(mockCategory));

        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Électronique"))
                .andExpect(jsonPath("$.data[0].color").value("#3498DB"));
    }

    @Test
    @WithMockUser
    void getAll_archived_shouldReturn200() throws Exception {
        CategoryResponse archived = new CategoryResponse(UUID.randomUUID(), "Ancien stock", "#999999", Instant.now(), Instant.now());
        when(categoryService.getAll(true)).thenReturn(List.of(archived));

        mockMvc.perform(get("/api/categories").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Ancien stock"));
    }

    @Test
    void getAll_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).getAll(anyBoolean());
    }

    // ─── GET /api/categories/{id} ─────────────────────────────────

    @Test
    @WithMockUser
    void getById_shouldReturn200() throws Exception {
        when(categoryService.getById(categoryId)).thenReturn(mockCategory);

        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Électronique"))
                .andExpect(jsonPath("$.data.color").value("#3498DB"));
    }

    @Test
    @WithMockUser
    void getById_notFound_shouldReturn404() throws Exception {
        when(categoryService.getById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Catégorie introuvable"));

        mockMvc.perform(get("/api/categories/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Catégorie introuvable"));
    }

    // ─── POST /api/categories ─────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_shouldReturn201() throws Exception {
        CategoryRequest request = new CategoryRequest("Électronique", "#3498DB");
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(mockCategory);

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.name").value("Électronique"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_asManager_shouldReturn201() throws Exception {
        CategoryRequest request = new CategoryRequest("Mobilier", "#27AE60");
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(mockCategory);

        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());
    }

    @Test
    @WithMockUser(roles = "READER")
    void create_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("X", "#000000"))))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("", "#FF5733"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(categoryService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withInvalidColor_shouldReturn400() throws Exception {
        // "rouge" n'est pas un code hex valide
        mockMvc.perform(post("/api/categories")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("Mobilier", "rouge"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(categoryService, never()).create(any());
    }

    // ─── PUT /api/categories/{id} ─────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_shouldReturn200() throws Exception {
        CategoryRequest request = new CategoryRequest("Modifié", "#E74C3C");
        CategoryResponse updated = new CategoryResponse(categoryId, "Modifié", "#E74C3C", Instant.now(), Instant.now());
        when(categoryService.update(eq(categoryId), any(CategoryRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Modifié"))
                .andExpect(jsonPath("$.data.color").value("#E74C3C"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_asManager_shouldReturn200() throws Exception {
        CategoryRequest request = new CategoryRequest("Modifié", "#E74C3C");
        when(categoryService.update(eq(categoryId), any(CategoryRequest.class))).thenReturn(mockCategory);

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = "READER")
    void update_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("X", "#000000"))))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_shouldReturn404() throws Exception {
        when(categoryService.update(eq(categoryId), any(CategoryRequest.class)))
                .thenThrow(new ResourceNotFoundException("Catégorie introuvable"));

        mockMvc.perform(put("/api/categories/{id}", categoryId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CategoryRequest("X", "#000000"))))
                .andExpect(status().isNotFound());
    }

    // ─── DELETE /api/categories/{id} ──────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Catégorie archivée"));

        verify(categoryService, times(1)).delete(categoryId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_asManager_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(csrf()))
                .andExpect(status().isOk());

        verify(categoryService, times(1)).delete(categoryId);
    }

    @Test
    @WithMockUser(roles = "READER")
    void delete_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(csrf()))
                .andExpect(status().isForbidden());

        verify(categoryService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_notFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Catégorie introuvable"))
                .when(categoryService).delete(categoryId);

        mockMvc.perform(delete("/api/categories/{id}", categoryId).with(csrf()))
                .andExpect(status().isNotFound());
    }
}
