# mi-backend – Teste Técnico MaterImperium

API REST em Java 21 + Spring Boot 3.4.4 para upload e processamento de arquivos. O projeto inclui segurança (Role-based access), banco de dados com migrações automáticas, cobertura rigorosa de testes conteinerizados e documentação interativa da API.

---

## Tecnologias e Arquitetura

O projeto foi construído em arquitetura limpa (**Clean Architecture**) pensando em leveza, performance em manipulação de grandes arquivos e facilidade para teste/deploy:

| Tecnologia | Finalidade / Versão |
|---|---|
| **Java** | 21 (Amazon Corretto) |
| **Spring Boot** | 3.4.4 (Web, Security, JDBC, Validation) |
| **Banco de Dados** | PostgreSQL 16 |
| **Migrações (DB)** | Flyway |
| **Persistência leve** | Spring `JdbcClient` (sem JPA/Hibernate para menor overhead de memória e melhor controle) |
| **Documentação API** | Swagger / OpenAPI 3 (`springdoc-openapi`) |
| **Testes e Qualidade** | JUnit 5 + Testcontainers (PostgreSQL) + JaCoCo (>90% de cobertura mínima restrita no build) |
| **Containerização** | Docker com arquitetura *multi-stage build* nativa |

---

## Pré-requisitos

Para rodar a aplicação imediatamente em um ambiente local isolado:
- **Docker** e **Docker Compose**

*(Não é necessário ter o Java ou Maven instalados nativamente na sua máquina, pois todo o processo de compilação, pacotes e execução foi inteiramente encapsulado nas imagens).*

---

## Como executar (Recomendado)

Disponibilizamos toda a infraestrutura pronta e já orquestrada. 
Na raiz do projeto (onde está o arquivo `docker-compose.yml`), simplesmente execute:

```bash
docker-compose up -d --build
```

O **Docker Compose** se encarregará de forma assíncrona de:
1. Provisionar e subir o contêiner do **PostgreSQL** (`mi-backend-postgres`).
2. Realizar o **build completo e automatizado** da aplicação backend partindo da imagem oficial do Amazon Corretto.
3. Subir a **API** (`mi-backend-api`) e liberar as portas apenas **após** a saúde (*healthcheck*) do banco de dados estar 100% OK.

A aplicação vai expor a porta **8080** no seu locahost. As tabelas necessárias serão criadas automaticamente na inicialização com a migração do `Flyway` dentro da API.

> **Nota para execução direta via fonte (opcional):**
> Caso deseje subir o app diretamente via Host (necessita do Java 21 e Maven instalados), suba primeiro somente o DB usando `docker-compose up -d postgres` e rode o projeto com `./mvnw spring-boot:run`.

---

## Documentação Interativa da API (Swagger)

A API é auto-documentada integrando o visualizador de interface Swagger. Com a aplicação rodando, acesse em qualquer navegador:

- **Swagger UI:** [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON:** [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Lá é possível validar detalhadamente o schema das rotas, simular autenticações via token, e enviar arquivos fisicamente pela própria tela web para testes ágeis de integração.

---

## Autenticação / Segurança

A API protege suas rotas com autenticação baseada em **Bearer Tokens** estáticos. Cada token concede um *Role* de acesso distinto pre-definido, reforçando a separação de controle (`application.yaml`):

| Token Estático | Role de Segurança | Permissões Mapeadas |
|---|---|---|
| `token-envio-secreto` | **ENVIO** | Autorizado a Enviar Arquivos (`/upload`) + Ver Progresso (`/progresso`) |
| `token-consulta-secreto` | **CONSULTA** | Autorizado a Ver Progresso (`/progresso`) + Ver Resumo Final (`/resultado`) |

Se for testar por scripts externos de HTTP (`curl` ou Postman etc.), injete no cabeçalho: 
`Authorization: Bearer <seu-token>`

---

## Testes Automatizados (Garantia de Cobertura)

O sistema de testes mescla um comportamento unitário somado a testes de Repositório/Integração através da biblioteca **Testcontainers**, que levanta uma instância isolada em um *Docker temporário* apenas para passar na esteira de integração validadando query real, e a descartando ao final do runner.

Para ver os relatórios de execução e relatórios de métrica de cobertura:
```bash
./mvnw clean test
```
*(Se atente que na máquina Host isso também exigirá ter daemon de Docker ligado).*
A execução gera um report de **JaCoCo** (`target/site/jacoco/index.html`) e **qualquer branch que falhe mais de 10% da cobertura de instruções é vetada do Build (Rule de Rate 90%)**.

---

## Eficiência de Memória & Lógica

### Regra Técnica de Domínio
A API é impulsionada para digerir arquivos com um layout customizado. As requisições entram delimitadas por pipes (`|`), capturando a primeira string do fragmento de leitura como "Códigos do Registro". 

Exemplo contido num arquivo `dados.txt`:
```text
|0000|017|EMPRESA VIRTUAL XYZ|...
|0001|0|...
|C170|1|ITEM FISCAL|...
```
O processamento assimila, processa concorrentemente e retorna, quando no estado de sucesso (Status 200 OK da última rota), a sumarização:
- `0000` → 1 Ocorrência Processada
- `0001` → 1 Ocorrência Processada
- `C170` → 1 Ocorrência Processada

### Processamento com Footprint Baixo (Leitura Transparente)
Para evitar corrupção por picos de excesso de heap (*OutOfMemoryError*), o Controller despacha de forma assíncrona o stream para Workers de background pool (`ThreadPoolTaskExecutor`), rodando via `spring-boot-async`.

O motor de digestão roda um algoritmo que faz proxy de stream, baseado em `BufferedReader.lines()`. A rotina **não armazena nem mapeia as seções massivas na memória**.  Ele engole linhas, mapeia os buffers e descarta do scope as strings passadas.
A arquitetura atesta suporte contínuo para arquivos imensos na margem dos **Gigabytes** usando apenas **poucos Megabytes em sua pegada de RAM.**
