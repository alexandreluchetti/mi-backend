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
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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

    /** Constrói um MultipartFile com cabeçalho válido (prefixo 017). */
    private MockMultipartFile validFile017(String extraLines) {
        String content = "|0000|017|DADOS\n|0001|0|\n" + extraLines;
        return new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
    }

    /** Constrói um MultipartFile com cabeçalho válido (prefixo 006). */
    private MockMultipartFile validFile006() {
        String content = "|0000|006|DADOS\n|0001|0|\n|1000|foo|\n";
        return new MockMultipartFile("file", "test.txt", "text/plain", content.getBytes());
    }

    // ================================================================== //
    //  upload()                                                            //
    // ================================================================== //
    @Nested
    @DisplayName("upload()")
    class UploadTests {

        @Test
        @DisplayName("deve retornar UploadResponseDTO com UUID quando cabeçalho 017 é válido")
        void deveSalvarUploadComCabecalho017() throws IOException {
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            UploadResponse response = arquivoUseCaseImpl.upload(validFile017(""));

            // UploadResponseDTO record has field 'id'
            assertThat(response.getId()).isEqualTo(FIXED_UUID);
            verify(uploadRepository).save();
            verify(processamentoUseCase).processar(eq(FIXED_UUID), any());
        }

        @Test
        @DisplayName("deve retornar UploadResponseDTO com UUID quando cabeçalho 006 é válido")
        void deveSalvarUploadComCabecalho006() throws IOException {
            when(uploadRepository.save()).thenReturn(FIXED_UUID);

            UploadResponse response = arquivoUseCaseImpl.upload(validFile006());

            assertThat(response.getId()).isEqualTo(FIXED_UUID);
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo é nulo")
        void deveLancarExcecaoParaArquivoNulo() {
            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(null))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Arquivo não enviado ou vazio");
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo está vazio")
        void deveLancarExcecaoParaArquivoVazio() {
            MockMultipartFile empty = new MockMultipartFile("file", "vazio.txt", "text/plain", new byte[0]);

            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(empty))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando prefixo da 1ª linha é inválido")
        void deveLancarExcecaoParaPrimeiraLinhaInvalida() {
            String content = "|9999|XXX|HEADER_ERRADO\n|0001|0|\n";
            MockMultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", content.getBytes());

            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(file))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Cabeçalho inválido");

            verifyNoInteractions(uploadRepository);
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando 2ª linha está ausente")
        void deveLancarExcecaoParaSegundaLinhaAusente() {
            String content = "|0000|017|DADOS";   // sem segunda linha
            MockMultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", content.getBytes());

            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(file))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Segunda linha inválida");
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando 2ª linha não é '|0001|0|'")
        void deveLancarExcecaoParaSegundaLinhaErrada() {
            String content = "|0000|017|DADOS\n|ERRADO|\n";
            MockMultipartFile file = new MockMultipartFile("file", "bad.txt", "text/plain", content.getBytes());

            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(file))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Segunda linha inválida");
        }

        @Test
        @DisplayName("deve lançar ArquivoInvalidoException quando arquivo tem apenas cabeçalho sem 2ª linha")
        void deveLancarExcecaoParaArquivoSemConteudo() {
            String content = "|0000|017|DADOS\n";
            MockMultipartFile file = new MockMultipartFile("file", "single.txt", "text/plain", content.getBytes());

            assertThatThrownBy(() -> arquivoUseCaseImpl.upload(file))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Segunda linha inválida");
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

            ProgressoResponse dto = arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString());

            assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.EM_PROCESSAMENTO);
        }

        @Test
        @DisplayName("deve retornar ProgressoResponseDTO com status FINALIZADO_COM_SUCESSO")
        void deveRetornarStatusFinalizado() {
            Upload upload = Upload.builder()
                    .id(FIXED_UUID)
                    .status(StatusProcessamento.FINALIZADO_COM_SUCESSO)
                    .build();
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.of(upload));

            ProgressoResponse dto = arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString());

            assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não existe")
        void deveLancarExcecaoParaIdInexistente() {
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> arquivoUseCaseImpl.consultarProgresso(FIXED_UUID.toString()))
                    .isInstanceOf(UploadNaoEncontradoException.class);
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não é UUID válido")
        void deveLancarExcecaoParaIdFormatadoErrado() {
            assertThatThrownBy(() -> arquivoUseCaseImpl.consultarProgresso("nao-eh-um-uuid"))
                    .isInstanceOf(UploadNaoEncontradoException.class);
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

            assertThatThrownBy(() -> arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .isInstanceOf(ProcessamentoEmAndamentoException.class);

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

            ResultadoResponse dto = arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString());

            assertThat(dto.getStatus()).isEqualTo(StatusProcessamento.FINALIZADO_COM_SUCESSO);
            assertThat(dto.getResumo()).hasSize(3);
            assertThat(dto.getResumo()).extracting(ResumoItem::getRegistro)
                    .containsExactly("0000", "0001", "1000");
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

            ResultadoResponse dto = arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString());

            assertThat(dto.getResumo()).isEmpty();
        }

        @Test
        @DisplayName("deve lançar UploadNaoEncontradoException quando ID não existe")
        void deveLancarExcecaoParaIdInexistente() {
            when(uploadRepository.findById(FIXED_UUID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> arquivoUseCaseImpl.consultarResultado(FIXED_UUID.toString()))
                    .isInstanceOf(UploadNaoEncontradoException.class);
        }
    }
}
