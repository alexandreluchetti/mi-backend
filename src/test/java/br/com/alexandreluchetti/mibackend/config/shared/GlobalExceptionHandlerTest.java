package br.com.alexandreluchetti.mibackend.config.shared;

import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ErrorResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler exceptionHandler;

    @BeforeEach
    void setUp() {
        exceptionHandler = new GlobalExceptionHandler();
    }

    @Test
    void testHandleArquivoInvalido() {
        ArquivoInvalidoException ex = new ArquivoInvalidoException("Arquivo com formato inválido");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleArquivoInvalido(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Arquivo com formato inválido", response.getBody().message());
    }

    @Test
    void testHandleEmProcessamento() {
        ProcessamentoEmAndamentoException ex = new ProcessamentoEmAndamentoException();
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleEmProcessamento(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Arquivo ainda em processamento. Consulte o endpoint de progresso.", response.getBody().message());
    }

    @Test
    void testHandleNaoEncontrado() {
        String id = UUID.randomUUID().toString();
        UploadNaoEncontradoException ex = new UploadNaoEncontradoException(id);
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleNaoEncontrado(ex);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Upload não encontrado para o ID: " + id, response.getBody().message());
    }

    @Test
    void testHandleGeneric() {
        Exception ex = new RuntimeException("Erro não mapeado");
        ResponseEntity<ErrorResponseDTO> response = exceptionHandler.handleGeneric(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Erro interno: Erro não mapeado", response.getBody().message());
    }
}
