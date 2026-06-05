# API Gateway — Sistema Florestal

Gateway central do Sistema Florestal. Responsável por roteamento, autenticação JWT e documentação unificada via Swagger.

## Porta: `8080`

---

## Arquitetura

```
Cliente
  │
  ▼
API Gateway :8080  ◄──► Eureka :8761
  │
  ├─► identificacao-service :8082   (auth, usuários)
  ├─► operacoes-service     :8081   (inventário, plantio, ocorrências)
  ├─► cadastro-service      :8083   (cadastros base)
  └─► auditoria-service     :8085   (trilha de auditoria)
```

---

## Rotas

| Prefixo no Gateway        | Serviço               | Porta  |
|---------------------------|-----------------------|--------|
| `/api/identificacao/**`   | identificacao-service | 8082   |
| `/api/operacoes/**`       | operacoes-service     | 8081   |
| `/api/cadastro/**`        | cadastro-service      | 8083   |
| `/api/auditoria/**`       | auditoria-service     | 8085   |

> O prefixo `/api/<servico>` é removido pelo `StripPrefix=1` antes de repassar ao microserviço.  
> Exemplo: `GET /api/operacoes/inventarios` → `GET /inventarios` no operacoes-service.

---

## Autenticação

O gateway **valida** tokens JWT mas **não os gera** — esse papel é do `identificacao-service`.

### Fluxo

```
1. POST /api/identificacao/auth/login  { "username": "...", "password": "..." }
   ← { "token": "eyJ..." }

2. Demais requisições:
   Authorization: Bearer eyJ...
```

### Rotas públicas (sem token)

- `POST /api/identificacao/auth/login`
- `POST /api/identificacao/auth/registro`
- `/swagger-ui/**`, `/v3/api-docs/**`

---

## Swagger UI

Acesse: **http://localhost:8080/swagger-ui.html**

No dropdown do topo é possível selecionar a spec de cada microserviço individualmente (desde que eles também tenham o SpringDoc configurado).

---

## Headers propagados para os microserviços

| Header         | Valor                        |
|----------------|------------------------------|
| `X-User-Name`  | Username extraído do JWT     |
| `X-User-Role`  | Role extraída do JWT         |

---

## Como rodar

### Pré-requisitos
- Java 17+
- Eureka (service-discovery) rodando em `localhost:8761`

```bash
./mvnw spring-boot:run
```

---

## Configuração JWT

No `application.yml`, ajuste o secret para um valor seguro em produção:

```yaml
jwt:
  secret: sua-chave-secreta-com-minimo-256-bits
  expiration: 86400000  # 24h
```

> ⚠️ O secret deve ser **idêntico** ao configurado no `identificacao-service`.

---

## Estrutura do projeto

```
src/main/java/com/florestal/gateway/
├── ApiGatewayApplication.java
├── config/
│   ├── GatewayConfig.java       # Rotas programáticas + Swagger aggregator
│   ├── SecurityConfig.java      # Spring Security WebFlux
│   └── SwaggerConfig.java       # OpenAPI / JWT scheme
├── filter/
│   ├── JwtAuthenticationFilter.java  # Validação JWT + propagação de headers
│   └── LoggingFilter.java            # Logging de latência
└── security/
    └── JwtUtil.java             # Validação e extração de claims JWT
```
