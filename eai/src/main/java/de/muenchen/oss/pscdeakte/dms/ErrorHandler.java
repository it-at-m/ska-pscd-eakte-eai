package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.model.DmsErrorResponse;
import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.DbToEakteService;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

    private final DBLogger dbLogger;
    private final DbToEakteService dbToEakteService;
    private final DmsProperties dmsProperties;
    private String gpId = "";
    private int retries = 1;

    public void handleException(final Exception exceptionToCatch, final PscdImport data) {

        data.setStatus(DatensatzStatus.ERROR);
        if (gpId.equals(data.getGeschaeftspartnerId())) {
            retries++;
        } else {
            retries = 1;
            gpId = data.getGeschaeftspartnerId();
        }

        if (this.retry(data)) {
            return;
        }

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

    private boolean retry(final PscdImport data) {
        if (retries >= dmsProperties.getRetries()) {
            dbLogger.error("Abbruch nach " + dmsProperties.getRetries() + " erfolglosen Versuchen ", null, data.getGeschaeftspartnerId());
            return false;
        }

        try {
            Thread.sleep(Duration.ofSeconds(retries * 2L));
            dbToEakteService.process(data);
            if (DatensatzStatus.ERROR != data.getStatus()) {
                log.info("GpId {} erfolgreich verarbeitet nach Retry {}", data.getGeschaeftspartnerId(), retries);
                return true;
            }
        } catch (InterruptedException e) {
            log.debug("interrupted");
        }
        log.warn("GpId {}: Retry {} von {} erfolglos", data.getGeschaeftspartnerId(), retries, dmsProperties.getRetries());
        return false;
    }

}
