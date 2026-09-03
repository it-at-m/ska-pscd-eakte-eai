package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DuplicateOrUpdateTest {

    @Test
    void testDuplicate(){
        final PscdImport psi = new PscdImport();
        final DuplicateOrUpdate dou = new DuplicateOrUpdate(psi, psi);
        Assertions.assertTrue(dou.isDuplicate());
        Assertions.assertFalse(dou.isUpdate());
    }

    @Test
    void testUpdate(){
        final PscdImport org = getPscdImport();

        final PscdImport second = new PscdImport();
        second.setName("nameNeu");
        second.setVorname("vornameNeu");
        second.setGeburtsdatum("geburtsdatumNeu");
        second.setZentralakt("zentralaktNeu");

        final DuplicateOrUpdate dou = new DuplicateOrUpdate(org, second);
        Assertions.assertTrue(dou.isUpdate());
        final PscdImport updated = dou.createUpdatedPscdImport();
        Assertions.assertEquals(second.getVorname(), updated.getVorname());
        Assertions.assertEquals(org.getId(), updated.getId());
    }

    private static @NonNull PscdImport getPscdImport() {
        final PscdImport org = new PscdImport();
        org.setAkte("akte");
        org.setAv("av");
        org.setBestandsakt("bestandsakt");
        org.setBetreffseinheit("bestreffseinheit");
        org.setGeburtsdatum("geburtsdatum");
        org.setGeschaeftspartnerId("geschaeftspartnerId");
        org.setId(1234);
        org.setName("name");
        org.setStatus(DatensatzStatus.DONE);
        org.setStatustext(DatensatzStatus.DONE.getValue());
        org.setVorname("vorname");
        org.setZentralakt("zentralakt");
        return org;
    }
}
