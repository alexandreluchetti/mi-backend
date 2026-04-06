package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.model.*;
import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ArquivoUseCase;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

public class ArquivoUseCaseImpl implements ArquivoUseCase {

    private static final Logger log = LoggerFactory.getLogger(ArquivoUseCaseImpl.class);

    private static final String HEADER_PREFIX_017 = "|0000|017|";
    private static final String HEADER_PREFIX_006 = "|0000|006|";
    private static final String SEGUNDA_LINHA_ESPERADA = "|0001|0|";
    private static final String UPLOADS_DIR = ".uploads";

    private final UploadRepository uploadRepository;
    private final ResumoRepository resumoRepository;
    private final ProcessamentoUseCase processamentoUseCase;

    public ArquivoUseCaseImpl(
            UploadRepository uploadRepository,
            ResumoRepository resumoRepository,
            ProcessamentoUseCase processamentoUseCase
    ) {
        this.uploadRepository = uploadRepository;
        this.resumoRepository = resumoRepository;
        this.processamentoUseCase = processamentoUseCase;
        
        try {
            Files.createDirectories(Paths.get(UPLOADS_DIR));
        } catch (IOException e) {
            log.error("Não foi possível criar o diretório de uploads: {}", e.getMessage(), e);
            throw new RuntimeException("Não foi possível criar o diretório de uploads", e);
        }
    }

    @Override
    public Mono<UploadResponse> upload(FilePart file) {
        if (file == null || file.filename().isEmpty()) {
            log.warn("Arquivo não enviado ou vazio.");
            return Mono.error(new ArquivoInvalidoException("Arquivo não enviado ou vazio."));
        }

        return Mono.fromCallable(uploadRepository::save)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(uploadId -> {
                    Path filePath = Paths.get(UPLOADS_DIR, uploadId.toString() + ".txt");
                    log.info("File path: {}", filePath);
                    
                    return file.transferTo(filePath)
                            .then(Mono.fromCallable(() -> {
                                validarCabecalho(filePath);
                                log.info("Arquivo enviado com sucesso: {}", uploadId);
                                return uploadId;
                            })).onErrorResume(e -> {
                                try {
                                    Files.deleteIfExists(filePath);
                                } catch (IOException exception) {
                                    log.warn(exception.getMessage(), e);
                                }
                                return Mono.error(e);
                            });
                })
                .doOnSuccess(uploadId -> {
                    Path filePath = Paths.get(UPLOADS_DIR, uploadId.toString() + ".txt");
                    processamentoUseCase.processar(uploadId, filePath);
                })
                .map(UploadResponse::new);
    }

    /**
     * Valida as 2 primeiras linhas do arquivo gerado no disco definitivo.
     */
    private void validarCabecalho(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath, StandardCharsets.UTF_8)) {
            String primeiraLinha = reader.readLine();
            String segundaLinha = reader.readLine();

            if (primeiraLinha == null) {
                log.warn("Arquivo vazio ou sem cabeçalho.");
                throw new ArquivoInvalidoException("Arquivo vazio ou sem cabeçalho.");
            }

            if (!primeiraLinha.startsWith(HEADER_PREFIX_017) && !primeiraLinha.startsWith(HEADER_PREFIX_006)) {
                String message = "Cabeçalho inválido. A primeira linha deve iniciar com \"" +
                        HEADER_PREFIX_017 + "\" ou \"" + HEADER_PREFIX_006 + "\".";
                log.warn(message);
                throw new ArquivoInvalidoException(message);
            }

            if (segundaLinha == null || !SEGUNDA_LINHA_ESPERADA.equals(segundaLinha.trim())) {
                String message = "Segunda linha inválida. Esperado: \"" + SEGUNDA_LINHA_ESPERADA + "\".";
                log.warn(message);
                throw new ArquivoInvalidoException(message);
            }
        }
    }

    @Override
    public Mono<ProgressoResponse> consultarProgresso(String id) {
        log.info("Consultando progresso: {}", id);
        return Mono.fromCallable(() -> {
            UUID uploadId = parseUUID(id);
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new UploadNaoEncontradoException(id));
            log.info("Consultando progresso: {}", uploadId);
            return new ProgressoResponse(upload.getStatus());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ResultadoResponse> consultarResultado(String id) {
        log.info("Consultando resultado: {}", id);
        return Mono.fromCallable(() -> {
            UUID uploadId = parseUUID(id);
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new UploadNaoEncontradoException(id));

            log.info("Consultando resultado: {}", uploadId);
            if (upload.getStatus() == StatusProcessamento.EM_PROCESSAMENTO) {
                throw new ProcessamentoEmAndamentoException();
            }

            List<ResumoItem> resumo = resumoRepository.findByUploadId(uploadId)
                    .stream()
                    .map(r -> new ResumoItem(r.getRegistro(), r.getTotal()))
                    .toList();

            ResultadoResponse resultadoResponse = new ResultadoResponse(upload.getStatus(), resumo);
            log.info("Consultando resultado: {}", resultadoResponse);
            return resultadoResponse;
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private UUID parseUUID(String id) {
        try {
            log.info("Tentando converter em UUID: {}", id);
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Nao foi possivel converter em UUID: {}", e.getMessage());
            throw new UploadNaoEncontradoException(id);
        }
    }
}
