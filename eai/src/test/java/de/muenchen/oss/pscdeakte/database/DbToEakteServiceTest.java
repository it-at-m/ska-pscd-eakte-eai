package de.muenchen.oss.pscdeakte.database;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import de.muenchen.oss.eakte.v1.generated.model.DmsErrorResponse;
import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.Objektreferenz;
import de.muenchen.oss.eakte.v1.generated.model.SearchFileResponseDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchProcedureResponseDTO;
import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.LogEntryRepository;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsApentryService;
import de.muenchen.oss.pscdeakte.dms.DmsFileService;
import de.muenchen.oss.pscdeakte.dms.DmsProcedureService;
import de.muenchen.oss.pscdeakte.dms.DmsProperties;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.client.RestClientResponseException;

@ExtendWith(MockitoExtension.class)
class DbToEakteServiceTest {

    public static final String GP_ID = "gp-id";
    @Mock
    private PscdImportRepository repository;

    @Mock
    private LogEntryRepository logEntryRepository;

    @Mock
    private DmsFileService dmsFileService;

    @Mock
    private DmsProcedureService dmsProcedureService;

    @Mock
    private Apentries apentries;

    private DBLogger dbLogger;
    private DmsProperties dmsProperties;

    @BeforeEach
    void beforeEach() {
        dbLogger = new DBLogger(logEntryRepository);
        dmsProperties = new DmsProperties();
        dmsProperties.setRetries(0);
    }

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
        when(apentries.getApentryCoo(GP_ID)).thenReturn("apentry-coo");
        when(dmsFileService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsProcedureService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

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
        when(apentries.getApentryCoo(GP_ID)).thenReturn("apentry-coo");
        when(dmsFileService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsProcedureService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processApentryRecordContinuesWithFileCreation() {
        final PscdImport data = data(DatensatzStatus.APENTRY_EXISTS);
        when(dmsFileService.createFile(data)).thenReturn(response("akte-coo"));
        when(dmsProcedureService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processFileRecordContinuesWithBestandsaktCreation() {
        final PscdImport data = data(DatensatzStatus.FILE_CREATED);
        data.setAkte("akte-coo");
        when(dmsProcedureService.createProcedureBestandsakte("akte-coo")).thenReturn(response("bestandsakt-coo"));
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processBestandsaktRecordCreatesAv() {
        final PscdImport data = data(DatensatzStatus.BESTANDSAKT_CREATED);
        data.setAkte("akte-coo");
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("av-coo"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processUpdateRecordUpdatesFile() {
        final PscdImport data = data(DatensatzStatus.UPDATE);

        service().process(data);

        verify(dmsFileService).updateFile(data);
        verifyLog(DBLogger.Level.INFO.toString(), "GpId gp-id hat neue Daten. -> update", null, GP_ID);
        assertEquals(DatensatzStatus.DONE, data.getStatus());
    }

    @Test
    void processArchiveRecordDoesNotCallDms() {
        final PscdImport data = data(DatensatzStatus.ARCHIVE);

        service().process(data);

        verifyNoInteractions(dmsFileService, dmsProcedureService);
        verify(repository).save(data);
    }

    @Test
    void processDoneRecordDoesNotCallDms() {
        final PscdImport data = data(DatensatzStatus.DONE);

        service().process(data);

        verifyNoInteractions(dmsFileService, dmsProcedureService);
        verify(repository).save(data);
    }

    @Test
    void processLogsTypedRestClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final RestClientResponseException exception = mock(RestClientResponseException.class);
        final DmsErrorResponse errorResponse = mock(DmsErrorResponse.class);
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenReturn(errorResponse);
        when(errorResponse.toString()).thenReturn("typed error");
        doThrow(exception).when(dmsFileService).updateFile(data);

        service().process(data);

        verifyLog(DBLogger.Level.ERROR.toString(), "GpId " + GP_ID + ": RestClientResponseException aus der eAkte", "request failed | typed error", GP_ID);

        verify(repository).save(data);
    }

    @Test
    void processLogsRawRestClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final RestClientResponseException exception = mock(RestClientResponseException.class);
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenReturn(null);
        when(exception.getResponseBodyAsString()).thenReturn("raw error");
        doThrow(exception).when(dmsFileService).updateFile(data);

        service().process(data);

        verifyLog(DBLogger.Level.ERROR.toString(), "GpId " + GP_ID + ": RestClientResponseException aus der eAkte", "request failed | raw error", GP_ID);
    }

    @Test
    void processHandlesUndecodableRestClientErrorResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final RestClientResponseException exception = mock(RestClientResponseException.class);
        final String rawResponseBody = "undecodable raw response";
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenThrow(new IllegalStateException("undecodable"));
        when(exception.getResponseBodyAsString()).thenReturn(rawResponseBody);
        doThrow(exception).when(dmsFileService).updateFile(data);
        final Logger logger = (Logger) LoggerFactory.getLogger(DbToEakteService.class);
        final ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        try {
            service().process(data);

            assertTrue(appender.list.stream().anyMatch(event -> event.getLevel() == Level.ERROR
                    && event.getFormattedMessage().equals("GpId gp-id: RestClientResponseException aus der eAkte: request failed | undecodable raw response")));
            verify(repository).save(data);
        } finally {
            logger.detachAppender(appender);
        }
    }

    @Test
    void processLogsIllegalStateException() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        doThrow(new IllegalStateException("timeout")).when(dmsFileService).updateFile(data);

        service().process(data);

        verifyLog(DBLogger.Level.ERROR.toString(), "GpId gp-id: IllegalStateException aus der eAkte: Timeout", "timeout", GP_ID);
    }

    @Test
    void processLogsUnexpectedException() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        doThrow(new RuntimeException("unexpected")).when(dmsFileService).updateFile(data);

        service().process(data);

        verifyLog(DBLogger.Level.ERROR.toString(), "GpId " + GP_ID + ": RuntimeException aus der eAkte", "unexpected", GP_ID);
    }

    @Test
    void processRetriesAndHealsCompleteRecord() {
        dmsProperties.setRetries(1);
        final PscdImport data = completeErrorData();
        data.setStatus(DatensatzStatus.UPDATE);
        doThrow(new RuntimeException("temporary")).doNothing().when(dmsFileService).updateFile(data);

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        verify(dmsFileService, times(2)).updateFile(data);
        verify(repository).save(data);
    }

    @Test
    void processStopsWhenRetryLimitIsReached() {
        dmsProperties.setRetries(1);
        final PscdImport data = completeErrorData();
        data.setStatus(DatensatzStatus.UPDATE);
        doThrow(new IllegalStateException("timeout")).when(dmsFileService).updateFile(data);
        service().process(data);

        assertEquals(DatensatzStatus.ERROR, data.getStatus());
        verify(dmsFileService, times(2)).updateFile(data);
        verify(repository).save(data);
        verifyLog(DBLogger.Level.ERROR.toString(), "GpId gp-id: IllegalStateException aus der eAkte: Timeout", "timeout", GP_ID);
    }

    @Test
    void retryCounterIsIndependentForConsecutiveCallsForSameGp() {
        dmsProperties.setRetries(1);
        final PscdImport first = completeErrorData();
        final PscdImport second = completeErrorData();
        first.setStatus(DatensatzStatus.UPDATE);
        second.setStatus(DatensatzStatus.UPDATE);
        doThrow(new IllegalStateException("timeout")).when(dmsFileService).updateFile(same(first));
        doThrow(new IllegalStateException("timeout")).doNothing().when(dmsFileService).updateFile(same(second));
        final DbToEakteService service = service();

        service.process(first);
        service.process(second);

        assertEquals(DatensatzStatus.ERROR, first.getStatus());
        assertEquals(DatensatzStatus.DONE, second.getStatus());
        verify(dmsFileService, times(2)).updateFile(same(first));
        verify(dmsFileService, times(2)).updateFile(same(second));
        verify(repository).save(same(first));
        verify(repository).save(same(second));
    }

    @Test
    void processStopsWhenRetryWaitIsInterrupted() {
        dmsProperties.setRetries(1);
        final PscdImport data = completeErrorData();
        data.setStatus(DatensatzStatus.UPDATE);
        doThrow(new RuntimeException("failure")).when(dmsFileService).updateFile(data);

        try {
            Thread.currentThread().interrupt();
            service().process(data);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }

        assertEquals(DatensatzStatus.ERROR, data.getStatus());
        verify(dmsFileService).updateFile(data);
        verify(repository).save(data);
    }

    @Test
    void processHandlesDecodingExceptionInRestResponse() {
        final PscdImport data = data(DatensatzStatus.UPDATE);
        final RestClientResponseException exception = mock(RestClientResponseException.class);
        when(exception.getMessage()).thenReturn("request failed");
        when(exception.getResponseBodyAs(DmsErrorResponse.class)).thenThrow(new DecodingException("invalid"));
        when(exception.getResponseBodyAsString()).thenReturn("raw error");
        doThrow(exception).when(dmsFileService).updateFile(data);

        service().process(data);

        verifyLog(DBLogger.Level.ERROR.toString(), "GpId gp-id: RestClientResponseException aus der eAkte", "request failed | raw error", GP_ID);
        assertEquals(DatensatzStatus.ERROR, data.getStatus());
        verify(repository).save(data);
    }

    @Test
    void retryAfterApentryTimeoutRefreshesMapAndContinuesCreation() {
        dmsProperties.setRetries(1);
        final PscdImport data = data(DatensatzStatus.NEW);
        when(apentries.getApentryCoo(GP_ID)).thenThrow(new IllegalStateException("timeout")).thenReturn("new-apentry");
        when(dmsFileService.createFile(data)).thenReturn(response("new-akte"));
        when(dmsProcedureService.createProcedureBestandsakte("new-akte")).thenReturn(response("new-bestandsakt"));
        when(dmsProcedureService.createProcedureAV("new-akte")).thenReturn(response("new-av"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        assertEquals("new-apentry", data.getBetreffseinheit());
        assertEquals("new-av", data.getAv());
        verify(apentries).refreshMap();
        verify(apentries, times(2)).getApentryCoo(GP_ID);
        verify(repository).save(data);
    }

    @Test
    void retryAfterFileTimeoutFindsExistingFileAndContinuesWithProcedures() {
        dmsProperties.setRetries(1);
        final PscdImport data = data(DatensatzStatus.APENTRY_EXISTS);
        data.setBetreffseinheit("apentry-coo");
        final SearchFileResponseDTO search = mock(SearchFileResponseDTO.class);
        final Objektreferenz reference = mock(Objektreferenz.class);
        when(reference.getObjaddress()).thenReturn("found-akte");
        when(search.getGiobjecttype()).thenReturn(List.of(reference));
        when(dmsFileService.createFile(data)).thenThrow(new IllegalStateException("timeout"));
        when(dmsFileService.searchFiles(data)).thenReturn(search);
        when(dmsProcedureService.createProcedureBestandsakte("found-akte")).thenReturn(response("new-bestandsakt"));
        when(dmsProcedureService.createProcedureAV("found-akte")).thenReturn(response("new-av"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        assertEquals("found-akte", data.getAkte());
        verify(dmsFileService).searchFiles(data);
        verify(dmsFileService).createFile(data);
        verify(dmsProcedureService).createProcedureBestandsakte("found-akte");
        verify(repository).save(data);
    }

    @Test
    void retryAfterFileTimeoutCreatesFileWhenSearchIsEmpty() {
        dmsProperties.setRetries(1);
        final PscdImport data = data(DatensatzStatus.APENTRY_EXISTS);
        data.setBetreffseinheit("apentry-coo");
        final SearchFileResponseDTO search = mock(SearchFileResponseDTO.class);
        when(search.getGiobjecttype()).thenReturn(List.of());
        when(dmsFileService.searchFiles(data)).thenReturn(search);
        when(dmsFileService.createFile(data)).thenThrow(new IllegalStateException("timeout")).thenReturn(response("new-akte"));
        when(dmsProcedureService.createProcedureBestandsakte("new-akte")).thenReturn(response("new-bestandsakt"));
        when(dmsProcedureService.createProcedureAV("new-akte")).thenReturn(response("new-av"));

        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        assertEquals("new-akte", data.getAkte());
        verify(dmsFileService, times(2)).createFile(data);
        verify(repository).save(data);
    }

    @Test
    void processErrorRecordRemainsInErrorWithoutCallingDms() {
        final PscdImport data = completeErrorData();

        service().process(data);

        assertEquals(DatensatzStatus.ERROR, data.getStatus());
        verifyNoInteractions(dmsFileService, dmsProcedureService, apentries);
        verify(repository).save(data);
    }

    @Test
    void retryAfterBestandsaktTimeoutUsesExistingProcedure() {
        dmsProperties.setRetries(1);
        final PscdImport data = data(DatensatzStatus.FILE_CREATED);
        data.setBetreffseinheit("apentry-coo");
        data.setAkte("akte-coo");
        final SearchProcedureResponseDTO search = mock(SearchProcedureResponseDTO.class);
        final Objektreferenz reference = mock(Objektreferenz.class);
        when(reference.getObjaddress()).thenReturn("found-bestandsakt");
        when(search.getGiobjecttype()).thenReturn(List.of(reference));
        when(dmsProcedureService.searchProcedures("akte-coo", DmsApentryService.BESTANDSAKTEN)).thenReturn(search);
        when(dmsProcedureService.createProcedureBestandsakte("akte-coo")).thenThrow(new IllegalStateException("timeout"));
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenReturn(response("new-av"));

        service().process(data);

        assertEquals("found-bestandsakt", data.getBestandsakt());
        assertEquals("new-av", data.getAv());
        assertEquals(DatensatzStatus.DONE, data.getStatus());
        verify(dmsProcedureService).searchProcedures("akte-coo", DmsApentryService.BESTANDSAKTEN);
        verify(dmsProcedureService).createProcedureBestandsakte("akte-coo");
        verify(repository).save(data);
    }

    @Test
    void retryAfterAvTimeoutUsesExistingProcedure() {
        dmsProperties.setRetries(1);
        final PscdImport data = completeErrorData();
        data.setStatus(DatensatzStatus.BESTANDSAKT_CREATED);
        data.setAv(null);
        final SearchProcedureResponseDTO search = mock(SearchProcedureResponseDTO.class);
        final Objektreferenz reference = mock(Objektreferenz.class);
        when(reference.getObjaddress()).thenReturn("found-av");
        when(search.getGiobjecttype()).thenReturn(List.of(reference));
        when(dmsProcedureService.searchProcedures("akte-coo", DmsApentryService.AVS_TITEL_HAFTBEFEHLE)).thenReturn(search);
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenThrow(new IllegalStateException("timeout"));

        service().process(data);

        assertEquals("found-av", data.getAv());
        assertEquals("bestandsakt-coo", data.getBestandsakt());
        assertEquals(DatensatzStatus.DONE, data.getStatus());
        verify(dmsProcedureService).searchProcedures("akte-coo", DmsApentryService.AVS_TITEL_HAFTBEFEHLE);
        verify(dmsProcedureService).createProcedureAV("akte-coo");
        verify(dmsProcedureService, never()).createProcedureBestandsakte("akte-coo");
        verify(repository).save(data);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void retryAfterAvTimeoutWithoutSearchResultsPreservesBestandsakt(final List<Objektreferenz> results) {
        dmsProperties.setRetries(1);
        final PscdImport data = completeErrorData();
        data.setStatus(DatensatzStatus.BESTANDSAKT_CREATED);
        data.setAv(null);
        final SearchProcedureResponseDTO search = mock(SearchProcedureResponseDTO.class);
        when(search.getGiobjecttype()).thenReturn(results);
        when(dmsProcedureService.searchProcedures("akte-coo", DmsApentryService.AVS_TITEL_HAFTBEFEHLE)).thenReturn(search);
        when(dmsProcedureService.createProcedureAV("akte-coo")).thenThrow(new IllegalStateException("timeout")).thenReturn(response("new-av"));
        service().process(data);

        assertEquals(DatensatzStatus.DONE, data.getStatus());
        assertEquals("bestandsakt-coo", data.getBestandsakt());
        assertEquals("new-av", data.getAv());
        verify(dmsProcedureService, never()).createProcedureBestandsakte("akte-coo");
        verify(dmsProcedureService, times(2)).createProcedureAV("akte-coo");
        verify(repository).save(data);
    }

    private static PscdImport completeErrorData() {
        final PscdImport data = data(DatensatzStatus.ERROR);
        data.setBetreffseinheit("apentry-coo");
        data.setAkte("akte-coo");
        data.setBestandsakt("bestandsakt-coo");
        data.setAv("av-coo");
        return data;
    }

    private DbToEakteService service() {
        return new DbToEakteService(repository, dmsFileService, dmsProcedureService, apentries, dbLogger, dmsProperties);
    }

    private void verifyLog(final String level, final String message, final String exception, final String gpId) {
        final ArgumentCaptor<LogEntry> logCaptor = ArgumentCaptor.forClass(LogEntry.class);
        verify(logEntryRepository, atLeastOnce()).save(logCaptor.capture());
        final LogEntry logEntry = logCaptor.getValue();
        assertEquals(level, logEntry.getLevel());
        assertEquals(message, logEntry.getMessage());
        assertEquals(exception, logEntry.getException());
        assertEquals(gpId, logEntry.getGpId());
    }

    private static PscdImport data(final DatensatzStatus status) {
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId(GP_ID);
        data.setStatus(status);
        return data;
    }

    private static DmsObjektResponse response(final String objid) {
        final DmsObjektResponse response = new DmsObjektResponse();
        response.setObjid(objid);
        return response;
    }

}
