package br.com.alexandreluchetti.mibackend.exception;

public class UploadNaoEncontradoException extends RuntimeException {

    public UploadNaoEncontradoException(String id) {
        super("Upload não encontrado para o ID: " + id);
    }
}
