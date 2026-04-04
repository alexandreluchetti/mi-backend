package br.com.alexandreluchetti.mibackend.core.usecase.impl;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcessamentoService")
class ProcessamentoUseCaseImplTest {

    @Mock
    private UploadRepository uploadRepository;

    @Mock
    private ResumoRepository resumoRepository;

    @InjectMocks
    private ProcessamentoUseCaseImpl processamentoUseCaseImpl;

    private static final UUID UPLOAD_ID = UUID.randomUUID();

    private Path createTempFile(String content) throws IOException {
        Path tempFile = Files.createTempFile("teste", ".txt");
        Files.writeString(tempFile, content);
        return tempFile;
    }

    private void waitAwhile() {
        try {
            Thread.sleep(500); // aguardar o boundedElastic completar assincronamente
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ================================================================== //
    //  Extração de código de registro                                     //
    // ================================================================== //
    @Nested
    @DisplayName("Contagem de registros")
    class ContagemTests {

        @Test
        @DisplayName("deve contar corretamente registros agrupados por código")
        void deveContarRegistrosCorretamente() throws IOException {
            String conteudo = "|0000|017|HEADER\n|0001|0|\n|1000|A|\n|1000|B|\n|2000|C|\n|9999|TRAILER|";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
            verify(resumoRepository).saveAll(eq(UPLOAD_ID), captor.capture());

            Map<String, Long> contagens = captor.getValue();
            assertThat(contagens).containsEntry("0000", 1L)
                                 .containsEntry("0001", 1L)
                                 .containsEntry("1000", 2L)
                                 .containsEntry("2000", 1L)
                                 .containsEntry("9999", 1L);
        }

        @Test
        @DisplayName("deve processar arquivo com uma única linha de dados")
        void deveProcessarArquivoComUmaLinha() throws IOException {
            String conteudo = "|0000|006|HEADER\n|0001|0|\n|3000|Dado|";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
            verify(resumoRepository).saveAll(eq(UPLOAD_ID), captor.capture());

            assertThat(captor.getValue()).containsEntry("3000", 1L);
        }

        @Test
        @DisplayName("deve ignorar linhas em branco no meio do arquivo")
        void deveIgnorarLinhasEmBranco() throws IOException {
            String conteudo = "|0000|017|HEADER\n\n|0001|0|\n\n|5000|X|\n\n";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
            verify(resumoRepository).saveAll(eq(UPLOAD_ID), captor.capture());

            Map<String, Long> contagens = captor.getValue();
            assertThat(contagens).doesNotContainKey("");
            assertThat(contagens).doesNotContainKey(null);
        }

        @Test
        @DisplayName("deve processar linha sem pipe inicial corretamente")
        void deveProcessarLinhaSemPipeInicial() throws IOException {
            String conteudo = "0000|017|HEADER\n|0001|0|\n|1000|foo|";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            @SuppressWarnings("unchecked")
            ArgumentCaptor<Map<String, Long>> captor = ArgumentCaptor.forClass(Map.class);
            verify(resumoRepository).saveAll(eq(UPLOAD_ID), captor.capture());

            assertThat(captor.getValue()).containsEntry("0000", 1L);
        }
    }

    // ================================================================== //
    //  Status de processamento                                            //
    // ================================================================== //
    @Nested
    @DisplayName("Status após processamento")
    class StatusTests {

        @Test
        @DisplayName("deve marcar upload como FINALIZADO_COM_SUCESSO após processamento normal")
        void deveMarcaStatusFinalizado() throws IOException {
            String conteudo = "|0000|017|HEADER\n|0001|0|\n|1000|X|";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            verify(uploadRepository).updateStatus(UPLOAD_ID, StatusProcessamento.FINALIZADO_COM_SUCESSO);
        }

        @Test
        @DisplayName("deve marcar upload como FINALIZADO_COM_SUCESSO mesmo para arquivo válido mínimo")
        void deveMarcaStatusFinalizadoArquivoMinimo() throws IOException {
            String conteudo = "|0000|017|X\n|0001|0|";
            Path file = createTempFile(conteudo);
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, file);
            waitAwhile();

            verify(uploadRepository).updateStatus(UPLOAD_ID, StatusProcessamento.FINALIZADO_COM_SUCESSO);
        }

        @Test
        @DisplayName("deve marcar upload como FINALIZADO_COM_ERROS quando Path for inválido/inexistente")
        void deveMarcaStatusErroQuandoIOException() {
            Path fileInexistente = Paths.get("/caminho/nao/existe");
            
            processamentoUseCaseImpl.processar(UPLOAD_ID, fileInexistente);
            waitAwhile();

            verify(uploadRepository).updateStatus(UPLOAD_ID, StatusProcessamento.FINALIZADO_COM_ERROS);
        }
    }
}
