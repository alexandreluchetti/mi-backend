package br.com.alexandreluchetti.mibackend.core.repository;

import br.com.alexandreluchetti.mibackend.model.StatusProcessamento;
import br.com.alexandreluchetti.mibackend.model.Upload;

import java.util.Optional;
import java.util.UUID;

public interface UploadRepository {

    UUID save();

    Optional<Upload> findById(UUID id);

    void updateStatus(UUID id, StatusProcessamento status);
}
