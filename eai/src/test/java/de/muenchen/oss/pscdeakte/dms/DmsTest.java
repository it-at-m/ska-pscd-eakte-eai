package de.muenchen.oss.pscdeakte.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.pscdeakte.TestConstants;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
@Disabled
class DmsTest extends WiremockTest {

    @Autowired
    public DmsService dmsService;

    @Test
    void getApentriesTest() {
        ReadApentryAntwortDTO response = dmsService.getApentries();
        response.getGiobjecttype().forEach(System.out::println);
        assertEquals(1, response.getGiobjecttype().size());
        assertEquals("COO.2150.8819.2.1195874", response.getGiobjecttype().getFirst().getObjaddress());
    }

    @Test
    void createSubjectAreaUnitTest() {
        DmsObjektResponse response = dmsService.createSubjectAreaUnit(199999, "1999995001-2000000000");
        System.out.println(response.toString());
        assertEquals("9512.199999/1999995001-2000000000", response.getObjname());
    }

    @Test
    void createFileTest() {
        PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId("2000000000");
        data.setName("s3testname");
        data.setVorname("s3testvorname");
        data.setGeburtsdatum("01.02.2012");
        data.setZentralakt("1234");
        data.setBetreffseinheit("COO.2150.8819.2.1195874");
        DmsObjektResponse response = dmsService.createFile(data);
        System.out.println(response.toString());
        assertEquals("9512.199999-2000000000-1", response.getObjname());
    }

    @Test
    void createProcedureBestandsakteTest() {
        DmsObjektResponse response = dmsService.createProcedureBestandsakte("COO.2150.8819.2.1195876");
        System.out.println(response.toString());
        assertEquals("Bestandsakten (9512.199999-1-0001)", response.getObjname());
    }

    @Test
    void createProcedureAVTest() {
        DmsObjektResponse response = dmsService.createProcedureAV("COO.2150.8819.2.1195876");
        System.out.println(response.toString());
        assertEquals("AVs, Titel, Haftbefehle (9512.199999-1-0002)", response.getObjname());
    }
}
