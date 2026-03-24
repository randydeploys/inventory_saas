package com.inventory.controller;

import com.inventory.config.JwtConfig;
import com.inventory.exception.ResourceNotFoundException;
import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.AuthResponse;
import com.inventory.model.dto.LoginRequest;
import com.inventory.model.dto.RegisterRequest;
import com.inventory.model.dto.UserResponse;
import com.inventory.security.CookieUtil;
import com.inventory.service.AuthService;
import com.inventory.service.AuthService.AuthTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.inventory.model.entity.User;
import com.inventory.repository.UserRepository;
import org.springframework.security.core.Authentication;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtConfig jwtConfig, UserRepository userRepository) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthTokens tokens = authService.register(request);
        return buildAuthResponse(tokens, "Inscription réussie");
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthTokens tokens = authService.login(request);
        return buildAuthResponse(tokens, "Connexion réussie");
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Void>> refresh(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken == null) {
            return ResponseEntity.status(401)
                    .body(ApiResponse.error("Refresh token manquant"));
        }

        String newAccessToken = authService.refresh(refreshToken);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.createAccessTokenCookie(
                                newAccessToken,
                                jwtConfig.accessTokenExpiration()
                        ).toString())
                .body(ApiResponse.success("Token rafraîchi"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest request) {
        String refreshToken = extractCookie(request, "refreshToken");
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.deleteCookie("accessToken", "/api").toString())
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.deleteCookie("refreshToken", "/api/auth").toString())
                .body(ApiResponse.success("Déconnexion réussie"));
    }

    @GetMapping("/me")
public ResponseEntity<?> me() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof UUID)) {
        return ResponseEntity.status(401).body(ApiResponse.error("Non authentifié"));
    }

    UUID userId = (UUID) auth.getPrincipal();
    UUID tenantId = (UUID) auth.getDetails();
    String role = auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "");

    // Charger le user depuis la base pour avoir le nom complet
    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable"));

    UserResponse userResponse = new UserResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole().name(),
            user.isActive(),
            tenantId
    );

    return ResponseEntity.ok(ApiResponse.success("Utilisateur courant", new AuthResponse(userResponse)));
}

    // ─── Helpers ─────────────────────────────────────────────────

    private ResponseEntity<ApiResponse<AuthResponse>> buildAuthResponse(AuthTokens tokens, String message) {
        AuthResponse authResponse = new AuthResponse(tokens.user());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.createAccessTokenCookie(
                                tokens.accessToken(),
                                jwtConfig.accessTokenExpiration()
                        ).toString())
                .header(HttpHeaders.SET_COOKIE,
                        CookieUtil.createRefreshTokenCookie(
                                tokens.rawRefreshToken(),
                                jwtConfig.refreshTokenExpiration()
                        ).toString())
                .body(ApiResponse.success(message, authResponse));
    }

    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if (name.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}