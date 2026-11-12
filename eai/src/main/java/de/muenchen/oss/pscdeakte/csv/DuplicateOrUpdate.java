package de.muenchen.oss.pscdeakte.csv;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.codec.binary.StringUtils;

public class DuplicateOrUpdate {

    private final PscdImport existingImport;
    private final PscdImport newImport;

    public DuplicateOrUpdate(final PscdImport existingImport, final PscdImport newImport) {
        this.existingImport = existingImport;
        this.newImport = newImport;
    }

    public boolean isDuplicate() {
        return StringUtils.equals(newImport.getName(), existingImport.getName()) &&
                StringUtils.equals(newImport.getVorname(), existingImport.getVorname()) &&
                StringUtils.equals(newImport.getGeburtsdatum(), existingImport.getGeburtsdatum()) &&
                StringUtils.equals(newImport.getZentralakt(), existingImport.getZentralakt());
    }

    public boolean isUpdate() {
        return !isDuplicate();
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    // mit existingImport werden interne Daten herausgegeben.
    // Dies ist hier gewuenscht, da genau existingImport in der Datenbank landen soll.
    public PscdImport createUpdatedPscdImport() {
        existingImport.setVorname(newImport.getVorname());
        existingImport.setName(newImport.getName());
        existingImport.setGeburtsdatum(newImport.getGeburtsdatum());
        existingImport.setZentralakt(newImport.getZentralakt());
        existingImport.setStatus(DatensatzStatus.UPDATE);
        return existingImport;
    }

}
