package com.florestal.gateway.filter;

import com.florestal.gateway.security.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Filtro WebFlux que intercepta todas as requisições, valida o JWT
 * e propaga o contexto de segurança para os serviços downstream.
 *
 * Também injeta os headers X-User-Name e X-User-Role para que os
 * microserviços possam identificar o usuário sem re-validar o token.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtUtil jwtUtil;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // Sem token — segurança decide se a rota é pública ou não
            return chain.filter(exchange);
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (!jwtUtil.isValid(token)) {
            log.warn("Token JWT rejeitado para {}", exchange.getRequest().getPath());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = jwtUtil.getClaims(token);
            String username = claims.getSubject();
            String role     = claims.get("role", String.class);

            // Monta autenticação Spring Security
            var authorities = (role != null)
                    ? List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    : List.<SimpleGrantedAuthority>of();

            var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);

            // Propaga headers para os microserviços downstream
            ServerWebExchange mutated = exchange.mutate()
                    .request(r -> r
                            .header("X-User-Name", username)
                            .header("X-User-Role", role != null ? role : "")
                    )
                    .build();

            return chain.filter(mutated)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));

        } catch (Exception e) {
            log.error("Erro ao processar JWT: {}", e.getMessage());
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }
}
