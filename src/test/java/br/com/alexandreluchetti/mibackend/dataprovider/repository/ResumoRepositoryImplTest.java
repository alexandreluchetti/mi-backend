package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.model.ResumoRegistro;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ResumoRepositoryImpl.class, UploadRepositoryImpl.class})
class ResumoRepositoryImplTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @Autowired
    private ResumoRepositoryImpl resumoRepository;

    @Autowired
    private UploadRepositoryImpl uploadRepository; // Para poder criar a fk em upload

    @Test
    void testSaveAllAndFindByUploadId() {
        UUID uploadId = uploadRepository.save();

        var contagens = Map.of(
                "0000", 1L,
                "1000", 10L
        );

        resumoRepository.saveAll(uploadId, contagens);

        var resumos = resumoRepository.findByUploadId(uploadId);
        
        assertEquals(2, resumos.size());
        
        // Verifica ordering the result that depends on String default order: "0000" < "1000"
        assertEquals("0000", resumos.get(0).getRegistro());
        assertEquals(1L, resumos.get(0).getTotal());
        assertEquals(uploadId, resumos.get(0).getUploadId());

        assertEquals("1000", resumos.get(1).getRegistro());
        assertEquals(10L, resumos.get(1).getTotal());
    }

    @Test
    void testFindByUploadId_Empty() {
        var resumos = resumoRepository.findByUploadId(UUID.randomUUID());
        assertTrue(resumos.isEmpty());
    }
}
