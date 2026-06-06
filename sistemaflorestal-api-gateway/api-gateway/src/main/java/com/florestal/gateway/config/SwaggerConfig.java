package com.florestal.gateway.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuração do Swagger/OpenAPI para o API Gateway MVC.
 * Acesse: http://localhost:8080/swagger-ui.html
 */
@Configuration
public class SwaggerConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI florestOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema Florestal - API Gateway")
                        .description("""
                                Gateway central do Sistema Florestal.
                                
                                **Autenticação**: obtenha um token JWT via `POST /api/identificacao/auth/login`
                                e clique em **Authorize** informando `Bearer <token>`.
                                
                                **Microserviços disponíveis**:
                                - `identificacao-service` — autenticação e gestão de usuários (porta 8082)
                                - `operacoes-service`     — inventários, plantios e ocorrências (porta 8081)
                                - `cadastro-service`      — cadastros base do sistema (porta 8083)
                                - `auditoria-service`     — trilha de auditoria (porta 8085)
                                """)
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Sistema Florestal")
                                .email("contato@florestal.com"))
                        .license(new License()
                                .name("MIT")
                                .url("https://opensource.org/licenses/MIT"))
                )
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Informe o token JWT obtido em /api/identificacao/auth/login")
                        )
                );
    }
}
