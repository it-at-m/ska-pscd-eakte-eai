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
        final SearchApentryDTO dto = new SearchApentryDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + lfdnr);
        return apentriesApi.searchApentry(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(30)).block();
    }

    /**
     *  Erzeugt in der Einzelakte eine Betreffseinheit
     * @param laufendeNr laufende Nummer für das Geschaeftszeichen des uebergeordneten Aktenplaneintrags
     * @param bereich Titel der anzulegenden Betreffseinheit
     * @return Response der eAkte
     */
    public DmsObjektResponse createSubjectAreaUnit(final int laufendeNr, final String bereich) {
        final CreateSubjectAreaUnitAnfrageDTO dto = new CreateSubjectAreaUnitAnfrageDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + laufendeNr);
        dto.setShortterm(bereich);
        dto.setObjaddress(dmsProperties.getCooEinzelakte());
        return subjectAreaUnitsApi.createSubjectAreaUnit(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition()).timeout(Duration.ofSeconds(30)).block();
    }

    /**
     * Erzeugt in der Betreffseinheit eine Sachakte (Geschaeftspartner)
     * @param data zu speichernde Daten der Sachakte
     * @return Response der eAkte
     */
    public DmsObjektResponse createFile(final PscdImport data) {
        final CreateFileDTO dto = new CreateFileDTO();
        //        TODO fallback falls data.getBetreffseinheit().isEmpty()?
        dto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt()).apentry(data.getBetreffseinheit()).definition(dmsProperties.getCooKmAkte());
        if (data.getVorname() != null && !data.getVorname().isEmpty()) {
            final UserFormsReferenz vornameReferenz = new UserFormsReferenz();
            // Review: woher kommt der Name? Kann sich der ändern? ist egal
            vornameReferenz.lhMBAI151700Ufreference("BusinessDataGPFirstname").addLhMBAI151700UfvalueItem(data.getVorname());
            dto.addUserformsdataItem(vornameReferenz);
        }
        if (data.getName() != null && !data.getName().isEmpty()) {
            final UserFormsReferenz nameReferenz = new UserFormsReferenz();
            // Review: woher kommt der Name? Kann sich der ändern?
            nameReferenz.lhMBAI151700Ufreference("BusinessDataGPSurname").addLhMBAI151700UfvalueItem(data.getName());
            dto.addUserformsdataItem(nameReferenz);
        }
        if (data.getGeburtsdatum() != null && !data.getGeburtsdatum().isEmpty()) {
            final UserFormsReferenz gebDatReferenz = new UserFormsReferenz();
            // Review: woher kommt der Name? Kann sich der ändern?
            gebDatReferenz.lhMBAI151700Ufreference("BusinessDataGPBirthDate").addLhMBAI151700UfvalueItem(DateHelper.format(data.getGeburtsdatum()));
            dto.addUserformsdataItem(gebDatReferenz);
        }
        return filesApi.createFile(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(30)).block();
    }

    /**
     * Bei der Erstellung einer Sachakte muss ein darin enthaltener Vorgang des Typs 'Bestandsakte' erzeugt werden.
     * @param referrednumber uebergeordnete Sachakte
     * @return Response der ProceduresApi
     */
    public DmsObjektResponse createProcedureBestandsakte(final String referrednumber) {
        return createProcedure(referrednumber, "Bestandsakten");
    }

    /**
     * Bei der Erstellung einer Sachakte muss ein darin enthaltener Vorgang des Typs 'AVs, Titel, Haftbefehle' erzeugt werden.
     * @param referrednumber uebergeordnete Sachakte
     * @return Response der ProceduresApi
     */
    public DmsObjektResponse createProcedureAV(final String referrednumber) {
        return createProcedure(referrednumber, "AVs, Titel, Haftbefehle");
    }

    /**
     * Kapselt den Aufruf der ProceduresApi.
     * @param referrednumber uebergeordnete Sachakte
     * @param shortname Typ des anzulegenden Vorgangs
     * @return Response der ProceduresApi
     */
    private DmsObjektResponse createProcedure(final String referrednumber, final String shortname) {
        final CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname(shortname).accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dto, dmsProperties.getXAnwendung(), dmsProperties.getUserlogin(), dmsProperties.getJoboe(), dmsProperties.getJobposition())
                .timeout(Duration.ofSeconds(30)).block();
    }
}
