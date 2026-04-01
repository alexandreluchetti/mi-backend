package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.model.ResumoRegistro;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
public class ResumoRepositoryImpl implements ResumoRepository {

    private final JdbcClient jdbcClient;

    public ResumoRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void saveAll(UUID uploadId, Map<String, Long> contagens) {
        for (Map.Entry<String, Long> entry : contagens.entrySet()) {
            jdbcClient
                    .sql("INSERT INTO resumo_registro (upload_id, registro, total) VALUES (:uploadId, :registro, :total)")
                    .param("uploadId", uploadId)
                    .param("registro", entry.getKey())
                    .param("total", entry.getValue())
                    .update();
        }
    }

    @Override
    public List<ResumoRegistro> findByUploadId(UUID uploadId) {
        return jdbcClient
                .sql("SELECT id, upload_id, registro, total FROM resumo_registro WHERE upload_id = :uploadId ORDER BY registro")
                .param("uploadId", uploadId)
                .query((rs, rowNum) -> ResumoRegistro.builder()
                        .id(rs.getLong("id"))
                        .uploadId(UUID.fromString(rs.getString("upload_id")))
                        .registro(rs.getString("registro"))
                        .total(rs.getLong("total"))
                        .build())
                .list();
    }
}
