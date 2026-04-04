package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.model.StatusProcessamento;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(UploadRepositoryImpl.class)
class UploadRepositoryImplTest {

    @Autowired
    private UploadRepositoryImpl uploadRepository;

    @Test
    void testSaveAndFindById() {
        // Testa o save
        UUID id = uploadRepository.save();
        assertNotNull(id);

        // Testa o findById
        var upload = uploadRepository.findById(id);
        assertTrue(upload.isPresent());
        assertEquals(StatusProcessamento.EM_PROCESSAMENTO, upload.get().getStatus());
        assertNotNull(upload.get().getCreatedAt());
    }

    @Test
    void testUpdateStatus() {
        UUID id = uploadRepository.save();
        
        uploadRepository.updateStatus(id, StatusProcessamento.FINALIZADO_COM_SUCESSO);

        var upload = uploadRepository.findById(id);
        assertTrue(upload.isPresent());
        assertEquals(StatusProcessamento.FINALIZADO_COM_SUCESSO, upload.get().getStatus());
    }

    @Test
    void testFindById_NotFound() {
        var upload = uploadRepository.findById(UUID.randomUUID());
        assertFalse(upload.isPresent());
    }
}
