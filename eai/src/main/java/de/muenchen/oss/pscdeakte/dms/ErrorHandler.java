package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.refarch.integration.dms.model.DmsErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Service
@RequiredArgsConstructor
@Slf4j
public class ErrorHandler extends DBLogger {

    public static final String ERROR = "error";

    public void handleException(final Exception exceptionToCatch, final String gpId) {

        final StringBuilder builder = new StringBuilder();
        if (gpId != null && !gpId.isEmpty()) {
            builder.append("GpId ").append(gpId).append(": ");
        }
        builder.append(exceptionToCatch.getClass().getSimpleName()).append(" aus der eAkte");
        final String logPrefix = builder.toString();

        switch (exceptionToCatch) {
        case final WebClientResponseException wcre:
            this.handleWebClientResponseException(wcre, logPrefix, gpId);
            break;
        case final IllegalStateException ise:
            log.error("{}: Timeout", logPrefix);
            log(ERROR, logPrefix + ": Timeout", ise.getMessage(), gpId);
            break;
        default:
            log.error("{}: {}", logPrefix, exceptionToCatch.getStackTrace());
            log(ERROR, logPrefix, exceptionToCatch.getMessage(), gpId);
        }

    }

    private void handleWebClientResponseException(WebClientResponseException e, String prefix, String gpId) {
        final StringBuilder sb = new StringBuilder(e.getMessage()).append(" | ");
        DmsErrorResponse errorResponse = null;
        try {
            errorResponse = e.getResponseBodyAs(DmsErrorResponse.class);
        } catch (IllegalStateException | DecodingException innerEx) {
            //            wird unter finally bearbeitet
        } finally {
            if (errorResponse != null) {
                log.error("{}: {} | {}", prefix, e.getMessage(), errorResponse);
                sb.append(errorResponse);
            } else {
                log.error("{}: {} | {}", prefix, e.getMessage(), e.getResponseBodyAsString());
                sb.append(e.getResponseBodyAsString());
            }
            log(ERROR, prefix, sb.toString(), gpId);
        }
    }

}
