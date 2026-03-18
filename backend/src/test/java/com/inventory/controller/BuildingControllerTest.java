package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.SecurityConfig;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.BuildingRequest;
import com.inventory.model.dto.BuildingResponse;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.BuildingService;
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

@WebMvcTest(BuildingController.class)
@Import(SecurityConfig.class)
class BuildingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private BuildingService buildingService;

    // Requis par JwtAuthFilter dans la chaîne de sécurité
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private UUID buildingId;
    private BuildingResponse mockBuilding;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        mockBuilding = new BuildingResponse(
                buildingId, "Entrepôt A", "10 rue Test", Instant.now(), Instant.now()
        );
    }

    // ─── GET /api/buildings ───────────────────────────────────────

    @Test
    @WithMockUser
    void getAll_authenticated_shouldReturn200() throws Exception {
        when(buildingService.getAll(false)).thenReturn(List.of(mockBuilding));

        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Entrepôt A"));
    }

    @Test
    @WithMockUser
    void getAll_archived_authenticated_shouldReturn200() throws Exception {
        BuildingResponse archived = new BuildingResponse(
                UUID.randomUUID(), "Entrepôt Archivé", "5 rue Ancienne", Instant.now(), Instant.now()
        );
        when(buildingService.getAll(true)).thenReturn(List.of(archived));

        mockMvc.perform(get("/api/buildings").param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Entrepôt Archivé"));
    }

    @Test
    void getAll_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).getAll(anyBoolean());
    }

    // ─── GET /api/buildings/{id} ──────────────────────────────────

    @Test
    @WithMockUser
    void getById_shouldReturn200() throws Exception {
        when(buildingService.getById(buildingId)).thenReturn(mockBuilding);

        mockMvc.perform(get("/api/buildings/{id}", buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Entrepôt A"))
                .andExpect(jsonPath("$.data.id").value(buildingId.toString()));
    }

    @Test
    @WithMockUser
    void getById_notFound_shouldReturn404() throws Exception {
        when(buildingService.getById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Bâtiment introuvable"));

        mockMvc.perform(get("/api/buildings/{id}", buildingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Bâtiment introuvable"));
    }

    @Test
    void getById_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/buildings/{id}", buildingId))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/buildings ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_shouldReturn201() throws Exception {
        BuildingRequest request = new BuildingRequest("Nouvel Entrepôt", "1 avenue Test");
        when(buildingService.create(any(BuildingRequest.class))).thenReturn(mockBuilding);

        mockMvc.perform(post("/api/buildings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Entrepôt A"));

        verify(buildingService, times(1)).create(any(BuildingRequest.class));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_asManager_shouldReturn403() throws Exception {
        BuildingRequest request = new BuildingRequest("Nouvel Entrepôt", "1 avenue Test");

        mockMvc.perform(post("/api/buildings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "READER")
    void create_asReader_shouldReturn403() throws Exception {
        BuildingRequest request = new BuildingRequest("Nouvel Entrepôt", "1 avenue Test");

        mockMvc.perform(post("/api/buildings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).create(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_shouldReturn400() throws Exception {
        BuildingRequest request = new BuildingRequest("", "1 avenue Test");

        mockMvc.perform(post("/api/buildings")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(buildingService, never()).create(any());
    }

    // ─── PUT /api/buildings/{id} ──────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_shouldReturn200() throws Exception {
        BuildingRequest request = new BuildingRequest("Entrepôt Modifié", "2 rue Modifiée");
        BuildingResponse updated = new BuildingResponse(
                buildingId, "Entrepôt Modifié", "2 rue Modifiée", Instant.now(), Instant.now()
        );
        when(buildingService.update(eq(buildingId), any(BuildingRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/buildings/{id}", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Entrepôt Modifié"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_asManager_shouldReturn403() throws Exception {
        BuildingRequest request = new BuildingRequest("Entrepôt Modifié", "2 rue Modifiée");

        mockMvc.perform(put("/api/buildings/{id}", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_shouldReturn404() throws Exception {
        BuildingRequest request = new BuildingRequest("Entrepôt Modifié", "2 rue Modifiée");
        when(buildingService.update(eq(buildingId), any(BuildingRequest.class)))
                .thenThrow(new ResourceNotFoundException("Bâtiment introuvable"));

        mockMvc.perform(put("/api/buildings/{id}", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankName_shouldReturn400() throws Exception {
        BuildingRequest request = new BuildingRequest("", "2 rue Modifiée");

        mockMvc.perform(put("/api/buildings/{id}", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(buildingService, never()).update(any(), any());
    }

    // ─── DELETE /api/buildings/{id} ───────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/buildings/{id}", buildingId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Bâtiment archivé"));

        verify(buildingService, times(1)).delete(buildingId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_asManager_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/buildings/{id}", buildingId).with(csrf()))
                .andExpect(status().isForbidden());

        verify(buildingService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_notFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Bâtiment introuvable"))
                .when(buildingService).delete(buildingId);

        mockMvc.perform(delete("/api/buildings/{id}", buildingId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
