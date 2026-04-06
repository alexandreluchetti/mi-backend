package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
    public void processar(UUID uploadId, Path arquivo) {
        log.info("Iniciando processamento do upload (Background reativo): {}", uploadId);

        Mono.fromRunnable(() -> {
            try (BufferedReader reader = Files.newBufferedReader(arquivo, StandardCharsets.UTF_8)) {
                Map<String, Long> contagens = new LinkedHashMap<>();

                reader.lines().forEach(linha -> {
                    String codigoRegistro = extrairCodigoRegistro(linha);
                    if (codigoRegistro != null && !codigoRegistro.isBlank()) {
                        contagens.merge(codigoRegistro, 1L, (a, b) -> a + b);
                    }
                });

                resumoRepository.saveAll(uploadId, contagens);
                uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_SUCESSO);

                log.info("Processamento finalizado com sucesso: {} | {} tipos de registro gravados.", uploadId, contagens.size());
            } catch (IOException e) {
                log.error("Erro de I/O no processamento do upload {}: {}", uploadId, e.getMessage());
                uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_ERROS);
            } catch (Exception e) {
                log.error("Erro inesperado no processamento do upload {}: {}", uploadId, e.getMessage());
                uploadRepository.updateStatus(uploadId, StatusProcessamento.FINALIZADO_COM_ERROS);
            }
        }).subscribeOn(Schedulers.boundedElastic())
          .doFinally(signalType -> {
              try {
                  Files.deleteIfExists(arquivo);
                  log.info("Arquivo de processamento {} deletado", arquivo);
              } catch (IOException e) {
                  log.error("Nao foi possivel apagar o arquivo definitivo {}", arquivo);
              }
          }).subscribe();
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
