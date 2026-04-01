package br.com.alexandreluchetti.mibackend.entrypoint.dto;

import br.com.alexandreluchetti.mibackend.core.model.ResumoItem;

public record ResumoItemDTO(String registro, Long total) {

    static ResumoItemDTO fromModel(ResumoItem resumoItem) {
        return new ResumoItemDTO(resumoItem.getRegistro(), resumoItem.getTotal());
    }

}