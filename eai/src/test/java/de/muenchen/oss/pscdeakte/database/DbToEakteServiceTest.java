package de.muenchen.oss.pscdeakte.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import de.muenchen.oss.refarch.integration.dms.model.DmsErrorResponse;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@ExtendWith(MockitoExtension.class)
class DbToEakteServiceTest {

    @Mock
    private PscdImportRepository repository;

    @Mock
    private DBLogger dbLogger;

    @Mock
    private DmsService dmsService;

    @Mock
    private Apentries apentries;

    @Test
    void startDoesNotInitializeApentriesWhenNoRecordsExist() {
        when(repository.streamAllByStatusIsNotOrderByCreatedTimeAsc(DatensatzStatus.DONE)).thenReturn(List.of());

        service().start();

        verifyNoInteractions(apentries);
    }

    @Test
    void startInitializesApentriesAndProcessesRecords() {
        final PscdImport data = data(DatensatzStatus.ARCHIVE);
        when(repository.streamAllByStatusIsNotOrderByCreatedTimeAsc(DatensatzStatus.DONE)).thenReturn(List.of(data));

        service().start();

        verify(apentries).initMap();
        verify(repository).save(data);
    }

    @Test
    void processNewRecordCreatesAllDmsObjects() {
        final PscdImport data = data(DatensatzStatus.NEW);
        when(apentries.getApentryCoo("gp-id")).thenReturn("apentry-coo");
        when(dmsService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        assertEquals("apentry-coo", data.getBetreffseinheit());
        assertEquals("akte-coo", data.getAkte());
        assertEquals("bestandsakt-coo", data.getBestandsakt());
        assertEquals("av-coo", data.getAv());
        verify(repository).save(data);
    }

    @Test
    void processStartedRecordContinuesWithApentryCreation() {
        final PscdImport data = data(DatensatzStatus.STARTED);
        when(apentries.getApentryCoo("gp-id")).thenReturn("apentry-coo");
        when(dmsService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processApentryRecordContinuesWithFileCreation() {
        final PscdImport data = data(DatensatzStatus.APENTRY_EXISTS);
        when(dmsService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processFileRecordContinuesWithBestandsaktCreation() {
        final PscdImport data = data(DatensatzStatus.FILE_CREATED);
        data.setAkte("akte-coo");
        when(dmsService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processBestandsaktRecordCreatesAv() {
        final PscdImport data = data(DatensatzStatus.BESTANDSAKT_CREATED);
        data.setAkte("akte-coo");
        when(dmsService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processUpdateRecordUpdatesFile() {
        final PscdImport data = data(DatensatzStatus.UPDATE);

        service().process(data);

        verify(dmsService).updateFile(data);
        verify(dbLogger).log("info", "Gp gp-id hat neue Daten. -> update", null);
        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processArchiveRecordDoesNotCallDms() {
        final PscdImport data = data(DatensatzStatus.ARCHIVE);

        service().process(data);

        verifyNoInteractions(dmsService);
        verify(repository).save(data);
    }

    @Test
    void processErrorRecordLogsError() {
        final PscdImport data = data(DatensatzStatus.ERROR);

        service().process(data);

        verify(dbLogger).log(DbToEakteService.ERROR, "GpId gp-idsteht auf ERROR.", null);
    }

    @Test
    void processDoneRecordDoesNotCallDms() {
        final PscdImport data = data(DatensatzStatus.DONE);

        service().process(data);

        verifyNoInteractions(dmsService);
        verify(repository).save(data);
    }

    @Test
    void processLogsTypedWebClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final WebClientResponseException exception = mock(WebClientResponseException.class);
        final DmsErrorResponse errorResponse = mock(DmsErrorResponse.class);
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenReturn(errorResponse);
        when(errorResponse.toString()).thenReturn("typed error");
        doThrow(exception).when(dmsService).updateFile(data);

        service().process(data);

        verify(dbLogger).log(DbToEakteService.ERROR, "Exception aus der eAkte: WebclientResponseException", "request failed");
        verify(dbLogger).log(DbToEakteService.ERROR, "DmsErrorResponse der WebclientResponseException", "typed error");
        verify(repository).save(data);
    }

    @Test
    void processLogsRawWebClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final WebClientResponseException exception = mock(WebClientResponseException.class);
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenReturn(null);
        when(exception.getResponseBodyAsString()).thenReturn("raw error");
        doThrow(exception).when(dmsService).updateFile(data);

        service().process(data);

        verify(dbLogger).log(DbToEakteService.ERROR, "ResponseBody der WebclientResponseException", "raw error");
    }

    @Test
    void processHandlesUndecodableWebClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final WebClientResponseException exception = mock(WebClientResponseException.class);
        final String rawResponseBody = "undecodable raw response";
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenThrow(new IllegalStateException("undecodable"));
        when(exception.getResponseBodyAsString()).thenReturn(rawResponseBody);
        doThrow(exception).when(dmsService).updateFile(data);
        final Logger logger = (Logger) LoggerFactory.getLogger(DbToEakteService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service().process(data);

            assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.WARN && event.getFormattedMessage().contains(rawResponseBody)));
            verify(repository).save(data);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void processLogsIllegalStateException() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        doThrow(new IllegalStateException("timeout")).when(dmsService).updateFile(data);

        service().process(data);

        verify(dbLogger).log(DbToEakteService.ERROR, "Timeout in der eAkte", "timeout");
    }

    @Test
    void processLogsUnexpectedException() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        doThrow(new RuntimeException("unexpected")).when(dmsService).updateFile(data);

        service().process(data);

        verify(dbLogger).log(DbToEakteService.ERROR, "Exception beim Schreiben in eAkte", "unexpected");
    }

    private DbToEakteService service() {
        return new DbToEakteService(repository, dbLogger, dmsService, apentries);
    }

    private static PscdImport data(final DatensatzStatus status) {
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId("gp-id");
        data.setStatus(status);
        return data;
    }

    private static DmsObjektResponse response(final String objid) {
        final DmsObjektResponse response = new DmsObjektResponse();
        response.setObjid(objid);
        return response;
    }

}
