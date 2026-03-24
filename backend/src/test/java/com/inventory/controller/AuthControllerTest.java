package com.inventory.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.config.JwtConfig;
import com.inventory.model.dto.UserResponse;
import com.inventory.model.dto.LoginRequest;
import com.inventory.model.dto.RegisterRequest;
import com.inventory.security.JwtTokenProvider;
import com.inventory.service.AuthService;
import com.inventory.service.AuthService.AuthTokens;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtConfig jwtConfig;

    // JwtAuthFilter dépend de JwtTokenProvider — doit être mocké même si inutilisé directement
    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private AuthTokens mockTokens;

    @BeforeEach
    void setUp() {
        UserResponse mockUser = new UserResponse(
                UUID.randomUUID(),
                "test@example.com",
                "Test",
                "User",
                "ADMIN",
                true,
                UUID.randomUUID()
        );
        mockTokens = new AuthTokens("mock-access-token", "mock-refresh-token", mockUser);

        when(jwtConfig.accessTokenExpiration()).thenReturn(900L);   // 15 min en secondes
        when(jwtConfig.refreshTokenExpiration()).thenReturn(604800L); // 7 jours en secondes
    }

    // ─── /register ────────────────────────────────────────────────

    @Test
    void register_ShouldReturnCookies_AndSuccessResponse() throws Exception {
        RegisterRequest request = new RegisterRequest(
                "test@example.com", "password123", "Test", "User", "Acme Corp"
        );
        when(authService.register(any(RegisterRequest.class))).thenReturn(mockTokens);

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Inscription réussie"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("accessToken=mock-access-token")))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    // ─── /login ───────────────────────────────────────────────────

    @Test
    void login_ShouldReturnCookies_AndSuccessResponse() throws Exception {
        LoginRequest request = new LoginRequest("test@example.com", "password123");
        when(authService.login(any(LoginRequest.class))).thenReturn(mockTokens);

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Connexion réussie"))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("accessToken=mock-access-token")))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("HttpOnly")));
    }

    @Test
    void login_WhenServiceThrows_ShouldPropagateException() throws Exception {
        LoginRequest request = new LoginRequest("bad@example.com", "wrong");
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new IllegalArgumentException("Email ou mot de passe incorrect"));

        // Le GlobalExceptionHandler gère l'exception → 400
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ─── /refresh ─────────────────────────────────────────────────

    @Test
    void refresh_WithValidToken_ShouldReturnNewAccessTokenCookie() throws Exception {
        Cookie refreshTokenCookie = new Cookie("refreshToken", "valid-refresh-token");
        when(authService.refresh("valid-refresh-token")).thenReturn("new-mock-access-token");

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("accessToken=new-mock-access-token")));
    }

    @Test
    void refresh_WithoutToken_ShouldReturnUnauthorized() throws Exception {
        mockMvc.perform(post("/api/auth/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Refresh token manquant"));
    }

    // ─── /logout ──────────────────────────────────────────────────

    @Test
    void logout_ShouldClearCookies_AndCallService() throws Exception {
        Cookie refreshTokenCookie = new Cookie("refreshToken", "token-to-revoke");

        mockMvc.perform(post("/api/auth/logout")
                .cookie(refreshTokenCookie))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie",
                        org.hamcrest.Matchers.containsString("Max-Age=0")));

        verify(authService, times(1)).logout("token-to-revoke");
    }

    @Test
    void logout_WithoutToken_ShouldStillSucceed() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService, never()).logout(any());
    }
}