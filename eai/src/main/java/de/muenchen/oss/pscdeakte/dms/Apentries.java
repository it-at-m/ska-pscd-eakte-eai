package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

@Log4j2
@RequiredArgsConstructor
@Component
public class Apentries {

    @Autowired
    public Apentries(DmsService dmsService, DmsProperties properties){
        this.dmsService = dmsService;
        this.dmsProperties = properties;
//        wg. Testsystem
        pattern = Pattern.compile(dmsProperties.getAktenplannummer() + "\\.[0-9]+/[0-9]{10}-[0-9]{10}");
    }

    private final DmsService dmsService;
    private final DmsProperties dmsProperties;
    private Map<Integer, String> apentryMap;
    private final Pattern pattern;

    public String getApentryCoo(String gpId){
        if (apentryMap == null){
            apentryMap = new HashMap<>();
            log.info("reading apentries");
            ReadApentryAntwortDTO response = dmsService.getApentries();
            if (response.getGiobjecttype() != null) {
                log.info("{} apentries found", response.getGiobjecttype().size());
                response.getGiobjecttype().forEach(this::fillMap);
            }
        }
        int lfdnr = this.generateLfdNr(gpId);
        String coo = apentryMap.get(lfdnr);
        if (coo == null) {
            log.info("creating new apentry");
            DmsObjektResponse response = dmsService.createSubjectAreaUnit(lfdnr, this.buildObjname(lfdnr));
            log.info("new apentry name: {} coo: {}", response.getObjname(), response.getObjid());
            apentryMap.put(lfdnr, response.getObjid());
            coo = response.getObjid();
        }
        return coo;
    }

    private void fillMap(Objektreferenz ref){
        String objname = ref.getObjname();
//        komplex wegen Testsystem
        if (objname != null && !objname.isEmpty() && pattern.matcher(objname).find()) {
            log.info("saving apentry name: {} coo: {}", ref.getObjname(), ref.getObjaddress());
            apentryMap.put(Integer.parseInt(objname.substring(objname.indexOf('.'), objname.indexOf('/'))), ref.getObjaddress());
        } else {
            log.warn("apentry does not match criteria: {}", ref.getObjname());
        }
    }

    public int generateLfdNr(String gpId){
        return (Integer.parseInt(gpId) - 1000000001) / 5000 + 1;
    }

    public String buildObjname(int lfdNr){
        int first = 1000000000;
        int begin = first + (lfdNr - 1) * 5000 + 1;
        int end = first + lfdNr * 5000;
        return dmsProperties.getAktenplannummer() + "." + lfdNr + "/" + begin + "-" + end;
    }

}
