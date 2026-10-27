package de.muenchen.oss.pscdeakte.dms;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.refarch.integration.dms.api.ApentriesApi;
import de.muenchen.oss.refarch.integration.dms.api.FilesApi;
import de.muenchen.oss.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.oss.refarch.integration.dms.api.SubjectAreaUnitsApi;
import de.muenchen.oss.refarch.integration.dms.api.UserFormsDataApi;
import de.muenchen.oss.refarch.integration.dms.model.CreateFileDTO;
import de.muenchen.oss.refarch.integration.dms.model.CreateProcedureDTO;
import de.muenchen.oss.refarch.integration.dms.model.CreateSubjectAreaUnitAnfrageDTO;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import de.muenchen.oss.refarch.integration.dms.model.SearchApentryDTO;
import de.muenchen.oss.refarch.integration.dms.model.SearchApentryResponseDTO;
import de.muenchen.oss.refarch.integration.dms.model.UpdateFileDTO;
import de.muenchen.oss.refarch.integration.dms.model.UpdateUserFormsDataRequestDTO;
import de.muenchen.oss.refarch.integration.dms.model.UserFormsReferenz;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class DmsServiceTest {

    private static final String AKTENPLANNUMMER = "9512";
    private static final String COO_EINZELAKTE = "coo-einzelakte";
    private static final String COO_KM_AKTE = "coo-km-akte";
    private static final String X_ANWENDUNG = "x-anwendung";
    private static final String USERLOGIN = "userlogin";
    private static final String JOBOE = "joboe";
    private static final String JOBPOSITION = "jobposition";

    @Mock
    private ApentriesApi apentriesApi;

    @Mock
    private SubjectAreaUnitsApi subjectAreaUnitsApi;

    @Mock
    private FilesApi filesApi;

    @Mock
    private ProceduresApi proceduresApi;

    @Mock
    private UserFormsDataApi userFormsDataApi;

    @Test
    void getApentriesReturnsResponseFromApi() {
        final ReadApentryAntwortDTO response = new ReadApentryAntwortDTO();
        when(apentriesApi.readApentry(COO_EINZELAKTE, X_ANWENDUNG, USERLOGIN, JOBOE, JOBPOSITION)).thenReturn(Mono.just(response));

        assertSame(response, dmsService().getApentries());
    }

    @Test
    void getApentryForSendsConfiguredBaseNumber() {
        final SearchApentryResponseDTO response = new SearchApentryResponseDTO();
        when(apentriesApi.searchApentry(any(SearchApentryDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION)))
                .thenReturn(Mono.just(response));

        assertSame(response, dmsService().getApentryFor(4));

        final ArgumentCaptor<SearchApentryDTO> requestCaptor = ArgumentCaptor.forClass(SearchApentryDTO.class);
        verify(apentriesApi).searchApentry(requestCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION));
        Assertions.assertEquals(AKTENPLANNUMMER + ".4", requestCaptor.getValue().getBasenr());
    }

    @Test
    void createSubjectAreaUnitSendsConfiguredData() {
        final DmsObjektResponse response = new DmsObjektResponse();
        when(subjectAreaUnitsApi.createSubjectAreaUnit(any(CreateSubjectAreaUnitAnfrageDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION)))
                .thenReturn(Mono.just(response));

        assertSame(response, dmsService().createSubjectAreaUnit(4, "bereich"));

        final ArgumentCaptor<CreateSubjectAreaUnitAnfrageDTO> requestCaptor = ArgumentCaptor.forClass(CreateSubjectAreaUnitAnfrageDTO.class);
        verify(subjectAreaUnitsApi).createSubjectAreaUnit(requestCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION));
        Assertions.assertEquals(AKTENPLANNUMMER + ".4", requestCaptor.getValue().getBasenr());
        Assertions.assertEquals("bereich", requestCaptor.getValue().getShortterm());
        Assertions.assertEquals(COO_EINZELAKTE, requestCaptor.getValue().getObjaddress());
    }

    @Test
    void createFileHandlesMissingEmptyAndPopulatedOptionalFields() {
        final DmsObjektResponse response = new DmsObjektResponse();
        when(filesApi.createFile(any(CreateFileDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION))).thenReturn(Mono.just(response));
        final DmsService dmsService = dmsService();

        assertSame(response, dmsService.createFile(fileData(null, null, null)));
        assertSame(response, dmsService.createFile(fileData("", "", "")));
        assertSame(response, dmsService.createFile(fileData("Vorname", "Name", "01.02.2020")));

        final ArgumentCaptor<CreateFileDTO> requestCaptor = ArgumentCaptor.forClass(CreateFileDTO.class);
        verify(filesApi, times(3)).createFile(requestCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION));
        assertFileRequest(requestCaptor.getAllValues().get(0));
        Assertions.assertEquals(List.of(), requestCaptor.getAllValues().get(0).getUserformsdata());
        assertFileRequest(requestCaptor.getAllValues().get(1));
        Assertions.assertEquals(List.of(), requestCaptor.getAllValues().get(1).getUserformsdata());
        assertFileRequest(requestCaptor.getAllValues().get(2));
        assertUserForms(Objects.requireNonNull(requestCaptor.getAllValues().get(2).getUserformsdata()));
    }

    @Test
    void updateFileUpdatesUserFormsDataAndFile() {
        when(userFormsDataApi.updateUserFormsData(eq("akte"), any(UpdateUserFormsDataRequestDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE),
                eq(JOBPOSITION))).thenReturn(Mono.empty());
        when(filesApi.updateFile(eq("akte"), any(UpdateFileDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION))).thenReturn(Mono.empty());

        dmsService().updateFile(fileData("Vorname", "Name", "01.02.2020"));

        final ArgumentCaptor<UpdateUserFormsDataRequestDTO> userFormsCaptor = ArgumentCaptor.forClass(UpdateUserFormsDataRequestDTO.class);
        verify(userFormsDataApi).updateUserFormsData(eq("akte"), userFormsCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE),
                eq(JOBPOSITION));
        assertUserForms(Objects.requireNonNull(userFormsCaptor.getValue().getUserformsdata()));
        final ArgumentCaptor<UpdateFileDTO> fileCaptor = ArgumentCaptor.forClass(UpdateFileDTO.class);
        verify(filesApi).updateFile(eq("akte"), fileCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION));
        Assertions.assertEquals("gp-id", fileCaptor.getValue().getShortname());
        Assertions.assertEquals("zentralakt", fileCaptor.getValue().getFilesubj());
    }

    @Test
    void createProceduresUseExpectedShortNames() {
        final DmsObjektResponse response = new DmsObjektResponse();
        when(proceduresApi.createProcedure(any(CreateProcedureDTO.class), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION)))
                .thenReturn(Mono.just(response));
        final DmsService dmsService = dmsService();

        assertSame(response, dmsService.createProcedureBestandsakte("coo-akte"));
        assertSame(response, dmsService.createProcedureAV("coo-akte"));

        final ArgumentCaptor<CreateProcedureDTO> requestCaptor = ArgumentCaptor.forClass(CreateProcedureDTO.class);
        verify(proceduresApi, times(2)).createProcedure(requestCaptor.capture(), eq(X_ANWENDUNG), eq(USERLOGIN), eq(JOBOE), eq(JOBPOSITION));
        Assertions.assertEquals("Bestandsakten", requestCaptor.getAllValues().get(0).getShortname());
        Assertions.assertEquals("AVs, Titel, Haftbefehle", requestCaptor.getAllValues().get(1).getShortname());
        for (CreateProcedureDTO request : requestCaptor.getAllValues()) {
            Assertions.assertEquals("Aktengebunden", request.getAccdef());
            Assertions.assertEquals("coo-akte", request.getReferrednumber());
        }
    }

    private DmsService dmsService() {
        return new DmsService(properties(), apentriesApi, subjectAreaUnitsApi, filesApi, proceduresApi, userFormsDataApi);
    }

    private static DmsProperties properties() {
        final DmsProperties properties = new DmsProperties();
        properties.setAktenplannummer(AKTENPLANNUMMER);
        properties.setCooEinzelakte(COO_EINZELAKTE);
        properties.setCooKmAkte(COO_KM_AKTE);
        properties.setXAnwendung(X_ANWENDUNG);
        properties.setUserlogin(USERLOGIN);
        properties.setJoboe(JOBOE);
        properties.setJobposition(JOBPOSITION);
        return properties;
    }

    private static PscdImport fileData(final String vorname, final String name, final String geburtsdatum) {
        final PscdImport data = new PscdImport();
        data.setGeschaeftspartnerId("gp-id");
        data.setVorname(vorname);
        data.setName(name);
        data.setGeburtsdatum(geburtsdatum);
        data.setZentralakt("zentralakt");
        data.setBetreffseinheit("betreffseinheit");
        data.setAkte("akte");
        return data;
    }

    private static void assertFileRequest(final CreateFileDTO request) {
        Assertions.assertEquals("gp-id", request.getShortname());
        Assertions.assertEquals("zentralakt", request.getFilesubj());
        Assertions.assertEquals("betreffseinheit", request.getApentry());
        Assertions.assertEquals(COO_KM_AKTE, request.getDefinition());
    }

    private static void assertUserForms(final List<UserFormsReferenz> userForms) {
        Assertions.assertEquals(3, userForms.size());
        Assertions.assertEquals("BusinessDataGPFirstname", userForms.get(0).getLhMBAI151700Ufreference());
        Assertions.assertEquals(List.of("Vorname"), userForms.get(0).getLhMBAI151700Ufvalue());
        Assertions.assertEquals("BusinessDataGPSurname", userForms.get(1).getLhMBAI151700Ufreference());
        Assertions.assertEquals(List.of("Name"), userForms.get(1).getLhMBAI151700Ufvalue());
        Assertions.assertEquals("BusinessDataGPBirthDate", userForms.get(2).getLhMBAI151700Ufreference());
        Assertions.assertEquals(List.of("2020-02-01"), userForms.get(2).getLhMBAI151700Ufvalue());
    }

}
