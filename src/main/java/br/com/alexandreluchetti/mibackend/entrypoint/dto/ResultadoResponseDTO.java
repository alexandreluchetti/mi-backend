package br.com.alexandreluchetti.mibackend.entrypoint.dto;

import br.com.alexandreluchetti.mibackend.model.StatusProcessamento;

import java.util.List;

public record ResultadoResponseDTO(
        StatusProcessamento status,
        List<ResumoItemDTO> resumo
) {
    public record ResumoItemDTO(String registro, Long total) {}
}
