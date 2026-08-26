package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Component
@Log4j2
public class DbToEakte {

    private final PscdImportRepository repo;
    private final DBLogger dbLog;
    private final DmsService dmsService;
    private final Apentries apentries;

    public void start() {
        repo.streamAllByStatusIsNot(DatensatzStatus.DONE).forEach(this::process);
    }

    public void process(final PscdImport data) {
        log.info("Processing {}", data.getGeschaeftspartnerId());
        try {
            datensatzVerarbeitung(data);
        } catch (WebClientResponseException e) {
            //        TODO Fehlerhandling der eAkte
            dbLog.log("error", "WebclientResponseException", e.getMessage());
        } finally {
            repo.save(data);
        }
    }

    private void datensatzVerarbeitung(final PscdImport data) {
        switch (data.getStatus()) {
        case DatensatzStatus.NEW:
            this.log(data, DatensatzStatus.STARTED);
            //              fallthrough
        case DatensatzStatus.STARTED:
            data.setBetreffseinheit(apentries.getApentryCoo(data.getGeschaeftspartnerId()));
            this.log(data, DatensatzStatus.APENTRY_EXISTS);
            //              fallthrough
        case DatensatzStatus.APENTRY_EXISTS:
            data.setAkte(dmsService.createFile(data).getObjid());
            this.log(data, DatensatzStatus.FILE_CREATED);
            //              fallthrough
        case DatensatzStatus.FILE_CREATED:
            data.setBestandsakt(dmsService.createProcedureBestandsakte(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.BESTANDSAKT_CREATED);
            //              fallthrough
        case DatensatzStatus.BESTANDSAKT_CREATED:
            data.setAv(dmsService.createProcedureAV(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.DONE);
            //              fallthrough
        case DatensatzStatus.DUPLICATE:
            //                TODO Update Funktion fuer Akte
            dbLog.log("info", "Geschaeftspartner " + data.getGeschaeftspartnerId() + " mehrfach vorhanden", null);
            break;
        case DatensatzStatus.ARCHIVE:
            //               TODO personenbezogene Daten entfernen
            break;
        case DatensatzStatus.ERROR:
            dbLog.log("error", "GpId " + data.getGeschaeftspartnerId() + "steht auf ERROR.", null);
            break;
        default:
            log.warn("Status steht auf {}", data.getStatus().getValue());
        }
    }

    private void log(final PscdImport data, final DatensatzStatus status) {
        data.setStatus(status);
        data.setStatustext(status.getValue());
        log.info(status.getValue());
    }

}
