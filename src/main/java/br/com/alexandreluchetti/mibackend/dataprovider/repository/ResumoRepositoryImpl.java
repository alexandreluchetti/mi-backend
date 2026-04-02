package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.repository.ResumoRepository;
import br.com.alexandreluchetti.mibackend.core.model.ResumoRegistro;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Repository
@PropertySource("classpath:sql.properties")
public class ResumoRepositoryImpl implements ResumoRepository {

    private final JdbcClient jdbcClient;

    @Value("${sql.resumo.saveAll}")
    private String saveAllSql;

    @Value("${sql.resumo.findByUploadId}")
    private String findByUploadIdSql;

    public ResumoRepositoryImpl(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public void saveAll(UUID uploadId, Map<String, Long> contagens) {
        for (Map.Entry<String, Long> entry : contagens.entrySet()) {
            jdbcClient.sql(saveAllSql)
                    .param("uploadId", uploadId)
                    .param("registro", entry.getKey())
                    .param("total", entry.getValue())
                    .update();
        }
    }

    @Override
    public List<ResumoRegistro> findByUploadId(UUID uploadId) {
        return jdbcClient.sql(findByUploadIdSql)
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
