package br.com.alexandreluchetti.mibackend.dataprovider.repository;

import br.com.alexandreluchetti.mibackend.core.model.ResumoItem;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({ResumoRepositoryImpl.class, UploadRepositoryImpl.class})
class ResumoRepositoryImplTest {

    @Autowired
    private ResumoRepositoryImpl resumoRepository;

    @Autowired
    private UploadRepositoryImpl uploadRepository;

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
        
        // "0000" should be before "1000" in alphabetical order
        resumos.sort((a,b) -> a.getRegistro().compareTo(b.getRegistro()));

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
