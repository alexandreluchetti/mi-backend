package br.com.alexandreluchetti.mibackend.core.usecase;

import br.com.alexandreluchetti.mibackend.core.model.ProgressoResponse;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.ResultadoResponseDTO;
import br.com.alexandreluchetti.mibackend.entrypoint.dto.UploadResponseDTO;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface ArquivoUseCase {

    /**
     * Valida o cabeçalho do arquivo e, se válido, registra o upload e dispara
     * o processamento em background. O InputStream é lido apenas uma vez.
     */
    UploadResponseDTO upload(MultipartFile file) throws IOException;

    ProgressoResponse consultarProgresso(String id);

    ResultadoResponseDTO consultarResultado(String id);
}
