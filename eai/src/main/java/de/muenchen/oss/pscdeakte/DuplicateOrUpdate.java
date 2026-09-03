package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import org.apache.commons.codec.binary.StringUtils;

import java.time.Instant;

public class DuplicateOrUpdate {

    private final PscdImport existingImport;
    private final PscdImport newImport;

    public DuplicateOrUpdate(final PscdImport existingImport, final PscdImport newImport){
        this.existingImport = existingImport;
        this.newImport = newImport;
    }

    public boolean isDuplicate(){
        return StringUtils.equals(newImport.getName(), existingImport.getName()) &&
                StringUtils.equals(newImport.getVorname(), existingImport.getVorname()) &&
                StringUtils.equals(newImport.getGeburtsdatum(), existingImport.getGeburtsdatum()) &&
                StringUtils.equals(newImport.getZentralakt(), existingImport.getZentralakt());
    }

    public boolean isUpdate(){
        return !isDuplicate();
    }

    public PscdImport createUpdatedPscdImport(){
        final PscdImport updated = new PscdImport();
        updated.setId(existingImport.getId());
        updated.setGeschaeftspartnerId(existingImport.getGeschaeftspartnerId());
        updated.setBetreffseinheit(existingImport.getBetreffseinheit());
        updated.setAkte(existingImport.getAkte());
        updated.setBestandsakt(existingImport.getBestandsakt());
        updated.setAv(existingImport.getAv());
        updated.setLastUpdate(Instant.now());

        updated.setVorname(newImport.getVorname());
        updated.setName(newImport.getName());
        updated.setGeburtsdatum(newImport.getGeburtsdatum());
        updated.setZentralakt(newImport.getZentralakt());
        updated.setStatus(DatensatzStatus.UPDATE);
        updated.setStatustext(DatensatzStatus.UPDATE.getValue());
        return updated;
    }

}
