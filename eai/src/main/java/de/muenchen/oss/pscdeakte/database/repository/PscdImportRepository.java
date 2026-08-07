package de.muenchen.oss.pscdeakte.database.repository;

import de.muenchen.oss.pscdeakte.database.entity.PscdImport;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PscdImportRepository extends CrudRepository<PscdImport, Integer> {

    List<PscdImport> findByGeschaeftspartnerId(String geschaeftspartnerId);

    List<PscdImport> streamAllByStatus(String status);
}
