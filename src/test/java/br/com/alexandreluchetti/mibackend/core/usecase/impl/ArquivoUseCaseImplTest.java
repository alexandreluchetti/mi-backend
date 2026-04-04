package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.model.*;
import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.usecase.ProcessamentoUseCase;
import br.com.alexandreluchetti.mibackend.core.exception.ArquivoInvalidoException;
import br.com.alexandreluchetti.mibackend.core.exception.ProcessamentoEmAndamentoException;
import br.com.alexandreluchetti.mibackend.core.exception.UploadNaoEncontradoException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ArquivoUseCase")
class ArquivoUseCaseImplTest {

    @Mock
    private UploadRepository uploadRepository;

    @Mock
    private ResumoRepository resumoRepository;

    @Mock
    private ProcessamentoUseCase processamentoUseCase;

    @InjectMocks
    private ArquivoUseCaseImpl arquivoUseCaseImpl;

    // ------------------------------------------------------------------ //
    //  Helpers                                                             //
    // ------------------------------------------------------------------ //

    private static final UUID FIXED_UUID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    private FilePart createMockFilePart(String filename, String content) {
        FilePart filePart = mock(FilePart.class);
        when(filePart.filename()).thenReturn(filename);
        when(filePart.transferTo(any(Path.class))).thenAnswer(invocation -> {
            Path path = invocation.getArgument(0);
            Files.writeString(path, content);
            return Mono.empty();
        });
        return filePart;
    }

    private FilePart validFile017(String extraLines) {
        return createMockFilePart("test.txt", "|0000|017|DADOS\n|0001|0|\n" + extraLines);
    }

    private FilePart validFile006() {
        return createMockFilePart("test.txt", "|0000|006|DADOS\n|0001|0|\n|1000|foo|\n");
    }

    // ================================================================== //
    //  upload()                                                            //
    // ================================================================== //
    @Nested
    @DisplayName("upload()")
    class UploadTests {

        @Test
        @DisplayName("deve retornar UploadResponseDTO com UUID quando cabeçalho 017 é válido")
        void deveSalvarUploadComCabecalho017() {
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(validFile017("")))
                    .assertNext(response -> {
                        assertThat(response.getId()).isEqualTo(FIXED_UUID);
                    })
                    .verifyComplete();

            verify(uploadRepository).save();
            verify(processamentoUseCase).processar(eq(FIXED_UUID), any(Path.class));
        }

