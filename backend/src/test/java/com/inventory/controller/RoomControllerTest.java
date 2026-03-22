package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.SecurityConfig;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.RoomRequest;
import com.inventory.model.dto.RoomResponse;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.RoomService;
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

@WebMvcTest(RoomController.class)
@Import(SecurityConfig.class)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RoomService roomService;

    // Requis par JwtAuthFilter dans la chaîne de sécurité
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private UUID buildingId;
    private UUID roomId;
    private RoomResponse mockRoom;

    @BeforeEach
    void setUp() {
        buildingId = UUID.randomUUID();
        roomId = UUID.randomUUID();
        mockRoom = new RoomResponse(
                roomId, buildingId, "Zone A", "Stockage palettes", Instant.now(), Instant.now()
        );
    }

    // ─── GET /api/buildings/{buildingId}/rooms ────────────────────

    @Test
    @WithMockUser
    void getAllByBuilding_authenticated_shouldReturn200() throws Exception {
        when(roomService.getAllByBuilding(buildingId, false)).thenReturn(List.of(mockRoom));

        mockMvc.perform(get("/api/buildings/{buildingId}/rooms", buildingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].name").value("Zone A"));
    }

    @Test
    @WithMockUser
    void getAllByBuilding_archived_shouldReturn200() throws Exception {
        RoomResponse archived = new RoomResponse(
                UUID.randomUUID(), buildingId, "Zone Archivée", "Ancienne zone", Instant.now(), Instant.now()
        );
        when(roomService.getAllByBuilding(buildingId, true)).thenReturn(List.of(archived));

        mockMvc.perform(get("/api/buildings/{buildingId}/rooms", buildingId)
                        .param("archived", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Zone Archivée"));
    }

    @Test
    void getAllByBuilding_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/buildings/{buildingId}/rooms", buildingId))
                .andExpect(status().isForbidden());

        verify(roomService, never()).getAllByBuilding(any(), anyBoolean());
    }

    // ─── GET /api/rooms/{id} ──────────────────────────────────────

    @Test
    @WithMockUser
    void getById_shouldReturn200() throws Exception {
        when(roomService.getById(roomId)).thenReturn(mockRoom);

        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Zone A"))
                .andExpect(jsonPath("$.data.id").value(roomId.toString()));
    }

    @Test
    @WithMockUser
    void getById_notFound_shouldReturn404() throws Exception {
        when(roomService.getById(any(UUID.class)))
                .thenThrow(new ResourceNotFoundException("Zone introuvable"));

        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Zone introuvable"));
    }

    @Test
    void getById_unauthenticated_shouldReturn403() throws Exception {
        mockMvc.perform(get("/api/rooms/{id}", roomId))
                .andExpect(status().isForbidden());
    }

    // ─── POST /api/buildings/{buildingId}/rooms ───────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_asAdmin_shouldReturn201() throws Exception {
        RoomRequest request = new RoomRequest("Nouvelle Zone", "Description test");
        when(roomService.create(eq(buildingId), any(RoomRequest.class))).thenReturn(mockRoom);

        mockMvc.perform(post("/api/buildings/{buildingId}/rooms", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Zone A"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void create_asManager_shouldReturn201() throws Exception {
        RoomRequest request = new RoomRequest("Nouvelle Zone", "Description test");
        when(roomService.create(eq(buildingId), any(RoomRequest.class))).thenReturn(mockRoom);

        mockMvc.perform(post("/api/buildings/{buildingId}/rooms", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "READER")
    void create_asReader_shouldReturn403() throws Exception {
        RoomRequest request = new RoomRequest("Nouvelle Zone", "Description test");

        mockMvc.perform(post("/api/buildings/{buildingId}/rooms", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roomService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_withBlankName_shouldReturn400() throws Exception {
        RoomRequest request = new RoomRequest("", "Description test");

        mockMvc.perform(post("/api/buildings/{buildingId}/rooms", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(roomService, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void create_buildingNotFound_shouldReturn404() throws Exception {
        RoomRequest request = new RoomRequest("Zone", "Description");
        when(roomService.create(eq(buildingId), any(RoomRequest.class)))
                .thenThrow(new ResourceNotFoundException("Bâtiment introuvable"));

        mockMvc.perform(post("/api/buildings/{buildingId}/rooms", buildingId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    // ─── PUT /api/rooms/{id} ──────────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_asAdmin_shouldReturn200() throws Exception {
        RoomRequest request = new RoomRequest("Zone Modifiée", "Nouvelle description");
        RoomResponse updated = new RoomResponse(
                roomId, buildingId, "Zone Modifiée", "Nouvelle description", Instant.now(), Instant.now()
        );
        when(roomService.update(eq(roomId), any(RoomRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/rooms/{id}", roomId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Zone Modifiée"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void update_asManager_shouldReturn200() throws Exception {
        RoomRequest request = new RoomRequest("Zone Modifiée", "Nouvelle description");
        RoomResponse updated = new RoomResponse(
                roomId, buildingId, "Zone Modifiée", "Nouvelle description", Instant.now(), Instant.now()
        );
        when(roomService.update(eq(roomId), any(RoomRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/rooms/{id}", roomId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(roles = "READER")
    void update_asReader_shouldReturn403() throws Exception {
        RoomRequest request = new RoomRequest("Zone Modifiée", "Nouvelle description");

        mockMvc.perform(put("/api/rooms/{id}", roomId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());

        verify(roomService, never()).update(any(), any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_notFound_shouldReturn404() throws Exception {
        RoomRequest request = new RoomRequest("Zone Modifiée", "Nouvelle description");
        when(roomService.update(eq(roomId), any(RoomRequest.class)))
                .thenThrow(new ResourceNotFoundException("Zone introuvable"));

        mockMvc.perform(put("/api/rooms/{id}", roomId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void update_withBlankName_shouldReturn400() throws Exception {
        RoomRequest request = new RoomRequest("", "Nouvelle description");

        mockMvc.perform(put("/api/rooms/{id}", roomId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(roomService, never()).update(any(), any());
    }

    // ─── DELETE /api/rooms/{id} ───────────────────────────────────

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_asAdmin_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", roomId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Zone archivée"));

        verify(roomService, times(1)).delete(roomId);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void delete_asManager_shouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", roomId).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(roomService, times(1)).delete(roomId);
    }

    @Test
    @WithMockUser(roles = "READER")
    void delete_asReader_shouldReturn403() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", roomId).with(csrf()))
                .andExpect(status().isForbidden());

        verify(roomService, never()).delete(any());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void delete_notFound_shouldReturn404() throws Exception {
        doThrow(new ResourceNotFoundException("Zone introuvable"))
                .when(roomService).delete(roomId);

        mockMvc.perform(delete("/api/rooms/{id}", roomId).with(csrf()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }
}
