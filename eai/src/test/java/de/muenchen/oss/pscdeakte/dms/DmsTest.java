package de.muenchen.oss.pscdeakte.dms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import de.muenchen.oss.pscdeakte.TestConstants;
import de.muenchen.oss.pscdeakte.data.PscdData;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles(TestConstants.SPRING_TEST_PROFILE)
class DmsTest extends WiremockTest {

    @Autowired
    public DmsService dmsService;

    @Test
    void getApentriesTest() {
        ReadApentryAntwortDTO response = dmsService.getApentries();
        response.getGiobjecttype().forEach(System.out::println);
        assertEquals(1, response.getGiobjecttype().size());
        assertEquals("COO.2150.8819.2.1120806", response.getGiobjecttype().getFirst().getObjaddress());
    }

    @Test
    void createSubjectAreaUnitTest() {
        DmsObjektResponse response = dmsService.createSubjectAreaUnit(999, "9000000001-9000005000");
        System.out.println(response.toString());
        assertEquals("9512.999/9000000001-9000005000", response.getObjname());
    }

    @Test
    void createFileTest() {
        PscdData data = new PscdData();
        data.setGpId("9000000001");
        data.setName("Gebdat ohne Zeit");
        data.setVorname("Gebdat ohne Zeit");
        data.setGebDat("31.01.3210");
        data.setZentralaktkennung("9999");
        DmsObjektResponse response = dmsService.createFile(data, "COO.2150.8819.2.1120806");
        System.out.println(response.toString());
        assertEquals("9512.999-9000000001-1", response.getObjname());
    }

    @Test
    void createProcedureBestandsakteTest() {
        DmsObjektResponse response = dmsService.createProcedureBestandsakte("COO.2150.8819.2.1120843");
        System.out.println(response.toString());
        assertEquals("Bestandsakten (9512.999-1-0001)", response.getObjname());
    }

    @Test
    void createProcedureAVTest() {
        DmsObjektResponse response = dmsService.createProcedureAV("COO.2150.8819.2.1120843");
        System.out.println(response.toString());
        assertEquals("AVs, Titel, Haftbefehle (9512.999-1-0002)", response.getObjname());
    }
}
