package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.pscdeakte.configuration.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Service
@Slf4j
public class DbToEakteService {

    public static final String ERROR = "error";
    private final PscdImportRepository repo;
    private final DBLogger dbLog;
    private final DmsService dmsService;
    private final Apentries apentries;

    @LogExecutionTime
    public void start() {
        log.info("Starting DB To Eakte");
        final List<PscdImport> pscdImports = repo.streamAllByStatusIsNot(DatensatzStatus.DONE);
        log.info("{} Datensätze zur Verarbeitung vorhanden. ", pscdImports.size());
        pscdImports.forEach(this::process);
        log.info("Finished DB To Eakte");
    }

    @LogExecutionTime
    protected void process(final PscdImport data) {
        log.debug("Processing {}", data.getGeschaeftspartnerId());
        try {
            datensatzVerarbeitung(data);
        } catch (WebClientResponseException e) {
            dbLog.log(ERROR, "Exception aus der eAkte: WebclientResponseException", e.getMessage());
        } catch (IllegalStateException e) {
            dbLog.log(ERROR, "Timeout in der eAkte", e.getMessage());
        } catch (Exception e) {
            dbLog.log(ERROR, "Exception beim Schreiben in eAkte", e.getMessage());
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
            break;
        case DatensatzStatus.UPDATE:
            dbLog.log("info", "Gp " + data.getGeschaeftspartnerId() + " hat neue Daten. -> update", null);
            dmsService.updateFile(data);
            this.log(data, DatensatzStatus.DONE);
            break;
        case DatensatzStatus.ARCHIVE:
            // personenbezogene Daten entfernen wird vom Projekt nicht erwartet.
            break;
        case DatensatzStatus.ERROR:
            dbLog.log(ERROR, "GpId " + data.getGeschaeftspartnerId() + "steht auf ERROR.", null);
            break;
        default:
            log.warn("Status steht auf {}", data.getStatus().getValue());
        }
    }

    private void log(final PscdImport data, final DatensatzStatus status) {
        data.setStatus(status);
        data.setStatustext(status.getValue());
        log.debug(status.getValue());
    }

}
