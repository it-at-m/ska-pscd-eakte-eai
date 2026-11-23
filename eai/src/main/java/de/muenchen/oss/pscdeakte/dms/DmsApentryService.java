package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.api.ApentriesApi;
import de.muenchen.oss.eakte.v1.generated.api.SubjectAreaUnitsApi;
import de.muenchen.oss.eakte.v1.generated.model.CreateSubjectAreaUnitAnfrageDTO;
import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.ReadApentryAntwortDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchApentryDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchApentryResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DmsApentryService {

    public static final String BESTANDSAKTEN = "Bestandsakten";
    public static final String AVS_TITEL_HAFTBEFEHLE = "AVs, Titel, Haftbefehle";
    private final DmsProperties dmsProperties;
    private final ApentriesApi apentriesApi;
    private final SubjectAreaUnitsApi subjectAreaUnitsApi;

    public ReadApentryAntwortDTO getApentries() {
        return apentriesApi.readApentry(dmsProperties.getUserlogin(), dmsProperties.getCooEinzelakte(), dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition());
    }

    public SearchApentryResponseDTO getApentryFor(final int lfdnr) {
        final SearchApentryDTO dto = new SearchApentryDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + lfdnr);
        return apentriesApi.searchApentry(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition());
    }

    /**
     * Erzeugt in der Einzelakte eine Betreffseinheit
     *
     * @param laufendeNr laufende Nummer für das Geschaeftszeichen des uebergeordneten Aktenplaneintrags
     * @param bereich Titel der anzulegenden Betreffseinheit
     * @return Response der eAkte
     */
    public DmsObjektResponse createSubjectAreaUnit(final int laufendeNr, final String bereich) {
        final CreateSubjectAreaUnitAnfrageDTO dto = new CreateSubjectAreaUnitAnfrageDTO();
        dto.setBasenr(dmsProperties.getAktenplannummer() + "." + laufendeNr);
        dto.setShortterm(bereich);
        dto.setObjaddress(dmsProperties.getCooEinzelakte());
        return subjectAreaUnitsApi.createSubjectAreaUnit(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition());
    }

}
