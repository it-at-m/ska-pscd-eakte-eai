package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.model.DmsErrorResponse;
import de.muenchen.oss.pscdeakte.database.DBLogger;
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
    public static final String ERROR = "error";

    public void handleException(final Exception exceptionToCatch, final String gpId) {

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
            dbLogger.log(ERROR, logPrefix + ": Timeout", ise.getMessage(), gpId);
            break;
        default:
            log.error("{}: {}", logPrefix, exceptionToCatch.getStackTrace());
            dbLogger.log(ERROR, logPrefix, exceptionToCatch.getMessage(), gpId);
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
            dbLogger.log(ERROR, prefix, sb.toString(), gpId);
        }
    }

}
