package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.helper.DateHelper;
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
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DmsService {

    public static final int TIMEOUT = 5;
    private final de.muenchen.oss.pscdeakte.dms.DmsProperties dmsProperties;

    private final ApentriesApi apentriesApi;
    private final SubjectAreaUnitsApi subjectAreaUnitsApi;
    private final FilesApi filesApi;
    private final ProceduresApi proceduresApi;
    private final UserFormsDataApi userFormsDataApi;

    public ReadApentryAntwortDTO getApentries() {
        return apentriesApi.readApentry(dmsProperties.getCooEinzelakte(), dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    public SearchApentryResponseDTO getApentryFor(final int lfdnr) {
        final SearchApentryDTO dto = new SearchApentryDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + lfdnr);
        return apentriesApi.searchApentry(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    public DmsObjektResponse createSubjectAreaUnit(final int laufendeNr, final String bereich) {
        final CreateSubjectAreaUnitAnfrageDTO dto = new CreateSubjectAreaUnitAnfrageDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + laufendeNr);
        dto.setShortterm(bereich);
        dto.setObjaddress(dmsProperties.getCooEinzelakte());
        return subjectAreaUnitsApi.createSubjectAreaUnit(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    public DmsObjektResponse createFile(final PscdImport data) {
        final CreateFileDTO dto = new CreateFileDTO();
        dto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt()).apentry(data.getBetreffseinheit()).definition(dmsProperties.getCooKmAkte());
        dto.userformsdata(getUserFormsData(data));
        return filesApi.createFile(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    public void updateFile(PscdImport data) {
        final UpdateUserFormsDataRequestDTO dto = new UpdateUserFormsDataRequestDTO();
        dto.userformsdata(getUserFormsData(data));
        userFormsDataApi
                .updateUserFormsData(data.getAkte(), dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                        dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(TIMEOUT)).block();
        final UpdateFileDTO ufdto = new UpdateFileDTO();
        ufdto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt());
        filesApi.updateFile(data.getAkte(), ufdto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    private static List<UserFormsReferenz> getUserFormsData(PscdImport data) {
        List<UserFormsReferenz> list = new ArrayList<>();
        if (data.getVorname() != null && !data.getVorname().isEmpty()) {
            final UserFormsReferenz vornameReferenz = new UserFormsReferenz();
            vornameReferenz.lhMBAI151700Ufreference("BusinessDataGPFirstname").addLhMBAI151700UfvalueItem(data.getVorname());
            list.add(vornameReferenz);
        }
        if (data.getName() != null && !data.getName().isEmpty()) {
            final UserFormsReferenz nameReferenz = new UserFormsReferenz();
            nameReferenz.lhMBAI151700Ufreference("BusinessDataGPSurname").addLhMBAI151700UfvalueItem(data.getName());
            list.add(nameReferenz);
        }
        if (data.getGeburtsdatum() != null && !data.getGeburtsdatum().isEmpty()) {
            final UserFormsReferenz gebDatReferenz = new UserFormsReferenz();
            gebDatReferenz.lhMBAI151700Ufreference("BusinessDataGPBirthDate").addLhMBAI151700UfvalueItem(DateHelper.format(data.getGeburtsdatum()));
            list.add(gebDatReferenz);
        }
        return list;
    }

    public DmsObjektResponse createProcedureBestandsakte(final String referrednumber) {
        final CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("Bestandsakten").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(TIMEOUT)).block();
    }

    public DmsObjektResponse createProcedureAV(final String referrednumber) {
        final CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("AVs, Titel, Haftbefehle").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(TIMEOUT)).block();
    }
}
