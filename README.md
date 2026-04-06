# mi-backend – Teste Técnico MaterImperium

API REST Reativa em **Java 21** + **Spring Boot 3.4.4** para upload e processamento de arquivos Sped-like. O projeto utiliza o **Project Reactor** para processamento não-bloqueante, segurança baseada em papéis (RBAC), banco de dados com migrações automáticas e cobertura de testes rigorosa.

---

## 🚀 Tecnologias e Arquitetura

O projeto foi construído seguindo os princípios da **Clean Architecture**, priorizando a separação de preocupações, performance em alta volumetria e baixo footprint de memória:

| Tecnologia | Finalidade / Versão |
|---|---|
| **Java** | 21 (Amazon Corretto) |
| **Spring Boot** | 3.4.4 (**WebFlux**, Security, JDBC, Validation) |
| **Project Reactor** | Processamento assíncrono e reativo (`Mono`, `Flux`, `Schedulers`) |
| **Banco de Dados** | PostgreSQL 16 |
| **Migrações (DB)** | Flyway |
| **Persistência leve** | Spring `JdbcClient` (Alternativa leve ao JPA/Hibernate) |
| **Documentação API** | Swagger / OpenAPI 3 (`springdoc-openapi`) |
| **Testes** | JUnit 5 + Mockito + Testcontainers + JaCoCo (>90% de cobertura) |
| **Containerização** | Docker (Multi-stage build) |

---

## 🛠️ Regras de Negócio e Validação

A aplicação impõe validações rigorosas antes de iniciar o processamento em background:

1.  **Validação de Cabeçalho**:
    - **Linha 1**: Deve obrigatoriamente iniciar com `|0000|017|` ou `|0000|006|`.
    - **Linha 2**: Deve conter exatamente o valor `|0001|0|`.
2.  **Processamento de Registros**:
    - O sistema identifica o "Código do Registro" (primeiro elemento após o pipe opcional).
    - Gera uma sumarização (contador) de todas as ocorrências por tipo de registro.
3.  **Histórico e Isolamento**: 
    - Cada upload gera um novo `UUID` único. 
    - Os resumos são armazenados vinculados a esse ID, mantendo um histórico completo de todos os arquivos processados.

---

## 📂 Recursos de Teste

Disponibilizamos uma pasta `test_files/` na raiz do projeto com arquivos prontos para validar o comportamento do sistema:

- `valid_017.txt` / `valid_006.txt`: Sucesso básico.
- `complex_resumo.txt`: Múltiplos registros para validar a contagem do resumo.
- `edge_cases.txt`: Casos especiais (espaços, sem pipe inicial, pipes duplos).
- `whitespace_test.txt`: Garante que linhas em branco sejam ignoradas.
- `invalid_*.txt`: Cenários de falha na validação de cabeçalho.

---

## 🏃 Como Executar (Docker Compose)

Certifique-se de ter o **Docker** instalado. Na raiz do projeto, execute:

```bash
docker-compose up -d --build
```

A API estará disponível em `http://localhost:8080`.

### Endpoints Principais:
- **POST** `/api/arquivos/upload`: Envio do arquivo (Multipart).
- **GET** `/api/arquivos/{id}/progresso`: Consulta do status (`EM_PROCESSAMENTO`, `FINALIZADO_COM_ERROS`, `FINALIZADO_COM_SUCESSO`).
- **GET** `/api/arquivos/{id}/resultado`: Retorna o JSON com o resumo das contagens (apenas após finalizado).

---

## 🔐 Autenticação e Segurança

A API utiliza **Static Bearer Tokens** configurados via `application.yaml`:

| Token | Role | Permissões |
|---|---|---|
| `token-envio-secreto` | **ENVIO** | Upload e Consulta de Progresso |
| `token-consulta-secreto` | **CONSULTA** | Consulta de Progresso e Resultado Final |

---

## 📊 Documentação e Qualidade

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **Testes**: Execute `./mvnw clean test` para rodar a suíte completa com **Testcontainers**.
- **Cobertura**: O relatório reside em `target/site/jacoco/index.html` após os testes. A build falha se a cobertura for inferior a **90%**.
