package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import br.com.alexandreluchetti.mibackend.core.model.Upload;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class UploadRepositoryImpl implements UploadRepository {

    private final JdbcClient jdbcClient;

    public UploadRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID save() {
        return jdbcClient
                .sql("INSERT INTO upload (status) VALUES ('EM_PROCESSAMENTO') RETURNING id")
                .query(UUID.class)
                .single();
    }

    @Override
    public Optional<Upload> findById(UUID id) {
        return jdbcClient
                .sql("SELECT id, status, created_at FROM upload WHERE id = :id")
                .param("id", id)
                .query((rs, rowNum) -> Upload.builder()
                        .id(UUID.fromString(rs.getString("id")))
                        .status(StatusProcessamento.valueOf(rs.getString("status")))
                        .createdAt(rs.getObject("created_at", java.time.OffsetDateTime.class))
                        .build())
                .optional();
    }

    @Override
    public void updateStatus(UUID id, StatusProcessamento status) {
        jdbcClient
                .sql("UPDATE upload SET status = :status WHERE id = :id")
                .param("status", status.name())
                .param("id", id)
                .update();
    }
}
