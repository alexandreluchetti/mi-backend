package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ProcessamentoUseCaseImpl implements ProcessamentoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ProcessamentoUseCaseImpl.class);

    private final UploadRepository uploadRepository;
    private final ResumoRepository resumoRepository;

    public ProcessamentoUseCaseImpl(
            UploadRepository uploadRepository,
            ResumoRepository resumoRepository
    ) {
        this.uploadRepository = uploadRepository;
        this.resumoRepository = resumoRepository;
    }

    @Override
    @Async("processingExecutor")
    public void processar(UUID uploadId, InputStream inputStream) {
        log.info("Iniciando processamento do upload: {}", uploadId);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            Map<String, Long> contagens = new LinkedHashMap<>();

            // Processa linha por linha — nunca carrega o arquivo inteiro em memória
            reader.lines().forEach(linha -> {
                String codigoRegistro = extrairCodigoRegistro(linha);
                if (codigoRegistro != null && !codigoRegistro.isBlank()) {
                    contagens.merge(codigoRegistro, 1L, (a, b) -> a + b);
                }
            });

            resumoRepository.saveAll(uploadId, contagens);
            uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_SUCESSO);

            log.info("Processamento finalizado com sucesso: {} | {} tipos de registro", uploadId, contagens.size());

        } catch (IOException e) {
            log.error("Erro de I/O no processamento do upload {}: {}", uploadId, e.getMessage());
            uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_ERROS);
        } catch (Exception e) {
            log.error("Erro inesperado no processamento do upload {}: {}", uploadId, e.getMessage());
            uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_ERROS);
        }
    }

    /**
     * Extrai o primeiro campo separado por "|" de uma linha do arquivo.
     * Exemplo: "|0000|017|..." → "0000"
     */
    private String extrairCodigoRegistro(String linha) {
        if (linha == null || linha.isBlank()) return null;

        // Remove o pipe inicial se existir
        String normalizada = linha.startsWith("|") ? linha.substring(1) : linha;

        int pipeIndex = normalizada.indexOf('|');
        if (pipeIndex < 0) return normalizada.trim();

        return normalizada.substring(0, pipeIndex).trim();
    }
}
