package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ArquivoUseCase;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ProgressoResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ResultadoResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.UploadResponseDTO;
import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import br.com.alexandreluchetti.mibackend.core.model.Upload;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class ArquivoUseCaseImpl implements ArquivoUseCase {

    private static final String HEADER_PREFIX_017 = "|0000|017|";
    private static final String HEADER_PREFIX_006 = "|0000|006|";
    private static final String SEGUNDA_LINHA_ESPERADA = "|0001|0|";

    private final UploadRepository uploadRepository;
    private final ResumoRepository resumoRepository;
    private final ProcessamentoUseCase processamentoUseCase;

    public ArquivoUseCaseImpl(UploadRepository uploadRepository,
                              ResumoRepository resumoRepository,
                              ProcessamentoUseCase processamentoUseCase) {
        this.uploadRepository = uploadRepository;
        this.resumoRepository = resumoRepository;
        this.processamentoUseCase = processamentoUseCase;
    }

    @Override
    public UploadResponseDTO upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new ArquivoInvalidoException("Arquivo não enviado ou vazio.");
        }

        InputStream inputStream = file.getInputStream();

        // Marca o início para poder reler após validação (se o stream suportar)
        // Como MultipartFile pode usar temp file ou memória, usamos BufferedReader
        // e relemos somente as 2 primeiras linhas para validação.
        // Depois passamos o stream restante para o processamento.
        // Porém, após consumir o InputStream, precisamos de um novo.
        // Solução: ler a validação primeiro, depois pegar novo InputStream.

        validarCabecalho(inputStream);

        // Pega um novo InputStream para passar ao processamento
        InputStream streamParaProcessamento = file.getInputStream();

        UUID uploadId = uploadRepository.save();
        processamentoUseCase.processar(uploadId, streamParaProcessamento);

        return new UploadResponseDTO(uploadId);
    }

    /**
     * Valida as 2 primeiras linhas do arquivo.
     * Lê o mínimo necessário — sem carregar o arquivo inteiro em memória.
     */
    private void validarCabecalho(InputStream inputStream) throws IOException {
        try (var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
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
    public ProgressoResponseDTO consultarProgresso(String id) {
        UUID uploadId = parseUUID(id);
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new UploadNaoEncontradoException(id));
        return new ProgressoResponseDTO(upload.getStatus());
    }

    @Override
    public ResultadoResponseDTO consultarResultado(String id) {
        UUID uploadId = parseUUID(id);
        Upload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new UploadNaoEncontradoException(id));

        if (upload.getStatus() == StatusProcessamento.EM_PROCESSAMENTO) {
            throw new ProcessamentoEmAndamentoException();
        }

        List<ResultadoResponseDTO.ResumoItemDTO> resumo = resumoRepository.findByUploadId(uploadId)
                .stream()
                .map(r -> new ResultadoResponseDTO.ResumoItemDTO(r.getRegistro(), r.getTotal()))
                .toList();

        return new ResultadoResponseDTO(upload.getStatus(), resumo);
    }

    private UUID parseUUID(String id) {
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            throw new UploadNaoEncontradoException(id);
        }
    }
}
