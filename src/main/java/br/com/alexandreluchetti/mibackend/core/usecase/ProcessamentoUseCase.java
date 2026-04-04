package br.com.alexandreluchetti.mibackend.core.usecase;

import java.nio.file.Path;
import java.util.UUID;

public interface ProcessamentoUseCase {

    /**
     * Processa o arquivo em background, linha a linha, sem carregar tudo em memória.
     * Usa BufferedReader com stream de linhas para suportar arquivos de até 1 GB.
     * O arquivo definitivo armazenado no Path fornecido será excluído após o fim da leitura (sucesso ou erro).
     *
     * @param uploadId  ID do upload registrado no banco
     * @param arquivo   Caminho do arquivo armazenado em disco
     */
    void processar(UUID uploadId, Path arquivo);
}
