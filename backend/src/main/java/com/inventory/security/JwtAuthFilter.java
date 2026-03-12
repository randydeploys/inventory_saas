package com.inventory.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    public JwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 1. Extraire le token depuis le cookie (PAS le header Authorization)
        String token = extractTokenFromCookie(request);

        // 2. Si le token existe ET personne n'est déjà authentifié
        if (token != null && !token.isBlank() && SecurityContextHolder.getContext().getAuthentication() == null) {

            // 3. Valider le JWT et extraire les claims
            Claims claims = jwtTokenProvider.extractClaims(token);

            if (claims != null) {
                // 4. Lire les infos depuis les claims (pas besoin d'aller en base)
                UUID userId = jwtTokenProvider.getUserId(claims);
                UUID tenantId = jwtTokenProvider.getTenantId(claims);
                String role = jwtTokenProvider.getRole(claims);

                // 5. Créer l'objet auth Spring Security
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
                var authentication = new UsernamePasswordAuthenticationToken(
                        userId, null, authorities
                );

                // 6. Stocker le tenantId pour l'utiliser dans les services
                authentication.setDetails(tenantId);

                // 7. Dire à Spring "cet utilisateur est authentifié"
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        // 8. Toujours passer au filtre suivant
        filterChain.doFilter(request, response);
    }

    private String extractTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie cookie : request.getCookies()) {
            if ("accessToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}