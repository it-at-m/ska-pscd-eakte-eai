package de.muenchen.oss.pscdeakte.s3;

import de.muenchen.oss.pscdeakte.CsvToDb;
import de.muenchen.oss.pscdeakte.TestConstants;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.WiremockTest;
import de.muenchen.oss.refarch.integration.s3.domain.model.FileReference;
import java.io.File;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Slf4j
@Disabled
class S3IntegrationTest extends WiremockTest {

    private final S3Properties props;
    private final CsvToDb csvToDb;
    private final PscdImportRepository repo;

    @Autowired
    S3IntegrationTest(S3Properties props, CsvToDb csvToDb, PscdImportRepository repo) {
        this.props = props;
        this.csvToDb = csvToDb;
        this.repo = repo;
    }

    @Test
    void integratedTest() {
        final String filename = "BP_Export_Test.csv";
        final String updateFilename = "BP_Export_UpdateTest.csv";
        final FileReference fileReference = new FileReference(props.getBucket(), filename);
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().saveFile(fileReference, new File("testdata/s3/" + filename)));
        Assertions.assertDoesNotThrow(() -> csvToDb.saveFilesToDb(props.getPrefix()));
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().deleteFile(fileReference));

        final String gpId = "2000000000";
        PscdImport pi = repo.findByGeschaeftspartnerId(gpId);
        Assertions.assertNotNull(pi);
        Assertions.assertEquals(gpId, pi.getGeschaeftspartnerId());
        Assertions.assertEquals("s3testname", pi.getName());
        Assertions.assertEquals("s3testvorname", pi.getVorname());
        Assertions.assertEquals("01.02.2012", pi.getGeburtsdatum());
        Assertions.assertEquals("1234", pi.getZentralakt());

        final FileReference updateFileReference = new FileReference(props.getBucket(), updateFilename);
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().saveFile(updateFileReference, new File("testdata/s3/" + updateFilename)));
        Assertions.assertDoesNotThrow(() -> csvToDb.saveFilesToDb(props.getPrefix()));
        Assertions.assertDoesNotThrow(() -> csvToDb.getS3().deleteFile(updateFileReference));

        final PscdImport updatePi = repo.findByGeschaeftspartnerId(gpId);
        Assertions.assertNotNull(updatePi);
        Assertions.assertEquals(gpId, updatePi.getGeschaeftspartnerId());
        Assertions.assertEquals("s3testnameUpdate", updatePi.getName());
        Assertions.assertEquals("s3testvornameUpdate", updatePi.getVorname());
        Assertions.assertEquals("31.12.2012", updatePi.getGeburtsdatum());
        Assertions.assertEquals("9876", updatePi.getZentralakt());

        Assertions.assertDoesNotThrow(() -> repo.delete(updatePi));
    }
}
