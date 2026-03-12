package com.inventory.controller;

import com.inventory.config.JwtConfig;
import com.inventory.model.dto.ApiResponse;
import com.inventory.model.dto.AuthResponse;
import com.inventory.model.dto.LoginRequest;
import com.inventory.model.dto.RegisterRequest;
import com.inventory.security.CookieUtil;
import com.inventory.service.AuthService;
import com.inventory.service.AuthService.AuthTokens;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtConfig jwtConfig;

    public AuthController(AuthService authService, JwtConfig jwtConfig) {
        this.authService = authService;
        this.jwtConfig = jwtConfig;
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