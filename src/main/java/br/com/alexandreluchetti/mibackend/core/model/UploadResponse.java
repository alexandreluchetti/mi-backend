package br.com.alexandreluchetti.mibackend.core.model;

import java.util.UUID;

public class UploadResponse {

    private UUID id;

    public UploadResponse(UUID id) {
        this.id = id;
    }

    public UUID getId() {
        return id;
    }
}
