package br.com.alexandreluchetti.mibackend.config.shared;

import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ErrorResponseDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> handleArquivoInvalido(ArquivoInvalidoException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(ProcessamentoEmAndamentoException.class)
    public ResponseEntity<ErrorResponseDTO> handleEmProcessamento(ProcessamentoEmAndamentoException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(UploadNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleNaoEncontrado(UploadNaoEncontradoException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponseDTO(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGeneric(Exception ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Erro interno: " + ex.getMessage()));
    }
}
