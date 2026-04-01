package br.com.alexandreluchetti.mibackend.core.repository;

import br.com.alexandreluchetti.mibackend.model.ResumoRegistro;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface ResumoRepository {

    /**
     * Persiste o mapa de contagens (registro → total) de um upload específico.
     * Usa batch insert manual para eficiência.
     */
    void saveAll(UUID uploadId, Map<String, Long> contagens);

    List<ResumoRegistro> findByUploadId(UUID uploadId);
}
