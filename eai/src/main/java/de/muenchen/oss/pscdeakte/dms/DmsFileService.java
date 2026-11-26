package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.api.FilesApi;
import de.muenchen.oss.eakte.v1.generated.api.UserFormsDataApi;
import de.muenchen.oss.eakte.v1.generated.model.CreateFileDTO;
import de.muenchen.oss.eakte.v1.generated.model.DmsObjektResponse;
import de.muenchen.oss.eakte.v1.generated.model.SearchFileRequestDTO;
import de.muenchen.oss.eakte.v1.generated.model.SearchFileResponseDTO;
import de.muenchen.oss.eakte.v1.generated.model.UpdateFileDTO;
import de.muenchen.oss.eakte.v1.generated.model.UpdateUserFormsDataRequestDTO;
import de.muenchen.oss.eakte.v1.generated.model.UserFormsReferenz;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.helper.DateHelper;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
@Slf4j
public class DmsFileService {

    private final DmsProperties dmsProperties;
    private final FilesApi filesApi;
    private final UserFormsDataApi userFormsDataApi;

    /**
     * Erzeugt in der Betreffseinheit eine Sachakte (Geschaeftspartner)
     *
     * @param data zu speichernde Daten der Sachakte
     * @return Response der eAkte
     */
    public DmsObjektResponse createFile(final PscdImport data) {
        final CreateFileDTO dto = new CreateFileDTO();
        dto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt()).apentry(data.getBetreffseinheit()).definition(dmsProperties.getCooKmAkte());
        dto.userformsdata(getUserFormsData(data));
        return filesApi.createFile(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(), dmsProperties.getJobposition());
    }

    public void updateFile(final PscdImport data) {
        formatForUpdate(data);
        final UpdateUserFormsDataRequestDTO dto = new UpdateUserFormsDataRequestDTO();
        dto.userformsdata(getUserFormsData(data));
        userFormsDataApi
                .updateUserFormsData(dmsProperties.getUserlogin(), data.getAkte(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                        dmsProperties.getJobposition());
        final UpdateFileDTO ufdto = new UpdateFileDTO();
        ufdto.shortname(data.getGeschaeftspartnerId()).filesubj(data.getZentralakt());
        filesApi.updateFile(dmsProperties.getUserlogin(), data.getAkte(), ufdto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(),
                dmsProperties.getJobposition());
    }

    private static List<UserFormsReferenz> getUserFormsData(final PscdImport data) {
        final List<UserFormsReferenz> userFormsData = new ArrayList<>();
        if (data.getVorname() != null && !data.getVorname().isEmpty()) {
            final UserFormsReferenz vornameReferenz = new UserFormsReferenz();
            vornameReferenz.lhMBAI151700Ufreference("BusinessDataGPFirstname").addLhMBAI151700UfvalueItem(data.getVorname());
            userFormsData.add(vornameReferenz);
        }
        if (data.getName() != null && !data.getName().isEmpty()) {
            final UserFormsReferenz nameReferenz = new UserFormsReferenz();
            nameReferenz.lhMBAI151700Ufreference("BusinessDataGPSurname").addLhMBAI151700UfvalueItem(data.getName());
            userFormsData.add(nameReferenz);
        }
        if (data.getGeburtsdatum() != null && !data.getGeburtsdatum().isEmpty()) {
            final UserFormsReferenz gebDatReferenz = new UserFormsReferenz();
            gebDatReferenz.lhMBAI151700Ufreference("BusinessDataGPBirthDate").addLhMBAI151700UfvalueItem(DateHelper.format(data.getGeburtsdatum()));
            userFormsData.add(gebDatReferenz);
        }
        return userFormsData;
    }

    public SearchFileResponseDTO searchFiles(final PscdImport data) {
        final SearchFileRequestDTO dto = new SearchFileRequestDTO();
        dto.apentry(data.getBetreffseinheit()).objmlname(data.getGeschaeftspartnerId());
        return filesApi.searchFile(dmsProperties.getUserlogin(), dto, dmsProperties.getXAnwendung(), dmsProperties.getJoboe(), dmsProperties.getJobposition());
    }

    private void formatForUpdate(final PscdImport data) {
        // null und "" fuehrt in der eAkte nicht zu einem Update/Entfernen des Feldes, daher workaround: " ".
        // bei geburtsdatum geht das nicht, da " " kein Datumsformat ist.
        data.setVorname(data.getVorname() == null || data.getVorname().isEmpty() ? " " : data.getVorname());
        data.setName(data.getName() == null || data.getName().isEmpty() ? " " : data.getName());
        data.setZentralakt(data.getZentralakt() == null || data.getZentralakt().isEmpty() ? " " : data.getZentralakt());
    }
}
