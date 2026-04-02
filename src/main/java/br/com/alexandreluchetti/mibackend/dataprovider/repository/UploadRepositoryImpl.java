package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.repository.UploadRepository;
import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import br.com.alexandreluchetti.mibackend.core.model.Upload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@PropertySource("classpath:sql.properties")
public class UploadRepositoryImpl implements UploadRepository {

    private final JdbcClient jdbcClient;

    @Value("${sql.upload.save}")
    private String saveSql;

    @Value("${sql.upload.findById}")
    private String findByIdSql;

    @Value("${sql.upload.updateStatus}")
    private String updateStatusSql;

    public UploadRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public UUID save() {
        return jdbcClient.sql(saveSql)
                .query(UUID.class)
                .single();
    }

    @Override
    public Optional<Upload> findById(UUID id) {
        return jdbcClient.sql(findByIdSql)
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
        jdbcClient.sql(updateStatusSql)
                .param("status", status.name())
                .param("id", id)
                .update();
    }
}
