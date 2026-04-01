package br.com.alexandreluchetti.mibackend.core.usecase;

import java.io.InputStream;
import java.util.UUID;

public interface ProcessamentoUseCase {

    /**
     * Processa o arquivo em background, linha a linha, sem carregar tudo em memória.
     * Usa BufferedReader com stream de linhas para suportar arquivos de até 1 GB.
     *
     * @param uploadId  ID do upload registrado no banco
     * @param inputStream InputStream do arquivo (sem copiar para disco)
     */
    void processar(UUID uploadId, InputStream inputStream);
}
