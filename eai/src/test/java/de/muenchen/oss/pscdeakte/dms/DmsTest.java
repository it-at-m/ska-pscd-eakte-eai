package de.muenchen.oss.pscdeakte.dms;

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
class DmsTest {

    @Autowired
    public DmsService dmsService;

    @Test
    void getApentriesTest(){
       ReadApentryAntwortDTO response = dmsService.getApentries();
       response.getGiobjecttype().forEach(System.out::println);
    }

    @Test
    void createSubjectAreaUnitTest(){
        DmsObjektResponse response = dmsService.createSubjectAreaUnit(999, "9000000001-9000005000");
        System.out.println(response.toString());
    }

    @Test
    void createFileTest(){
        PscdData data = new PscdData();
        data.setGpId("9000000003");
        data.setName("Gebdat ohne Zeit");
        data.setVorname("Gebdat ohne Zeit");
        data.setGebDat("31.01.3210");
        data.setZentralaktkennung("9999");
        DmsObjektResponse response = dmsService.createFile(data, "COO.2150.8819.2.1120806");
        System.out.println(response.toString());
    }

    @Test
    void createProcedureBestandsakteTest(){
        DmsObjektResponse response = dmsService.createProcedureBestandsakte("COO.2150.8819.2.1120843");
        System.out.println(response.toString());
    }

    @Test
    void createProcedureAVTest(){
        DmsObjektResponse response = dmsService.createProcedureAV("COO.2150.8819.2.1120843");
        System.out.println(response.toString());
    }
}
