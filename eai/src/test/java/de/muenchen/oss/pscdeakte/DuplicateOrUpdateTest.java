package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.csv.DuplicateOrUpdate;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DuplicateOrUpdateTest {

    @Test
    void testDuplicate() {
        final PscdImport psi = new PscdImport();
        final DuplicateOrUpdate dou = new DuplicateOrUpdate(psi, psi);
        Assertions.assertTrue(dou.isDuplicate());
        Assertions.assertFalse(dou.isUpdate());
    }

    @Test
    void testUpdateWhenNameDiffers() {
        final PscdImport existingImport = getPscdImport();

        final PscdImport newImport = getPscdImport();
        newImport.setName("nameNeu");

        final DuplicateOrUpdate dou = new DuplicateOrUpdate(existingImport, newImport);
        Assertions.assertFalse(dou.isDuplicate());
        Assertions.assertTrue(dou.isUpdate());
        final PscdImport updated = dou.createUpdatedPscdImport();
        Assertions.assertEquals(existingImport.getGeschaeftspartnerId(), updated.getGeschaeftspartnerId());
        Assertions.assertEquals(existingImport.getBetreffseinheit(), updated.getBetreffseinheit());
        Assertions.assertEquals(existingImport.getAkte(), updated.getAkte());
        Assertions.assertEquals(existingImport.getBestandsakt(), updated.getBestandsakt());
        Assertions.assertEquals(existingImport.getAv(), updated.getAv());
        Assertions.assertEquals(newImport.getVorname(), updated.getVorname());
        Assertions.assertEquals(newImport.getName(), updated.getName());
        Assertions.assertEquals(newImport.getGeburtsdatum(), updated.getGeburtsdatum());
        Assertions.assertEquals(newImport.getZentralakt(), updated.getZentralakt());
        Assertions.assertEquals(DatensatzStatus.UPDATE, updated.getStatus());
    }

    @Test
    void testUpdateWhenVornameDiffers() {
        final PscdImport existingImport = getPscdImport();
        final PscdImport newImport = getPscdImport();
        newImport.setVorname("vornameNeu");

        final DuplicateOrUpdate dou = new DuplicateOrUpdate(existingImport, newImport);

        Assertions.assertFalse(dou.isDuplicate());
        Assertions.assertTrue(dou.isUpdate());
    }

    @Test
    void testUpdateWhenGeburtsdatumDiffers() {
        final PscdImport existingImport = getPscdImport();
        final PscdImport newImport = getPscdImport();
        newImport.setGeburtsdatum("geburtsdatumNeu");

        final DuplicateOrUpdate dou = new DuplicateOrUpdate(existingImport, newImport);

        Assertions.assertFalse(dou.isDuplicate());
        Assertions.assertTrue(dou.isUpdate());
    }

    @Test
    void testUpdateWhenZentralaktDiffers() {
        final PscdImport existingImport = getPscdImport();
        final PscdImport newImport = getPscdImport();
        newImport.setZentralakt("zentralaktNeu");

        final DuplicateOrUpdate dou = new DuplicateOrUpdate(existingImport, newImport);

        Assertions.assertFalse(dou.isDuplicate());
        Assertions.assertTrue(dou.isUpdate());
    }

    private static @NonNull PscdImport getPscdImport() {
        final PscdImport org = new PscdImport();
        org.setAkte("akte");
        org.setAv("av");
        org.setBestandsakt("bestandsakt");
        org.setBetreffseinheit("bestreffseinheit");
        org.setGeburtsdatum("geburtsdatum");
        org.setGeschaeftspartnerId("geschaeftspartnerId");
        org.setName("name");
        org.setStatus(DatensatzStatus.DONE);
        org.setVorname("vorname");
        org.setZentralakt("zentralakt");
        return org;
    }
}
