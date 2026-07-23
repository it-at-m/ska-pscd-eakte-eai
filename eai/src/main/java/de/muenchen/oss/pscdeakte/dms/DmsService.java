package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.pscdeakte.data.PscdData;
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
import de.muenchen.oss.refarch.integration.dms.model.UserFormsReferenz;
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

    public ReadApentryAntwortDTO getApentries(){
        return apentriesApi.readApentry(dmsProperties.getCooEinzelakte(), dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition()).block();
    }

    public DmsObjektResponse createSubjectAreaUnit(final int laufendeNr, final String bereich){
        CreateSubjectAreaUnitAnfrageDTO dto = new CreateSubjectAreaUnitAnfrageDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + laufendeNr);
        dto.setShortterm(bereich);
        dto.setObjaddress(dmsProperties.getCooEinzelakte());
        return subjectAreaUnitsApi.createSubjectAreaUnit(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition()).block();
    }

    public DmsObjektResponse createFile(final PscdData data, final String cooSubjectArea){
        CreateFileDTO dto = new CreateFileDTO();
        dto.shortname(data.getGpId()).filesubj(data.getZentralaktkennung()).apentry(cooSubjectArea).definition(dmsProperties.getCooKmAkte());
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
        if (data.getGebDat() != null && !data.getGebDat().isEmpty()) {
            UserFormsReferenz gebDatReferenz = new UserFormsReferenz();
            gebDatReferenz.lhMBAI151700Ufreference("BusinessDataGPBirthDate").addLhMBAI151700UfvalueItem(DateHelper.format(data.getGebDat()));
            dto.addUserformsdataItem(gebDatReferenz);
        }
        return filesApi.createFile(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition()).block();
    }

    public DmsObjektResponse createProcedureBestandsakte(final String referrednumber){
        CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("Bestandsakten").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi.createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition()).block();
    }

    public DmsObjektResponse createProcedureAV(final String referrednumber){
        CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname("AVs, Titel, Haftbefehle").accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi.createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition()).block();
    }
}
