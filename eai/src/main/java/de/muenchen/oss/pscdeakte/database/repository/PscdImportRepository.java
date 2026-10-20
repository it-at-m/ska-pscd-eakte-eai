package de.muenchen.oss.pscdeakte.database.repository;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PscdImportRepository extends CrudRepository<PscdImport, String> {

    Optional<PscdImport> findByGeschaeftspartnerId(String geschaeftspartnerId);

    List<PscdImport> streamAllByStatusIsNotOrderByCreatedTimeAsc(DatensatzStatus datensatzStatus);

}
