package br.com.alexandreluchetti.mibackend.core.model;

public class ResumoItem {

    private String registro;
    private Long total;

    public ResumoItem(String registro, Long total) {
        this.registro = registro;
        this.total = total;
    }

    public String getRegistro() {
        return registro;
    }

    public Long getTotal() {
        return total;
    }
}
