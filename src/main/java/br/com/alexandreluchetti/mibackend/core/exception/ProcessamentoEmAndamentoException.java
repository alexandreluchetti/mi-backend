package br.com.alexandreluchetti.mibackend.core.exception;

public class ProcessamentoEmAndamentoException extends RuntimeException {

    public ProcessamentoEmAndamentoException() {
        super("Arquivo ainda em processamento. Consulte o endpoint de progresso.");
    }
}
