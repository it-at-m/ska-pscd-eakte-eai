package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.helper.DateHelper;
import de.muenchen.oss.refarch.integration.dms.api.ApentriesApi;
import de.muenchen.oss.refarch.integration.dms.api.FilesApi;
import de.muenchen.oss.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.oss.refarch.integration.dms.api.SubjectAreaUnitsApi;
import de.muenchen.oss.refarch.integration.dms.model.CreateFileDTO;
import de.muenchen.oss.refarch.integration.dms.model.CreateProcedureDTO;
import de.muenchen.oss.refarch.integration.dms.model.CreateSubjectAreaUnitAnfrageDTO;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import de.muenchen.oss.refarch.integration.dms.model.SearchApentryDTO;
import de.muenchen.oss.refarch.integration.dms.model.SearchApentryResponseDTO;
import de.muenchen.oss.refarch.integration.dms.model.UserFormsReferenz;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class DmsService {

    private final de.muenchen.oss.pscdeakte.dms.DmsProperties dmsProperties;

    private final ApentriesApi apentriesApi;
    private final SubjectAreaUnitsApi subjectAreaUnitsApi;
    private final FilesApi filesApi;
    private final ProceduresApi proceduresApi;

    public ReadApentryAntwortDTO getApentries() {
        return apentriesApi.readApentry(dmsProperties.getCooEinzelakte(), dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(30)).block();
    }

    public SearchApentryResponseDTO getApentryFor(final int lfdnr) {
        SearchApentryDTO dto = new SearchApentryDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + lfdnr);
        return apentriesApi.searchApentry(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(30)).block();
    }

    public DmsObjektResponse createSubjectAreaUnit(final int laufendeNr, final String bereich) {
        CreateSubjectAreaUnitAnfrageDTO dto = new CreateSubjectAreaUnitAnfrageDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + laufendeNr);
        dto.setShortterm(bereich);
        dto.setObjaddress(dmsProperties.getCooEinzelakte());
        return subjectAreaUnitsApi.createSubjectAreaUnit(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(30)).block();
    }

    public DmsObjektResponse createFile(final PscdImport data) {
        CreateFileDTO dto = new CreateFileDTO();
//        TODO fallback falls data.getBetreffseinheit().isEmpty()?
        dto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt()).apentry(data.getBetreffseinheit()).definition(dmsProperties.getCooKmAkte());
        if (data.getVorname() != null && !data.getVorname().isEmpty()) {
            UserFormsReferenz vornameReferenz = new UserFormsReferenz();
            vornameReferenz.lhMBAI151700Ufreference("BusinessDataGPFirstname").addLhMBAI151700UfvalueItem(data.getVorname());
            dto.addUserformsdataItem(vornameReferenz);
        }
        if (data.getName() != null && !data.getName().isEmpty()) {
            UserFormsReferenz nameReferenz = new UserFormsReferenz();
            nameReferenz.lhMBAI151700Ufreference("BusinessDataGPSurname").addLhMBAI151700UfvalueItem(data.getName());
            dto.addUserformsdataItem(nameReferenz);
        }
        if (data.getGeburtsdatum() != null && !data.getGeburtsdatum().isEmpty()) {
            UserFormsReferenz gebDatReferenz = new UserFormsReferenz();
            gebDatReferenz.lhMBAI151700Ufreference("BusinessDataGPBirthDate").addLhMBAI151700UfvalueItem(DateHelper.format(data.getGeburtsdatum()));
            dto.addUserformsdataItem(gebDatReferenz);
        }
        return filesApi.createFile(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(30)).block();
    }

    public DmsObjektResponse createProcedureBestandsakte(final String referrednumber) {
        CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("Bestandsakten").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(30)).block();
    }

    public DmsObjektResponse createProcedureAV(final String referrednumber) {
        CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("AVs, Titel, Haftbefehle").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(30)).block();
    }
}
