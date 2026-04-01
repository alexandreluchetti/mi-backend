package br.com.alexandreluchetti.mibackend.entrypoint.dto;

import br.com.alexandreluchetti.mibackend.core.model.UploadResponse;

import java.util.UUID;

public record UploadResponseDTO(UUID id) {

    public static UploadResponseDTO fromModel(UploadResponse uploadResponse) {
        return new UploadResponseDTO(uploadResponse.getId());
    }

}
