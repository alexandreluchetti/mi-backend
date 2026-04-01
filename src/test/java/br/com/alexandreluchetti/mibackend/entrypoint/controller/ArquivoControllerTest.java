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
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes de integração da camada web (Controller + Security) usando @WebMvcTest.
 * O ArquivoUseCase é mockado — nenhuma conexão com banco de dados é necessária.
 *
 * Tokens estáticos (injetados via @TestPropertySource):
 *   - ENVIO:    "token-envio-secreto"
 *   - CONSULTA: "token-consulta-secreto"
 */
@WebMvcTest(ArquivoController.class)
@Import({SecurityConfig.class,
         StaticTokenFilter.class})
@TestPropertySource(properties = {
        "security.tokens.envio=token-envio-secreto",
        "security.tokens.consulta=token-consulta-secreto"
})
@DisplayName("ArquivoController")
class ArquivoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ArquivoUseCaseImpl arquivoUseCaseImpl;

    private static final String TOKEN_ENVIO    = "Bearer token-envio-secreto";
    private static final String TOKEN_CONSULTA = "Bearer token-consulta-secreto";
    private static final String TOKEN_INVALIDO = "Bearer token-errado";

    private static final UUID UPLOAD_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    /** Helper: cria um multipart file de upload mínimo válido. */
    private MockMultipartFile validMultipartFile() {
        byte[] content = "|0000|017|HEADER\n|0001|0|\n|1000|X|\n".getBytes();
        return new MockMultipartFile("file", "dados.txt", "text/plain", content);
    }

    // ================================================================== //
    //  POST /api/arquivos/upload                                          //
    // ================================================================== //
    @Nested
    @DisplayName("POST /api/arquivos/upload")
    class UploadEndpointTests {

        @Test
        @DisplayName("201 – upload aceito com token ENVIO e arquivo válido")
        void deveRetornar201QuandoUploadValido() throws Exception {
            // UploadResponseDTO record field is 'id'
            when(arquivoUseCaseImpl.upload(any())).thenReturn(new UploadResponse(UPLOAD_ID));

            mockMvc.perform(multipart("/api/arquivos/upload")
                            .file(validMultipartFile())
                            .header("Authorization", TOKEN_ENVIO))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(UPLOAD_ID.toString()));
        }

        @Test
        @DisplayName("400 – cabeçalho do arquivo inválido retorna mensagem de erro")
        void deveRetornar400QuandoCabecalhoInvalido() throws Exception {
            when(arquivoUseCaseImpl.upload(any()))
                    .thenThrow(new ArquivoInvalidoException("Cabeçalho inválido."));

            mockMvc.perform(multipart("/api/arquivos/upload")
                            .file(validMultipartFile())
                            .header("Authorization", TOKEN_ENVIO))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").value("Cabeçalho inválido."));
        }

        @Test
        @DisplayName("401 – requisição sem token é rejeitada")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(multipart("/api/arquivos/upload")
                            .file(validMultipartFile()))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 – token inválido retorna 401")
        void deveRetornar401ComTokenInvalido() throws Exception {
            mockMvc.perform(multipart("/api/arquivos/upload")
                            .file(validMultipartFile())
                            .header("Authorization", TOKEN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 – token CONSULTA não pode fazer upload")
        void deveRetornar403ComTokenConsulta() throws Exception {
            mockMvc.perform(multipart("/api/arquivos/upload")
                            .file(validMultipartFile())
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isForbidden());
        }
    }

    // ================================================================== //
    //  GET /api/arquivos/{id}/progresso                                   //
    // ================================================================== //
    @Nested
    @DisplayName("GET /api/arquivos/{id}/progresso")
    class ProgressoEndpointTests {

        @Test
        @DisplayName("200 – progresso retornado com token ENVIO e status EM_PROCESSAMENTO")
        void deveRetornar200ComTokenEnvio() throws Exception {
            when(arquivoUseCaseImpl.consultarProgresso(UPLOAD_ID.toString()))
                    .thenReturn(new ProgressoResponse(StatusProcessamento.EM_PROCESSAMENTO));

            mockMvc.perform(get("/api/arquivos/{id}/progresso", UPLOAD_ID)
                            .header("Authorization", TOKEN_ENVIO))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("EM_PROCESSAMENTO"));
        }

        @Test
        @DisplayName("200 – progresso retornado com token CONSULTA")
        void deveRetornar200ComTokenConsulta() throws Exception {
            when(arquivoUseCaseImpl.consultarProgresso(UPLOAD_ID.toString()))
                    .thenReturn(new ProgressoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO));

            mockMvc.perform(get("/api/arquivos/{id}/progresso", UPLOAD_ID)
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FINALIZADO_COM_SUCESSO"));
        }

        @Test
        @DisplayName("401 – sem token retorna 401")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/api/arquivos/{id}/progresso", UPLOAD_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("401 – token inválido retorna 401")
        void deveRetornar401ComTokenInvalido() throws Exception {
            mockMvc.perform(get("/api/arquivos/{id}/progresso", UPLOAD_ID)
                            .header("Authorization", TOKEN_INVALIDO))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("404 – ID inexistente retorna mensagem de erro")
        void deveRetornar404ParaIdInexistente() throws Exception {
            UUID idInexistente = UUID.randomUUID();
            when(arquivoUseCaseImpl.consultarProgresso(idInexistente.toString()))
                    .thenThrow(new UploadNaoEncontradoException(idInexistente.toString()));

            mockMvc.perform(get("/api/arquivos/{id}/progresso", idInexistente)
                            .header("Authorization", TOKEN_ENVIO))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    // ================================================================== //
    //  GET /api/arquivos/{id}/resultado                                   //
    // ================================================================== //
    @Nested
    @DisplayName("GET /api/arquivos/{id}/resultado")
    class ResultadoEndpointTests {

        @Test
        @DisplayName("200 – resultado retornado com token CONSULTA e processamento finalizado")
        void deveRetornar200ComResultadoFinalizado() throws Exception {
            List<ResumoItem> resumo = List.of(
                    new ResumoItem("0000", 1L),
                    new ResumoItem("1000", 10L)
            );
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenReturn(new ResultadoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO, resumo));

            mockMvc.perform(get("/api/arquivos/{id}/resultado", UPLOAD_ID)
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("FINALIZADO_COM_SUCESSO"))
                    .andExpect(jsonPath("$.resumo").isArray())
                    .andExpect(jsonPath("$.resumo.length()").value(2))
                    .andExpect(jsonPath("$.resumo[0].registro").value("0000"))
                    .andExpect(jsonPath("$.resumo[1].total").value(10));
        }

        @Test
        @DisplayName("400 – arquivo ainda em processamento retorna erro")
        void deveRetornar400QuandoEmProcessamento() throws Exception {
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenThrow(new ProcessamentoEmAndamentoException());

            mockMvc.perform(get("/api/arquivos/{id}/resultado", UPLOAD_ID)
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }

        @Test
        @DisplayName("401 – sem token retorna 401")
        void deveRetornar401SemToken() throws Exception {
            mockMvc.perform(get("/api/arquivos/{id}/resultado", UPLOAD_ID))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("403 – token ENVIO não pode consultar resultado")
        void deveRetornar403ComTokenEnvio() throws Exception {
            mockMvc.perform(get("/api/arquivos/{id}/resultado", UPLOAD_ID)
                            .header("Authorization", TOKEN_ENVIO))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("404 – ID inexistente retorna 404")
        void deveRetornar404ParaIdInexistente() throws Exception {
            UUID idInexistente = UUID.randomUUID();
            when(arquivoUseCaseImpl.consultarResultado(idInexistente.toString()))
                    .thenThrow(new UploadNaoEncontradoException(idInexistente.toString()));

            mockMvc.perform(get("/api/arquivos/{id}/resultado", idInexistente)
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("200 – resultado com lista de resumo vazia quando nenhum registro foi processado")
        void deveRetornar200ComResumoVazio() throws Exception {
            when(arquivoUseCaseImpl.consultarResultado(UPLOAD_ID.toString()))
                    .thenReturn(new ResultadoResponse(StatusProcessamento.FINALIZADO_COM_SUCESSO, List.of()));

            mockMvc.perform(get("/api/arquivos/{id}/resultado", UPLOAD_ID)
                            .header("Authorization", TOKEN_CONSULTA))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.resumo").isArray())
                    .andExpect(jsonPath("$.resumo.length()").value(0));
        }
    }
}
