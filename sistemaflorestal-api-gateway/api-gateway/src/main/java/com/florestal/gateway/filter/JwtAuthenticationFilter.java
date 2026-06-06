package com.florestal.gateway.filter;

import com.florestal.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Filtro MVC que intercepta todas as requisições, valida o JWT
 * e popula o SecurityContext com a autenticação do usuário.
 *
 * Também injeta os headers X-Usuario-Email e X-Usuario-Perfil para que os
 * microserviços downstream possam identificar o usuário sem re-validar o token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.isValid(token)) {
            log.warn("Token JWT rejeitado para {}", request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            String username = claims.getSubject();
            String role     = claims.get("role", String.class);

            String authority = role != null && role.startsWith("ROLE_")
                    ? role
                    : "ROLE_" + (role != null ? role.toUpperCase() : "USER");

            var authorities = (role != null)
                    ? List.of(new SimpleGrantedAuthority(authority))
                    : List.<SimpleGrantedAuthority>of();

            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(auth);

            // Injeta headers para os microserviços downstream
            MutableHttpServletRequest mutableRequest = new MutableHttpServletRequest(request);
            mutableRequest.putHeader("X-Usuario-Email", username);
            mutableRequest.putHeader("X-Usuario-Perfil", role != null ? role : "");

            filterChain.doFilter(mutableRequest, response);

        } catch (Exception e) {
            log.error("Erro ao processar JWT: {}", e.getMessage());
            SecurityContextHolder.clearContext();
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
    }
}
