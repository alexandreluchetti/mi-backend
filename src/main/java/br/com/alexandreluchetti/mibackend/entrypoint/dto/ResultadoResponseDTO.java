package br.com.alexandreluchetti.mibackend.entrypoint.dto;

import br.com.alexandreluchetti.mibackend.core.model.ResultadoResponse;
import br.com.alexandreluchetti.mibackend.core.model.ResumoItem;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;

import java.util.List;

public record ResultadoResponseDTO(
        StatusProcessamento status,
        List<ResumoItemDTO> resumo
) {

    public static ResultadoResponseDTO fromModel(ResultadoResponse resultadoResponse) {
        return new ResultadoResponseDTO(
                resultadoResponse.getStatus(),
                resultadoResponse.getResumo().stream().map(ResumoItemDTO::fromModel).toList()
        );
    }
}
