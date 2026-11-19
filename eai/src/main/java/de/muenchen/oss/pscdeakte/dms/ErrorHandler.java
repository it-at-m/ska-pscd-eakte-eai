package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.model.DmsErrorResponse;
import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.DbToEakteService;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

    private final DBLogger dbLogger;
    private final DbToEakteService dbToEakteService;

    public void handleException(final Exception exceptionToCatch, final PscdImport data) {

        data.setStatus(DatensatzStatus.ERROR);
        final String gpId = data.getGeschaeftspartnerId();

        final StringBuilder builder = new StringBuilder();
        if (gpId != null && !gpId.isEmpty()) {
            builder.append("GpId ").append(gpId).append(": ");
        }
        builder.append(exceptionToCatch.getClass().getSimpleName()).append(" aus der eAkte");
        final String logPrefix = builder.toString();

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
        this.retry(data);
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

    private void retry(final PscdImport data) {
//        FIXME Dauerschleife: wegen rekursivem Aufruf von process, kommt man ueber ErrorHandler wieder hier an. => immer nur erster retry
        for (int i = 0; i < 10; i++) {
            try {
                Thread.sleep(Duration.ofSeconds(i*2));
                dbToEakteService.process(data);
                if (DatensatzStatus.ERROR != data.getStatus()) {
                    dbLogger.info("erfolgreich verarbeitet nach Retry " + (i + 1), null, data.getGeschaeftspartnerId());
                    return;
                }
            } catch (InterruptedException e) {
                log.debug("interrupted");
            }
            dbLogger.info("Retry " + (i + 1) + " von 10 erfolglos", null, data.getGeschaeftspartnerId());
        }
        dbLogger.error("Abbruch nach 10 erfolglosen Versuchen ", null, data.getGeschaeftspartnerId());
    }

}
