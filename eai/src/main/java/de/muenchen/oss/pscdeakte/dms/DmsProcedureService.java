package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.api.ProceduresApi;
import de.muenchen.oss.eakte.v1.generated.model.CreateProcedureDTO;
import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.SearchProcedureRequestDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchProcedureResponseDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DmsProcedureService {

    public static final String BESTANDSAKTEN = "Bestandsakten";
    public static final String AVS_TITEL_HAFTBEFEHLE = "AVs, Titel, Haftbefehle";
    private final DmsProperties dmsProperties;
    private final ProceduresApi proceduresApi;

    /**
     * Bei der Erstellung einer Sachakte muss ein darin enthaltener Vorgang des Typs 'Bestandsakte'
     * erzeugt werden.
     *
     * @param referrednumber uebergeordnete Sachakte
     * @return Response der ProceduresApi
     */
    public DmsObjektResponse createProcedureBestandsakte(final String referrednumber) {
        return createProcedure(referrednumber, BESTANDSAKTEN);
    }

    /**
     * Bei der Erstellung einer Sachakte muss ein darin enthaltener Vorgang des Typs 'AVs, Titel,
     * Haftbefehle' erzeugt werden.
     *
     * @param referrednumber uebergeordnete Sachakte
     * @return Response der ProceduresApi
     */
    public DmsObjektResponse createProcedureAV(final String referrednumber) {
        return createProcedure(referrednumber, AVS_TITEL_HAFTBEFEHLE);
    }

    /**
     * Kapselt den Aufruf der ProceduresApi.
     *
     * @param referrednumber uebergeordnete Sachakte
     * @param shortname Typ des anzulegenden Vorgangs
     * @return Response der ProceduresApi
     */
    private DmsObjektResponse createProcedure(final String referrednumber, final String shortname) {
        final CreateProcedureDTO dto = new CreateProcedureDTO();
        dto.shortname(shortname).accdef("Aktengebunden").referrednumber(referrednumber);
        return proceduresApi
                .createProcedure(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(), dmsProperties.getJobposition());
    }

    public SearchProcedureResponseDTO searchProcedures(final String akteCoo, final String name) {
        final SearchProcedureRequestDTO dto = new SearchProcedureRequestDTO();
        dto.referrednumber(akteCoo).objmlname(name);
        return proceduresApi.searchProcedure(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition());
    }

}
