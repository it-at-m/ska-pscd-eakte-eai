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
public class ErrorHandler {

    public static final String ERROR = "error";
    private final DBLogger dbLog;

    public void handleException(final Exception exceptionToCatch, final String gpId){

        final StringBuilder builder = new StringBuilder();
        if (gpId != null && !gpId.isEmpty()){
            builder.append("GpId").append(gpId).append(": ");
        }
        builder.append(exceptionToCatch.getClass().getName()).append(" aus der eAkte: ");
        final String logPrefix = builder.toString();

        switch(exceptionToCatch){
            case final WebClientResponseException wcre:
                this.handleWebClientResponseException(wcre, logPrefix);
                break;
            case final IllegalStateException ise:
                log.error("{} Timeout", logPrefix);
                dbLog.log(ERROR, logPrefix + "Timeout", ise.getMessage());
                break;
            default:
                log.error("{}{}", logPrefix, exceptionToCatch.getStackTrace());
                dbLog.log(ERROR, logPrefix, exceptionToCatch.getMessage());
        }

    }

    private void handleWebClientResponseException(WebClientResponseException e, String prefix){
        DmsErrorResponse errorResponse = null;
        try {
            errorResponse = e.getResponseBodyAs(DmsErrorResponse.class);
        } catch (IllegalStateException | DecodingException innerEx) {
//            wird unter finally bearbeitet
        } finally {
            if (errorResponse != null) {
                log.error(prefix, errorResponse);
                dbLog.log(ERROR, prefix, errorResponse.toString());
            } else {
                log.error(prefix, e.getResponseBodyAsString());
                dbLog.log(ERROR, prefix, e.getResponseBodyAsString());
            }
        }
    }

}