        @Test
        @DisplayName("deve retornar UploadResponseDTO com UUID quando cabeçalho 006 é válido")
        void deveSalvarUploadComCabecalho006() {
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(validFile006()))
                    .assertNext(response -> {
                        assertThat(response.getId()).isEqualTo(FIXED_UUID);
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo é nulo")
        void deveLancarExcecaoParaArquivoNulo() {
            StepVerifier.create(arquivoUseCaseImpl.upload(null))
                    .expectErrorMatches(t -> t instanceof ArquivoInvalidoException &&
                            t.getMessage().contains("Arquivo não enviado ou vazio"))
                    .verify();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo está vazio")
        void deveLancarExcecaoParaArquivoVazio() {
            FilePart empty = mock(FilePart.class);
            when(empty.filename()).thenReturn("");

            StepVerifier.create(arquivoUseCaseImpl.upload(empty))
                    .expectError(ArquivoInvalidoException.class)
                    .verify();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando prefixo da 1ª linha é inválido")
        void deveLancarExcecaoParaPrimeiraLinhaInvalida() {
            FilePart file = createMockFilePart("bad.txt", "|9999|XXX|HEADER_ERRADO\n|0001|0|\n");
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(file))
                    .expectErrorMatches(t -> t instanceof ArquivoInvalidoException &&
                            t.getMessage().contains("Cabeçalho inválido"))
                    .verify();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando 2ª linha está ausente")
        void deveLancarExcecaoParaSegundaLinhaAusente() {
            FilePart file = createMockFilePart("bad.txt", "|0000|017|DADOS");
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(file))
                    .expectErrorMatches(t -> t instanceof ArquivoInvalidoException &&
                            t.getMessage().contains("Segunda linha inválida"))
                    .verify();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando 2ª linha não é '|0001|0|'")
        void deveLancarExcecaoParaSegundaLinhaErrada() {
            FilePart file = createMockFilePart("bad.txt", "|0000|017|DADOS\n|ERRADO|\n");
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(file))
                    .expectErrorMatches(t -> t instanceof ArquivoInvalidoException &&
                            t.getMessage().contains("Segunda linha inválida"))
                    .verify();
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo tem apenas cabeçalho sem 2ª linha")
        void deveLancarExcecaoParaArquivoSemConteudo() {
            FilePart file = createMockFilePart("single.txt", "|0000|017|DADOS\n");
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            StepVerifier.create(arquivoUseCaseImpl.upload(file))
                    .expectErrorMatches(t -> t instanceof ArquivoInvalidoException &&
                            t.getMessage().contains("Segunda linha inválida"))
                    .verify();
        }
    }

    // ================================================================== //
    //  consultarProgresso()                                                //
    // ================================================================== //
    @Nested
    @DisplayName("consultarProgresso()")
    class ProgressoTests {

        @Test
        @DisplayName("deve retornar ProgressoResponseDTO com status EM_PROCESSAMENTO")
        void deveRetornarStatusEmProcessamento() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.EM_PROCESSAMENTO)
                    .build();
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));

            StepVerifier.create(arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString()))
                    .assertNext(dto -> assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.EM_PROCESSAMENTO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve retornar ProgressoResponseDTO com status FINALIZADO_COM_SUCESSO")
        void deveRetornarStatusFinalizado() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.FINALIZADO_COM_SUCESSO)
                    .build();
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));

            StepVerifier.create(arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString()))
                    .assertNext(dto -> assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.FINALIZADO_COM_SUCESSO))
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não existe")
        void deveLancarExcecaoParaIdInexistente() {
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.empty());

            StepVerifier.create(arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString()))
                    .expectError(UploadNaoEncontradoException.class)
                    .verify();
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não é UUID válido")
        void deveLancarExcecaoParaIdFormatadoErrado() {
            StepVerifier.create(arquivoUseCaseImpl.consultarProgresso("nao-eh-um-uuid"))
                    .expectError(UploadNaoEncontradoException.class)
                    .verify();
        }
    }

    // ================================================================== //
    //  consultarResultado()                                                //
    // ================================================================== //
    @Nested
    @DisplayName("consultarResultado()")
    class ResultadoTests {

        @Test
        @DisplayName("deve lançar ProcessamentoEmAndamentoException quando status é EM_PROCESSAMENTO")
        void deveLancarExcecaoSeAindaProcessando() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.EM_PROCESSAMENTO)
                    .build();
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));

            StepVerifier.create(arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .expectError(ProcessamentoEmAndamentoException.class)
                    .verify();

            verifyNoInteractions(resumoRepository);
        }

        @Test
        @DisplayName("deve retornar ResultadoResponseDTO com resumo quando processamento finalizou")
        void deveRetornarResultadoFinalizado() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.FINALIZADO_COM_SUCESSO)
                    .build();
            ResumoRegistro item1 = ResumoRegistro.builder().id(1L).uploadId(FIXED_UUID).registro("0000").total(1L).build();
            ResumoRegistro item2 = ResumoRegistro.builder().id(2L).uploadId(FIXED_UUID).registro("0001").total(1L).build();
            ResumoRegistro item3 = ResumoRegistro.builder().id(3L).uploadId(FIXED_UUID).registro("1000").total(5L).build();

            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));
            when(resumoRepository.findByUploadId(FIXED_UUID)).thenReturn(List.of(item1, item2, item3));

            StepVerifier.create(arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .assertNext(dto -> {
                        assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
                        assertThat(dto.getResumo()).hasSize(3);
                        assertThat(dto.getResumo()).extracting(ResumoItem::getRegistro)
                                .containsExactly("0000", "0001", "1000");
                    })
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve retornar ResultadoResponseDTO com resumo vazio quando nenhum registro foi processado")
        void deveRetornarResumoVazioQuandoArquivoNaoTemRegistros() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.FINALIZADO_COM_SUCESSO)
                    .build();

            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));
            when(resumoRepository.findByUploadId(FIXED_UUID)).thenReturn(List.of());

            StepVerifier.create(arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .assertNext(dto -> assertThat(dto.getResumo()).isEmpty())
                    .verifyComplete();
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não existe")
        void deveLancarExcecaoParaIdInexistente() {
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.empty());

            StepVerifier.create(arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .expectError(UploadNaoEncontradoException.class)
                    .verify();
        }
    }
}
