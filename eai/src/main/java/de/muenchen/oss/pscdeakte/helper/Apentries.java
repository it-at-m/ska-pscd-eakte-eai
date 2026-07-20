package de.muenchen.oss.pscdeakte.helper;

import de.muenchen.oss.pscdeakte.dms.DmsService;
import de.muenchen.oss.refarch.integration.dms.model.DmsObjektResponse;
import de.muenchen.oss.refarch.integration.dms.model.Objektreferenz;
import de.muenchen.oss.refarch.integration.dms.model.ReadApentryAntwortDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class Apentries {

    @Autowired
    public Apentries(DmsService service){
        this.dmsService = service;
    }

    DmsService dmsService;
    private Map<Integer, String> apentryMap;

    public String getApentryCoo(String gpId){
        if (apentryMap == null){
            apentryMap = new HashMap<>();
            ReadApentryAntwortDTO response = dmsService.getApentries();
            response.getGiobjecttype().forEach(this::fillMap);
        }
        int id = Integer.parseInt(gpId);
        int lfdnr = id / 5000;
        String coo = apentryMap.get(lfdnr);
        if (coo == null) {
            DmsObjektResponse response = dmsService.createSubjectAreaUnit(lfdnr, this.buildObjname(lfdnr));
//            Todo objName -> int (fillMap umbauen und verwenden)
            apentryMap.put(Integer.parseInt(response.getObjname()), response.getObjid());
        }
        return coo;
    }

    private String buildObjname(int lfdNr){
//        Todo 9512.lfdNr/10stelligBeginn-10stelligEnde aufbauen
        String s = "";
        return s;
    }

    private void fillMap(Objektreferenz ref){
        String objname = ref.getObjname();
//        komplex wegen Testsystem
        if (objname == null || objname.isEmpty() || !(objname.startsWith("9512.") && objname.contains("/") && objname.contains("-"))){
            return;
        }
        String firstId = objname.substring(objname.indexOf('/'), objname.indexOf('-')).trim();
        apentryMap.put(Integer.parseInt(firstId), ref.getObjaddress());
    }

}
