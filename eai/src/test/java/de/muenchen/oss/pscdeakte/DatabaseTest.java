package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.apache.camel.test.spring.junit5.CamelSpringBootTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(classes = {Application.class})
@CamelSpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
public class DatabaseTest {

    private final PscdImportRepository pir;

    @Autowired
    public DatabaseTest(PscdImportRepository pir) {
        this.pir = pir;
    }

    @Test
    void test_pscdimport_crud() {

        PscdImport pscdImport = new PscdImport();
        pscdImport.setName("name");
        pscdImport.setVorname("vorname");
        pir.save(pscdImport);

        Iterable<PscdImport> imports = pir.findAll();
        assertTrue(imports.iterator().hasNext(), "Insert and find should work");
        PscdImport selected = imports.iterator().next();
        assertEquals("name", selected.getName());
        selected.setName("updateName");

        pir.save(selected);
        Integer id = selected.getId();
        Optional<PscdImport> updated = pir.findById(id);

        updated.ifPresentOrElse(u -> assertEquals("updateName", u.getName()), () -> fail("Update failed"));

        pir.delete(updated.get());

        imports = pir.findAll();

        assertFalse(imports.iterator().hasNext(), "Delete failed");

    }

}
