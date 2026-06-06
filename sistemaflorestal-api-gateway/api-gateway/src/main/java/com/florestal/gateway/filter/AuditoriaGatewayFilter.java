package com.florestal.gateway.filter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditoriaGatewayFilter extends OncePerRequestFilter {

    private final RestTemplate restTemplate;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        filterChain.doFilter(request, response);

        String metodo = request.getMethod();
        String uri = request.getRequestURI();

        if (!deveAuditar(metodo, uri)) {
            return;
        }

        String usuario = request.getHeader("X-Usuario-Email");

        if (usuario == null || usuario.isBlank()) {
            usuario = "usuario-nao-identificado";
        }

        AuditoriaRequest auditoria = new AuditoriaRequest(
                usuario,
                definirAcao(metodo, uri),
                definirMicroservico(uri),
                definirEntidade(uri),
                null,
                response.getStatus() < 400 ? "SUCESSO" : "ERRO",
                "Requisição " + metodo + " " + uri + " retornou HTTP " + response.getStatus()
        );

        try {
            restTemplate.postForObject(
                    "http://localhost:8085/api/auditorias",
                    auditoria,
                    Void.class
            );
        } catch (Exception e) {
            log.warn("Falha ao registrar auditoria: {}", e.getMessage());
        }
    }

    private boolean deveAuditar(String metodo, String uri) {
        if (uri.startsWith("/api/auditorias")) {
            return false;
        }

        return metodo.equals("POST")
                || metodo.equals("PUT")
                || metodo.equals("DELETE");
    }

    private String definirMicroservico(String uri) {
        if (uri.startsWith("/api/areas") || uri.startsWith("/api/especies") || uri.startsWith("/api/colaboradores")) {
            return "cadastro-service";
        }

        if (uri.startsWith("/api/plantios") || uri.startsWith("/api/inventarios") || uri.startsWith("/api/ocorrencias")) {
            return "operacoes-service";
        }

        if (uri.startsWith("/api/identificacao") || uri.startsWith("/api/auth") || uri.startsWith("/api/usuarios")) {
            return "identidade-service";
        }

        return "api-gateway";
    }

    private String definirEntidade(String uri) {
        if (uri.startsWith("/api/areas")) return "AreaFlorestal";
        if (uri.startsWith("/api/especies")) return "Especie";
        if (uri.startsWith("/api/colaboradores")) return "Colaborador";
        if (uri.startsWith("/api/plantios")) return "Plantio";
        if (uri.startsWith("/api/inventarios")) return "InventarioFlorestal";
        if (uri.startsWith("/api/ocorrencias")) return "Ocorrencia";
        if (uri.startsWith("/api/usuarios")) return "Usuario";

        return "Requisicao";
    }

    private String definirAcao(String metodo, String uri) {
        if (metodo.equals("POST") && uri.startsWith("/api/areas")) return "CADASTRAR_AREA_FLORESTAL";
        if (metodo.equals("PUT") && uri.startsWith("/api/areas")) return "ATUALIZAR_AREA_FLORESTAL";
        if (metodo.equals("DELETE") && uri.startsWith("/api/areas")) return "INATIVAR_AREA_FLORESTAL";

        if (metodo.equals("POST") && uri.startsWith("/api/especies")) return "CADASTRAR_ESPECIE";
        if (metodo.equals("PUT") && uri.startsWith("/api/especies")) return "ATUALIZAR_ESPECIE";
        if (metodo.equals("DELETE") && uri.startsWith("/api/especies")) return "INATIVAR_ESPECIE";

        if (metodo.equals("POST") && uri.startsWith("/api/colaboradores")) return "CADASTRAR_COLABORADOR";
        if (metodo.equals("PUT") && uri.startsWith("/api/colaboradores")) return "ATUALIZAR_COLABORADOR";
        if (metodo.equals("DELETE") && uri.startsWith("/api/colaboradores")) return "INATIVAR_COLABORADOR";

        if (metodo.equals("POST") && uri.startsWith("/api/plantios")) return "REGISTRAR_PLANTIO";
        if (metodo.equals("PUT") && uri.startsWith("/api/plantios")) return "ATUALIZAR_PLANTIO";

        if (metodo.equals("POST") && uri.startsWith("/api/inventarios")) return "REGISTRAR_INVENTARIO";
        if (metodo.equals("PUT") && uri.startsWith("/api/inventarios")) return "ATUALIZAR_INVENTARIO";

        if (metodo.equals("POST") && uri.startsWith("/api/ocorrencias")) return "REGISTRAR_OCORRENCIA";
        if (metodo.equals("PUT") && uri.startsWith("/api/ocorrencias")) return "ATUALIZAR_OCORRENCIA";

        return "GERAR_RELATORIO";
    }

    public record AuditoriaRequest(
            String usuario,
            String acao,
            String microservicoOrigem,
            String entidade,
            Long entidadeId,
            String status,
            String detalhes
    ) {}
}
