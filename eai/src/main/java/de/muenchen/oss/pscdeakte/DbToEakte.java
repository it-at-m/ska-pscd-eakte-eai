package de.muenchen.oss.pscdeakte;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import de.muenchen.oss.pscdeakte.database.repository.PscdImportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@RequiredArgsConstructor
@Component
public class DbToEakte {

    private final EAkteConnector eakte;
    private final PscdImportRepository repo;

    public void start(){
//        TODO wiederaufnahme unfertiger Datensaetze
        List<PscdImport> list = repo.streamAllByStatus(DatensatzStatus.NEW.getValue());
        list.forEach(this::process);
    }

    public void process(PscdImport data){
        data.setStatus(DatensatzStatus.STARTED.getValue());
        eakte.process(data);
        data.setStatus(DatensatzStatus.DONE.getValue());
        repo.save(data);
    }

}
