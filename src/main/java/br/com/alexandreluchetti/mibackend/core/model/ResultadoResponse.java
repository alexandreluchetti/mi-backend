package br.com.alexandreluchetti.mibackend.core.model;

import java.util.List;

public class ResultadoResponse {

    private StatusProcessamento status;
    private List<ResumoItem> resumo;

    public ResultadoResponse(StatusProcessamento status, List<ResumoItem> resumo) {
        this.status = status;
        this.resumo = resumo;
    }

    public StatusProcessamento getStatus() {
        return status;
    }

    public List<ResumoItem> getResumo() {
        return resumo;
    }
}
