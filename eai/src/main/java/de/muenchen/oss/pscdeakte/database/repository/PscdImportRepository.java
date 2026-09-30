package de.muenchen.oss.pscdeakte.database.repository;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PscdImportRepository extends CrudRepository<PscdImport, Integer> {

    PscdImport findByGeschaeftspartnerId(String geschaeftspartnerId);

    List<PscdImport> streamAllByStatusIsNot(DatensatzStatus datensatzStatus);

}
