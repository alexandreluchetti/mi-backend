package br.com.alexandreluchetti.mibackend.core.model;

public class ProgressoResponse {

    private StatusProcessamento status;

    public ProgressoResponse(StatusProcessamento status) {
        this.status = status;
    }

    public StatusProcessamento getStatus() {
        return status;
    }
}
