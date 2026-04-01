# mi-backend – Teste Técnico MaterImperium

API REST em Java 21 + Spring Boot 3.4.4 para upload e processamento de arquivos.

---

## Tecnologias

| Tecnologia | Versão |
|---|---|
| Java | 21 |
| Spring Boot | 3.4.4 |
| Spring Security | (incluso no Boot) |
| Spring JDBC (JdbcClient) | (incluso no Boot) |
| PostgreSQL | 16 |
| Flyway | (incluso no Boot) |
| Lombok | (incluso no Boot) |

---

## Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose (para subir o PostgreSQL)

---

## Como executar

### 1. Subir o banco de dados

```bash
docker-compose up -d
```

O PostgreSQL ficará disponível em `localhost:5432` com:
- **Database:** `mibackend`
- **User:** `postgres`
- **Password:** `postgres`

### 2. Executar a aplicação

```bash
./mvnw spring-boot:run
```

A aplicação iniciará na porta **8080** e o Flyway criará o schema automaticamente.

---

## Autenticação

A API usa **Bearer Token** estático. Existem dois tokens (configuráveis em `application.yaml`):

| Token | Role | Permissões |
|---|---|---|
| `token-envio-secreto` | ENVIO | Upload + Consulta de Progresso |
| `token-consulta-secreto` | CONSULTA | Consulta de Progresso + Consulta de Resultado |

Inclua o header em todas as requisições:
```
Authorization: Bearer <token>
```

---

## Endpoints

### POST `/api/arquivos/upload`
**Role:** ENVIO

Recebe um arquivo multipart, valida o cabeçalho e inicia o processamento em background.

**Validações do cabeçalho:**
- Linha 1: deve iniciar com `|0000|017|` **ou** `|0000|006|`
- Linha 2: deve conter exatamente `|0001|0|`

**Curl:**
```bash
curl -X POST http://localhost:8080/api/arquivos/upload \
  -H "Authorization: Bearer token-envio-secreto" \
  -F "file=@/caminho/para/arquivo.txt"
```

**Resposta (201 Created):**
```json
{ "id": "550e8400-e29b-41d4-a716-446655440000" }
```

---

### GET `/api/arquivos/{id}/progresso`
**Role:** ENVIO ou CONSULTA

Consulta o status do processamento.

**Curl:**
```bash
curl http://localhost:8080/api/arquivos/{id}/progresso \
  -H "Authorization: Bearer token-envio-secreto"
```

**Resposta (200 OK):**
```json
{ "status": "EM_PROCESSAMENTO" }
```

**Status possíveis:** `EM_PROCESSAMENTO`, `FINALIZADO_COM_SUCESSO`, `FINALIZADO_COM_ERROS`

---

### GET `/api/arquivos/{id}/resultado`
**Role:** CONSULTA

Retorna o resultado do processamento. Se ainda em andamento, retorna 400.

**Curl:**
```bash
curl http://localhost:8080/api/arquivos/{id}/resultado \
  -H "Authorization: Bearer token-consulta-secreto"
```

**Resposta (200 OK):**
```json
{
  "status": "FINALIZADO_COM_SUCESSO",
  "resumo": [
    { "registro": "0000", "total": 1 },
    { "registro": "0001", "total": 1 },
    { "registro": "C170", "total": 3 }
  ]
}
```

**Resposta (400 Bad Request – ainda processando):**
```json
{ "message": "Arquivo ainda em processamento. Consulte o endpoint de progresso." }
```

---

## Código de Registro

O arquivo é delimitado por `|` (pipe). O **primeiro campo** de cada linha é o "Código do Registro". Exemplo:

```
|0000|017|EMPRESA XYZ|...
|0001|0|...
|C170|1|ITEM A|...
```

Resulta em:
- `0000` → 1 ocorrência
- `0001` → 1 ocorrência
- `C170` → 1 ocorrência

---

## Eficiência de Memória

O processamento é feito via `BufferedReader.lines()` — leitura **linha a linha** sem nunca carregar o arquivo inteiro em memória. Suporta arquivos de até 1 GB com footprint mínimo.

---

## Configuração

As configurações estão em `src/main/resources/application.yaml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/mibackend
    username: postgres
    password: postgres

security:
  tokens:
    envio: "token-envio-secreto"
    consulta: "token-consulta-secreto"
```
