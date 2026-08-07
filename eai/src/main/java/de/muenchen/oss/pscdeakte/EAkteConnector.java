package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.dms.Apentries;
import de.muenchen.oss.pscdeakte.dms.DmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.springframework.stereotype.Component;

@Log4j2
@RequiredArgsConstructor
@Component("eAkteConnector")
public class EAkteConnector implements Processor {

    private final DmsService dmsService;
    private final Apentries apentries;

    @Override
    public void process(final Exchange exchange){
        throw new UnsupportedOperationException();
    }

    public void process(final PscdImport data){
        if (data == null) {
            log.error("data is null");
            return;
        }
        log.info("processing gpid {}", data.getGeschaeftspartnerId());
        data.setStatus(DatensatzStatus.STARTED.getValue());
        String apentryCoo = apentries.getApentryCoo(data.getGeschaeftspartnerId());
        data.setStatus(DatensatzStatus.APENTRY_EXISTS.getValue());
        log.info("saving gp {} into apentry {}", data.getGeschaeftspartnerId(), apentryCoo);
        log.info("creating file");
        String filecoo = dmsService.createFile(data, apentryCoo).getObjid();
        data.setStatus(DatensatzStatus.FILE_CREATED.getValue());
        log.info("file {} created", filecoo);
        log.info("creating procedures");
        String procedurecoo = dmsService.createProcedureBestandsakte(filecoo).getObjid();
        data.setStatus(DatensatzStatus.BESTANDSAKT_CREATED.getValue());
        log.info("Bestandsakten created: {}", procedurecoo);
        procedurecoo = dmsService.createProcedureAV(filecoo).getObjid();
        data.setStatus(DatensatzStatus.AV_CREATED.getValue());
        log.info("AVs, Titel, Haftbefehle created: {}", procedurecoo);
    }
}
