package com.florestal.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;

/**
 * Filtro global de logging.
 * Registra método, path, status e latência de cada requisição roteada.
 */
@Slf4j
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        long start = Instant.now().toEpochMilli();

        log.info("[GATEWAY] {} {} | requestId={}",
                request.getMethod(),
                request.getURI().getPath(),
                request.getId());

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long latency = Instant.now().toEpochMilli() - start;
            log.info("[GATEWAY] {} {} → {} | {}ms",
                    request.getMethod(),
                    request.getURI().getPath(),
                    exchange.getResponse().getStatusCode(),
                    latency);
        }));
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
