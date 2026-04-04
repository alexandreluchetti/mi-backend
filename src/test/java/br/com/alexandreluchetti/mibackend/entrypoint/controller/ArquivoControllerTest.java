package br.com.alexandreluchetti.mibackend.entrypoint.controller;

import br.com.alexandreluchetti.mibackend.config.shared.SecurityConfig;
import br.com.alexandreluchetti.mibackend.core.model.*;
import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import br.com.alexandreluchetti.mibackend.config.shared.StaticTokenFilter;
import br.com.alexandreluchetti.mibackend.core.usecase.impl.ArquivoUseCaseImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(ArquivoController.class)
@Import({SecurityConfig.class, StaticTokenFilter.class})
@TestPropertySource(properties = {
        "security.tokens.envio=token-envio-secreto",
        "security.tokens.consulta=token-consulta-secreto"
})
@DisplayName("ArquivoController")
class ArquivoControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private ArquivoUseCaseImpl arquivoUseCaseImpl;

    private static final String TOKEN_ENVIO    = "Bearer token-envio-secreto";
    private static final String TOKEN_CONSULTA = "Bearer token-consulta-secreto";
    private static final String TOKEN_INVALIDO = "Bearer token-errado";

    private static final UUID UPLOAD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private MultipartBodyBuilder validMultipartBody() {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("file", "|0000|017|HEADER\n|0001|0|\n|1000|X|\n".getBytes(StandardCharsets.UTF_8))
                .header("Content-Disposition", "form-data; name=file; filename=dados.txt");
        return builder;
    }

    @Nested
    @DisplayName("POST /api/arquivos/upload")
    class UploadEndpointTests {

        @Test
        @DisplayName("201 – upload aceito com token ENVIO e arquivo válido")
        void deveRetornar201QuandoUploadValido() {
            when(arquivoUseCaseImpl.upload(any()))
                    .thenReturn(Mono.just(new UploadResponse(UPLOAD_ID)));

            webTestClient.post().uri("/api/arquivos/upload")
                    .header("Authorization", TOKEN_ENVIO)
                    .body(BodyInserters.fromMultipartData(validMultipartBody().build()))
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.id").isEqualTo(UPLOAD_ID.toString());
        }

        @Test
        @DisplayName("400 – cabeçalho do arquivo inválido retorna mensagem de erro")
        void deveRetornar400QuandoCabecalhoInvalido() {
            when(arquivoUseCaseImpl.upload(any()))
                    .thenReturn(Mono.error(new ArquivoInvalidoException("Cabeçalho inválido.")));

            webTestClient.post().uri("/api/arquivos/upload")
                    .header("Authorization", TOKEN_ENVIO)
                    .body(BodyInserters.fromMultipartData(validMultipartBody().build()))
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").isEqualTo("Cabeçalho inválido.");
        }

        @Test
        @DisplayName("401 – requisição sem token é rejeitada")
        void deveRetornar401SemToken() {
            webTestClient.post().uri("/api/arquivos/upload")
                    .body(BodyInserters.fromMultipartData(validMultipartBody().build()))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("401 – token inválido retorna 401")
        void deveRetornar401ComTokenInvalido() {
            webTestClient.post().uri("/api/arquivos/upload")
                    .header("Authorization", TOKEN_INVALIDO)
                    .body(BodyInserters.fromMultipartData(validMultipartBody().build()))
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("403 – token CONSULTA não pode fazer upload")
        void deveRetornar403ComTokenConsulta() {
            webTestClient.post().uri("/api/arquivos/upload")
                    .header("Authorization", TOKEN_CONSULTA)
                    .body(BodyInserters.fromMultipartData(validMultipartBody().build()))
                    .exchange()
                    .expectStatus().isForbidden();
        }
    }

    @Nested
    @DisplayName("GET /api/arquivos/{id}/progresso")
    class ProgressoEndpointTests {

        @Test
        @DisplayName("200 – progresso retornado com token ENVIO e status EM_PROCESSAMENTO")
        void deveRetornar200ComTokenEnvio() {
            when(arquivoUseCaseImpl.consultarProgresso(UPLOAD_ID.toString()))
                    .thenReturn(Mono.just(new ProgressoResponse(StatusProcessamento.EM_PROCESSAMENTO)));

            webTestClient.get().uri("/api/arquivos/{id}/progresso", UPLOAD_ID)
                    .header("Authorization", TOKEN_ENVIO)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("EM_PROCESSAMENTO");
        }

        @Test
        @DisplayName("200 – progresso retornado com token CONSULTA")
        void deveRetornar200ComTokenConsulta() {
            when(arquivoUseCaseImpl.consultarProgresso(UPLOAD_ID.toString()))
                    .thenReturn(Mono.just(new ProgressoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO)));

            webTestClient.get().uri("/api/arquivos/{id}/progresso", UPLOAD_ID)
                    .header("Authorization", TOKEN_CONSULTA)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("FINALIZADO_COM_SUCESSO");
        }

        @Test
        @DisplayName("401 – sem token retorna 401")
        void deveRetornar401SemToken() {
            webTestClient.get().uri("/api/arquivos/{id}/progresso", UPLOAD_ID)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("401 – token inválido retorna 401")
        void deveRetornar401ComTokenInvalido() {
            webTestClient.get().uri("/api/arquivos/{id}/progresso", UPLOAD_ID)
                    .header("Authorization", TOKEN_INVALIDO)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("404 – ID inexistente retorna mensagem de erro")
        void deveRetornar404ParaIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(arquivoUseCaseImpl.consultarProgresso(idInexistente.toString()))
                    .thenReturn(Mono.error(new UploadNaoEncontradoException(idInexistente.toString())));

            webTestClient.get().uri("/api/arquivos/{id}/progresso", idInexistente)
                    .header("Authorization", TOKEN_ENVIO)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }
    }

    @Nested
    @DisplayName("GET /api/arquivos/{id}/resultado")
    class ResultadoEndpointTests {

        @Test
        @DisplayName("200 – resultado retornado com token CONSULTA e processamento finalizado")
        void deveRetornar200ComResultadoFinalizado() {
            List<ResumoItem> resumo = List.of(
                    new ResumoItem("0000", 1L),
                    new ResumoItem("1000", 10L)
            );
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenReturn(Mono.just(new ResultadoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO, resumo)));

            webTestClient.get().uri("/api/arquivos/{id}/resultado", UPLOAD_ID)
                    .header("Authorization", TOKEN_CONSULTA)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.status").isEqualTo("FINALIZADO_COM_SUCESSO")
                    .jsonPath("$.resumo").isArray()
                    .jsonPath("$.resumo.length()").isEqualTo(2)
                    .jsonPath("$.resumo[0].registro").isEqualTo("0000")
                    .jsonPath("$.resumo[1].total").isEqualTo(10);
        }

        @Test
        @DisplayName("400 – arquivo ainda em processamento retorna erro")
        void deveRetornar400QuandoEmProcessamento() {
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenReturn(Mono.error(new ProcessamentoEmAndamentoException()));

            webTestClient.get().uri("/api/arquivos/{id}/resultado", UPLOAD_ID)
                    .header("Authorization", TOKEN_CONSULTA)
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").exists();
        }

        @Test
        @DisplayName("401 – sem token retorna 401")
        void deveRetornar401SemToken() {
            webTestClient.get().uri("/api/arquivos/{id}/resultado", UPLOAD_ID)
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("403 – token ENVIO não pode consultar resultado")
        void deveRetornar403ComTokenEnvio() {
            webTestClient.get().uri("/api/arquivos/{id}/resultado", UPLOAD_ID)
                    .header("Authorization", TOKEN_ENVIO)
                    .exchange()
                    .expectStatus().isForbidden();
        }

        @Test
        @DisplayName("404 – ID inexistente retorna 404")
        void deveRetornar404ParaIdInexistente() {
            UUID idInexistente = UUID.randomUUID();
            when(arquivoUseCaseImpl.consultarResultado(idInexistente.toString()))
                    .thenReturn(Mono.error(new UploadNaoEncontradoException(idInexistente.toString())));

            webTestClient.get().uri("/api/arquivos/{id}/resultado", idInexistente)
                    .header("Authorization", TOKEN_CONSULTA)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("200 – resultado com lista de resumo vazia quando nenhum registro foi processado")
        void deveRetornar200ComResumoVazio() {
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenReturn(Mono.just(new ResultadoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO, List.of())));

            webTestClient.get().uri("/api/arquivos/{id}/resultado", UPLOAD_ID)
                    .header("Authorization", TOKEN_CONSULTA)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.resumo").isArray()
                    .jsonPath("$.resumo.length()").isEqualTo(0);
        }
    }
}
