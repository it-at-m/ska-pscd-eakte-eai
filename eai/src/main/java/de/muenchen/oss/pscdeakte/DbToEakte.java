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

import java.util.List;

@RequiredArgsConstructor
@Component
@Log4j2
public class DbToEakte {

    private final PscdImportRepository repo;
    private final DBLogger dbLog;
    private final DmsService dmsService;
    private final Apentries apentries;

    public void start(){
        List<PscdImport> list = repo.streamAllByStatusIsNot(DatensatzStatus.DONE);
        list.forEach(this::process);
    }

    public void process(final PscdImport data){
        log.info("Processing {}", data.getGeschaeftspartnerId());
        try {
            datensatzVerarbeitung(data);
        } catch (WebClientResponseException e){
//        TODO Fehlerhandling der eAkte
            dbLog.log("error", "WebclientResponseException", e.getMessage());
        } finally {
            repo.save(data);
        }
    }

    private void datensatzVerarbeitung(final PscdImport data) {
        switch (data.getStatus()) {
            case DatensatzStatus.NEW:
                data.setStatus(DatensatzStatus.STARTED);
                log.info(DatensatzStatus.STARTED.getValue());
//              fallthrough
            case DatensatzStatus.STARTED:
                data.setBetreffseinheit(apentries.getApentryCoo(data.getGeschaeftspartnerId()));
                data.setStatus(DatensatzStatus.APENTRY_EXISTS);
                log.info(DatensatzStatus.APENTRY_EXISTS.getValue());
//              fallthrough
            case DatensatzStatus.APENTRY_EXISTS:
                data.setAkte(dmsService.createFile(data).getObjid());
                data.setStatus(DatensatzStatus.FILE_CREATED);
                log.info(DatensatzStatus.FILE_CREATED.getValue());
//              fallthrough
            case DatensatzStatus.FILE_CREATED:
                data.setBestandsakt(dmsService.createProcedureBestandsakte(data.getAkte()).getObjid());
                data.setStatus(DatensatzStatus.BESTANDSAKT_CREATED);
                log.info(DatensatzStatus.BESTANDSAKT_CREATED.getValue());
//              fallthrough
            case DatensatzStatus.BESTANDSAKT_CREATED:
                data.setAv(dmsService.createProcedureAV(data.getAkte()).getObjid());
                data.setStatus(DatensatzStatus.DONE);
                log.info(DatensatzStatus.DONE.getValue());
//              fallthrough
            case DatensatzStatus.DONE:
//              sollte nicht von der DB geladen werden
                log.info(DatensatzStatus.DONE.getValue());
                break;
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

}
