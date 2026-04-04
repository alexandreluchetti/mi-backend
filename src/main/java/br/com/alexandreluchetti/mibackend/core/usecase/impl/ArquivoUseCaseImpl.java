package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.model.*;
import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ArquivoUseCase;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
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

    private static final String HEADER_PREFIX_017 = "|0000|017|";
    private static final String HEADER_PREFIX_006 = "|0000|006|";
    private static final String SEGUNDA_LINHA_ESPERADA = "|0001|0|";
    private static final String UPLOADS_DIR = ".uploads";

    private final UploadRepository uploadRepository;
    private final ResumoRepository resumoRepository;
    private final ProcessamentoUseCase processamentoUseCase;

    public ArquivoUseCaseImpl(UploadRepository uploadRepository,
                              ResumoRepository resumoRepository,
                              ProcessamentoUseCase processamentoUseCase) {
        this.uploadRepository = uploadRepository;
        this.resumoRepository = resumoRepository;
        this.processamentoUseCase = processamentoUseCase;
        
        try {
            Files.createDirectories(Paths.get(UPLOADS_DIR));
        } catch (IOException e) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads", e);
        }
    }

    @Override
    public Mono<UploadResponse> upload(FilePart file) {
        if (file == null || file.filename().isEmpty()) {
            return Mono.error(new ArquivoInvalidoException("Arquivo não enviado ou vazio."));
        }

        return Mono.fromCallable(uploadRepository::save)
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(uploadId -> {
                    Path filePath = Paths.get(UPLOADS_DIR, uploadId.toString() + ".txt");
                    
                    return file.transferTo(filePath)
                            .then(Mono.fromCallable(() -> {
                                validarCabecalho(filePath);
                                return uploadId;
                            })).onErrorResume(e -> {
                                try {
                                    Files.deleteIfExists(filePath);
                                } catch (IOException ignored) {}
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
                throw new ArquivoInvalidoException("Arquivo vazio ou sem cabeçalho.");
            }

            if (!primeiraLinha.startsWith(HEADER_PREFIX_017) && !primeiraLinha.startsWith(HEADER_PREFIX_006)) {
                throw new ArquivoInvalidoException(
                        "Cabeçalho inválido. A primeira linha deve iniciar com \"" +
                        HEADER_PREFIX_017 + "\" ou \"" + HEADER_PREFIX_006 + "\".");
            }

            if (segundaLinha == null || !SEGUNDA_LINHA_ESPERADA.equals(segundaLinha.trim())) {
                throw new ArquivoInvalidoException(
                        "Segunda linha inválida. Esperado: \"" + SEGUNDA_LINHA_ESPERADA + "\".");
            }
        }
    }

    @Override
    public Mono<ProgressoResponse> consultarProgresso(String id) {
        return Mono.fromCallable(() -> {
            UUID uploadId = parseUUID(id);
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new UploadNaoEncontradoException(id));
            return new ProgressoResponse(upload.getStatus());
        }).subscribeOn(Schedulers.boundedElastic());
    }

    @Override
    public Mono<ResultadoResponse> consultarResultado(String id) {
        return Mono.fromCallable(() -> {
            UUID uploadId = parseUUID(id);
            Upload upload = uploadRepository.findById(uploadId)
                    .orElseThrow(() -> new UploadNaoEncontradoException(id));

            if (upload.getStatus() == StatusProcessamento.EM_PROCESSAMENTO) {
                throw new ProcessamentoEmAndamentoException();
            }

            List<ResumoItem> resumo = resumoRepository.findByUploadId(uploadId)
                    .stream()
                    .map(r -> new ResumoItem(r.getRegistro(), r.getTotal()))
                    .toList();

            return new ResultadoResponse(upload.getStatus(), resumo);
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new UploadNaoEncontradoException(id);
        }
    }
}
