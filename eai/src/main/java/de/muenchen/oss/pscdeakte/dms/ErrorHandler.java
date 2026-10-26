package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.database.DBLogger;
import de.muenchen.oss.refarch.integration.dms.model.DmsErrorResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Slf4j
public class ErrorHandler {

    public static final String ERROR = "error";
    private final DBLogger dbLog;

    public void handleException(final Exception exceptionToCatch, final String gpId){

        final StringBuilder builder = new StringBuilder();
        if (gpId != null && !gpId.isEmpty()){
            builder.append("GpId").append(gpId).append(": ").append(exceptionToCatch.getClass().getName()).append(" aus der eAkte: ");
        }

        switch(exceptionToCatch){
            case final WebClientResponseException wcre:
                System.out.println(wcre);
                break;
            case final IllegalStateException ise:
                log.error("{} timeout", builder);
                dbLog.log(ERROR, "Timeout in der eAkte", ise.getMessage());
                break;
            default:
                log.error("{}{}", builder, exceptionToCatch.getStackTrace());
                dbLog.log(ERROR, builder.toString(), exceptionToCatch.getMessage());
        }

    }

    private void handleWebClientResponseException(WebClientResponseException e, StringBuilder gpId){
        log.error("Exception aus der eAkte: WebclientResponseException", e);
        dbLog.log(ERROR, "Exception aus der eAkte: WebclientResponseException", e.getMessage());
        try {
            final DmsErrorResponse errorResponse = e.getResponseBodyAs(DmsErrorResponse.class);
            if (errorResponse != null) {
                log.error(errorResponse.toString());
                dbLog.log(ERROR, "DmsErrorResponse der WebclientResponseException", errorResponse.toString());
            } else {
                log.error("ResponseBody der WebclientResponseException: {}", e.getResponseBodyAsString());
                dbLog.log(ERROR, "ResponseBody der WebclientResponseException", e.getResponseBodyAsString());
            }
        } catch (IllegalStateException | DecodingException innerEx) {
            log.warn("ResponseBody der WebClientResponseException konnte nicht dekodiert werden: {}", e.getResponseBodyAsString());
        }
    }

}
