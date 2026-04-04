package br.com.alexandreluchetti.mibackend.core.usecase;

import br.com.alexandreluchetti.mibackend.core.model.ProgressoResponse;
import br.com.alexandreluchetti.mibackend.core.model.ResultadoResponse;
import br.com.alexandreluchetti.mibackend.core.model.UploadResponse;
import org.springframework.http.codec.multipart.FilePart;
import reactor.core.publisher.Mono;

public interface ArquivoUseCase {

    /**
     * Valida o cabeçalho do arquivo e, se válido, registra o upload e dispara
     * o processamento em background. O Stream é consumido para o disco via NIO de forma não-bloqueante.
     */
    Mono<UploadResponse> upload(FilePart file);

    Mono<ProgressoResponse> consultarProgresso(String id);

    Mono<ResultadoResponse> consultarResultado(String id);
}
