package br.com.alexandreluchetti.mibackend.entrypoint.dto;

import br.com.alexandreluchetti.mibackend.core.model.ProgressoResponse;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;

public record ProgressoResponseDTO(StatusProcessamento status) {

    public static ProgressoResponseDTO fromModel(
            ProgressoResponse progressoResponse
    ) {
        return new ProgressoResponseDTO(progressoResponse.getStatus());
    }

}
