package de.muenchen.oss.pscdeakte.database.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "logs", schema = "pscdeakte")
@NoArgsConstructor
public class LogEntry extends BaseEntity {

    private String level;
    private String message;
    private String exception;
}
