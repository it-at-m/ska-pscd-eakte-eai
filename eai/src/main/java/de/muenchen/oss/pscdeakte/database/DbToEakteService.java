package de.muenchen.oss.pscdeakte.database;

import de.muenchen.oss.eakte.v1.generated.model.DmsErrorResponse;
import de.muenchen.oss.eakte.v1.generated.model.Objektreferenz;
import de.muenchen.oss.eakte.v1.generated.model.SearchFileResponseDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchProcedureResponseDTO;
import de.muenchen.oss.pscdeakte.configuration.annotation.LogExecutionTime;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsApentryService;
import de.muenchen.oss.pscdeakte.dms.DmsFileService;
import de.muenchen.oss.pscdeakte.dms.DmsProcedureService;
import de.muenchen.oss.pscdeakte.dms.DmsProperties;

import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@RequiredArgsConstructor
@Service
@Slf4j
public class DbToEakteService {

    private final PscdImportRepository repo;
    private final DmsFileService dmsFileService;
    private final DmsProcedureService dmsProcedureService;
    private final Apentries apentries;
    private final DBLogger dbLogger;
    private final DmsProperties dmsProperties;

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
        try {
            for (int retry = 0;; retry++) {
                if (processOrHeal(data, retry)) {
                    return;
                }
            }
        } finally {
            repo.save(data);
        }
    }

    private boolean processOrHeal(final PscdImport data, final int retry) {
        try {
            if (retry == 0) {
                datensatzVerarbeitung(data);
            } else {
                heal(data);
            }
            return true;
        } catch (Exception e) {
            return catchException(data, retry, e);
        }
    }

    @SuppressWarnings("PMD.DoNotUseThreads")
    private boolean catchException(final PscdImport data, final int retry, final Exception e) {
        data.setStatus(DatensatzStatus.ERROR);

        final int maxRetries = dmsProperties.getRetries();
        if (retry >= maxRetries) {
            logException(e, data.getGeschaeftspartnerId());
            return true;
        }

        final int nextRetry = retry + 1;
        log.warn("GpId {}: Retry {} von {}",
                data.getGeschaeftspartnerId(), nextRetry, maxRetries);

        final Integer waitTimeMultiplier = dmsProperties.getWaitTimeMultiplier();
        if (waitTimeMultiplier != null && waitTimeMultiplier > 0) {
            try {
                Thread.sleep(Duration.ofSeconds((long) nextRetry * waitTimeMultiplier));
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                log.warn("Retry für GpId {} unterbrochen", data.getGeschaeftspartnerId());
                return true;
            }
        }
        return false;
    }

    private void logException(final Exception exceptionToCatch, final String gpId) {
        final String logPrefix = "GpId " + gpId + ": " + exceptionToCatch.getClass().getSimpleName() + " aus der eAkte";

        switch (exceptionToCatch) {
        case final RestClientResponseException rcre:
            this.handleRestClientResponseException(rcre, logPrefix, gpId);
            break;
        case final IllegalStateException ise:
            log.error("{}: Timeout", logPrefix);
            dbLogger.error(logPrefix + ": Timeout", ise.getMessage(), gpId);
            break;
        default:
            log.error("{}: {}", logPrefix, exceptionToCatch.getStackTrace());
            dbLogger.error(logPrefix, exceptionToCatch.getMessage(), gpId);
        }
    }

    @SuppressWarnings("PMD.EmptyCatchBlock")
    private void handleRestClientResponseException(final RestClientResponseException e, final String prefix, final String gpId) {
        final StringBuilder sb = new StringBuilder(e.getMessage()).append(" | ");
        DmsErrorResponse errorResponse = null;
        try {
            errorResponse = e.getResponseBodyAs(DmsErrorResponse.class);
        } catch (IllegalStateException | DecodingException innerEx) {
            // wird unter finally bearbeitet
        } finally {
            if (errorResponse != null) {
                log.error("{}: {} | {}", prefix, e.getMessage(), errorResponse);
                sb.append(errorResponse);
            } else {
                log.error("{}: {} | {}", prefix, e.getMessage(), e.getResponseBodyAsString());
                sb.append(e.getResponseBodyAsString());
            }
            dbLogger.error(prefix, sb.toString(), gpId);
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
            data.setAkte(dmsFileService.createFile(data).getObjid());
            this.log(data, DatensatzStatus.FILE_CREATED);
            // fallthrough
        case DatensatzStatus.FILE_CREATED:
            data.setBestandsakt(dmsProcedureService.createProcedureBestandsakte(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.BESTANDSAKT_CREATED);
            // fallthrough
        case DatensatzStatus.BESTANDSAKT_CREATED:
            data.setAv(dmsProcedureService.createProcedureAV(data.getAkte()).getObjid());
            this.log(data, DatensatzStatus.DONE);
            break;
        case DatensatzStatus.UPDATE:
            dbLogger.info("GpId " + data.getGeschaeftspartnerId() + " hat neue Daten. -> update", null, data.getGeschaeftspartnerId());
            dmsFileService.updateFile(data);
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
        if (data.getBetreffseinheit() == null || data.getBetreffseinheit().isEmpty()) {
            apentries.refreshMap();
            data.setStatus(DatensatzStatus.STARTED);
        } else if (data.getAkte() == null || data.getAkte().isEmpty()) {
            this.healAkte(data);
        } else if (data.getBestandsakt() == null || data.getBestandsakt().isEmpty()) {
            data.setBestandsakt(this.healVorgang(data, DmsApentryService.BESTANDSAKTEN, DatensatzStatus.FILE_CREATED, DatensatzStatus.BESTANDSAKT_CREATED));
        } else if (data.getAv() == null || data.getAv().isEmpty()) {
            data.setAv(this.healVorgang(data, DmsApentryService.AVS_TITEL_HAFTBEFEHLE, DatensatzStatus.BESTANDSAKT_CREATED, DatensatzStatus.DONE));
        } else {
            data.setStatus(DatensatzStatus.UPDATE);
        }
        this.datensatzVerarbeitung(data);
    }

    private void healAkte(final PscdImport data) {
        final SearchFileResponseDTO dto = dmsFileService.searchFiles(data);
        final List<Objektreferenz> akten = dto == null ? null : dto.getGiobjecttype();
        if (akten == null || akten.isEmpty()) {
            data.setStatus(DatensatzStatus.APENTRY_EXISTS);
            return;
        }
        data.setAkte(akten.getFirst().getObjaddress());
        data.setStatus(DatensatzStatus.FILE_CREATED);
        if (akten.size() != 1) {
            final StringBuilder str = new StringBuilder("Duplikat zu GpId ").append(data.getGeschaeftspartnerId()).append(" gefunden:");
            akten.forEach(akte -> str.append(" ").append(akte.getObjaddress()));
            str.append(" Im weiteren Verlauf wird die erste Akte verwendet: ").append(data.getAkte());
            dbLogger.warn(str.toString(), null, data.getGeschaeftspartnerId());
        }
    }

    private String healVorgang(final PscdImport data, final String procedureName, final DatensatzStatus ohneCoo, final DatensatzStatus mitCoo) {
        final SearchProcedureResponseDTO dto = dmsProcedureService.searchProcedures(data.getAkte(), procedureName);
        final List<Objektreferenz> vorgaenge = dto == null ? null : dto.getGiobjecttype();
        if (vorgaenge == null || vorgaenge.isEmpty()) {
            data.setStatus(ohneCoo);
            return null;
        }

        if (vorgaenge.size() > 1) {
            final StringBuilder str = new StringBuilder("mehrere Vorgänge \"").append(procedureName).append("\" gefunden in Akte GpId ")
                    .append(data.getGeschaeftspartnerId()).append(", coo-Adresse ").append(data.getAkte()).append(":");
            vorgaenge.forEach(vorgang -> str.append(" ").append(vorgang.getObjaddress()));
            dbLogger.warn(str.toString(), null, data.getGeschaeftspartnerId());
        }
        data.setStatus(mitCoo);
        return vorgaenge.getFirst().getObjaddress();
    }

}
