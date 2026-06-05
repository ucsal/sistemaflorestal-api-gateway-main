package com.florestal.gateway.config;

import org.springdoc.core.properties.AbstractSwaggerUiConfigProperties;
import org.springdoc.core.properties.SwaggerUiConfigProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.HashSet;
import java.util.Set;

/**
 * Configuração programática das rotas do gateway.
 *
 * Centraliza aqui a definição das URLs de api-docs dos microserviços
 * para o Swagger aggregator funcionar corretamente.
 */
@Configuration
public class GatewayConfig {

    /**
     * Registra as specs OpenAPI de cada microserviço no Swagger UI agregado.
     * Cada entrada aparece como opção no dropdown do topo da página.
     */
    @Bean
    @Primary
    public SwaggerUiConfigProperties swaggerUiConfigProperties() {
        SwaggerUiConfigProperties config = new SwaggerUiConfigProperties();
        config.setPath("/swagger-ui.html");

        Set<AbstractSwaggerUiConfigProperties.SwaggerUrl> urls = new HashSet<>();
        urls.add(swaggerUrl("Gateway",        "/v3/api-docs"));
        urls.add(swaggerUrl("Identificacao",  "/api/identificacao/v3/api-docs"));
        urls.add(swaggerUrl("Operacoes",      "/api/operacoes/v3/api-docs"));
        urls.add(swaggerUrl("Cadastro",       "/api/cadastro/v3/api-docs"));
        urls.add(swaggerUrl("Auditoria",      "/api/auditoria/v3/api-docs"));

        config.setUrls(urls);
        return config;
    }

    private AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl(String name, String url) {
        AbstractSwaggerUiConfigProperties.SwaggerUrl swaggerUrl =
                new AbstractSwaggerUiConfigProperties.SwaggerUrl();
        swaggerUrl.setName(name);
        swaggerUrl.setUrl(url);
        return swaggerUrl;
    }

    /**
     * RouteLocator programático — útil para rotas com lógica dinâmica.
     * As rotas base estão no application.yml.
     */
    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
                // Rota de health check própria do gateway
                .route("gateway-health", r -> r
                        .path("/gateway/health")
                        .filters(f -> f.setPath("/actuator/health"))
                        .uri("http://localhost:8080"))
                .build();
    }
}
