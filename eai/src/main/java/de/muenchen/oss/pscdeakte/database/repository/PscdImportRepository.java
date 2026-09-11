package de.muenchen.oss.pscdeakte.database.repository;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.util.List;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
// Review: Ggfs umstellen auf Jparepository anstelle von Crud
public interface PscdImportRepository extends CrudRepository<PscdImport, Integer> {

    List<PscdImport> findByGeschaeftspartnerId(String geschaeftspartnerId);

    List<PscdImport> streamAllByStatusIsNot(DatensatzStatus datensatzStatus);

    long countByGeschaeftspartnerId(String geschaeftspartnerId);
}
