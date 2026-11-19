package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.eakte.v1.generated.model.Objektreferenz;
import de.muenchen.oss.eakte.v1.generated.model.SearchFileResponseDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchProcedureResponseDTO;
import de.muenchen.oss.pscdeakte.configuration.annotation.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import de.muenchen.oss.pscdeakte.dms.ErrorHandler;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DbToEakteService {

    private final PscdImportRepository repo;
    private final DmsService dmsService;
    private final Apentries apentries;
    private final ErrorHandler errorHandler;
    private final DBLogger dbLogger;

    @LogExecutionTime
    public void start() {
        log.info("Starting DB To Eakte");
        final List<PscdImport> pscdImports = repo.streamAllByStatusIsNotOrderByCreatedTimeAsc(DatensatzStatus.DONE);
        log.info("{} Datensätze zur Verarbeitung vorhanden. ", pscdImports.size());
        if (CollectionUtils.isNotEmpty(pscdImports)) {
            apentries.initMap();
            pscdImports.forEach(this::process);
        }
        log.info("Finished DB To Eakte");
    }

    public void process(final PscdImport data) {
        log.debug("Processing {}", data.getGeschaeftspartnerId());
        try {
            datensatzVerarbeitung(data);
        } catch (Exception e) {
            errorHandler.handleException(e, data);
        } finally {
            repo.save(data);
        }
    }

    private void datensatzVerarbeitung(final PscdImport data) {
        switch (data.getStatus()) {
        case DatensatzStatus.NEW:
            this.log(data, DatensatzStatus.STARTED);
            // fallthrough
        case DatensatzStatus.STARTED:
            data.setBetreffseinheit(apentries.getApentryCoo(data.getGeschaeftspartnerId()));
            this.log(data, DatensatzStatus.APENTRY_EXISTS);
            // fallthrough
        case DatensatzStatus.APENTRY_EXISTS:
            data.setAkte(dmsService.createFile(data).getObjid());
            this.log(data, DatensatzStatus.FILE_CREATED);
            // fallthrough
        case DatensatzStatus.FILE_CREATED:
            data.setBestandsakt(dmsService.createProcedureBestandsakte(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.BESTANDSAKT_CREATED);
            // fallthrough
        case DatensatzStatus.BESTANDSAKT_CREATED:
            data.setAv(dmsService.createProcedureAV(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.DONE);
            break;
        case DatensatzStatus.UPDATE:
            dbLogger.info("GpId " + data.getGeschaeftspartnerId() + " hat neue Daten. -> update", null, data.getGeschaeftspartnerId());
            dmsService.updateFile(data);
            this.log(data, DatensatzStatus.DONE);
            break;
        case DatensatzStatus.ARCHIVE:
            // personenbezogene Daten entfernen wird vom Projekt nicht erwartet.
            break;
        case DatensatzStatus.ERROR:
            this.heal(data);
            break;
        default:
            log.warn("Status steht auf {}", data.getStatus().getValue());
        }
    }

    private void log(final PscdImport data, final DatensatzStatus status) {
        data.setStatus(status);
        log.debug("{}: {}", data.getGeschaeftspartnerId(), status.getValue());
    }

    private void heal(final PscdImport data) {
        if (data.getBetreffseinheit().isEmpty()) {
            data.setStatus(DatensatzStatus.STARTED);
            this.datensatzVerarbeitung(data);
        } else if (data.getAkte().isEmpty()) {
            this.healAkte(data);
        } else if (data.getBestandsakt().isEmpty()) {
            data.setBestandsakt(this.healVorgang(data, DmsService.BESTANDSAKTEN, DatensatzStatus.FILE_CREATED, DatensatzStatus.BESTANDSAKT_CREATED));
        } else if (data.getAv().isEmpty()) {
            data.setAv(this.healVorgang(data, DmsService.AVS_TITEL_HAFTBEFEHLE, DatensatzStatus.BESTANDSAKT_CREATED, DatensatzStatus.DONE));
        } else {
            dbLogger.info("keine Heilung gefunden", null, data.getGeschaeftspartnerId());
        }
    }

    private void healAkte(PscdImport data) {
        SearchFileResponseDTO dto = dmsService.searchFiles(data);
        if (dto.getGiobjecttype() != null && !dto.getGiobjecttype().isEmpty()) {
            data.setAkte(dto.getGiobjecttype().getFirst().getObjaddress());
            data.setStatus(DatensatzStatus.FILE_CREATED);
            this.datensatzVerarbeitung(data);
        } else {
            data.setStatus(DatensatzStatus.APENTRY_EXISTS);
            this.datensatzVerarbeitung(data);
        }
    }

    private String healVorgang(final PscdImport data, final String procedureName, final DatensatzStatus ohneCoo, final DatensatzStatus mitCoo) {
        final SearchProcedureResponseDTO dto = dmsService.searchProcedures(procedureName);
        String coo = null;
        if (dto.getGiobjecttype() != null ) {
            for (Objektreferenz ref : dto.getGiobjecttype()) {
                if (procedureName.equals(ref.getObjname())) {
                    coo = ref.getObjaddress();
                    break;
                }
            }
        }
        if (coo == null) {
            data.setStatus(ohneCoo);
            return null;
        }
        data.setStatus(mitCoo);
        return coo;
    }

}
