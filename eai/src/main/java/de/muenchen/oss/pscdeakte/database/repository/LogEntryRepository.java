package de.muenchen.oss.pscdeakte.database.repository;

import de.muenchen.oss.pscdeakte.database.entity.LogEntry;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogEntryRepository extends CrudRepository<LogEntry, String> {

}
