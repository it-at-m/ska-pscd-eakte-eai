package de.muenchen.oss.pscdeakte.database.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Entity
@Table(name = "logs", schema = "pscdeakte")
@NoArgsConstructor
public class LogEntry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;
    private String message;
    private String exception;
}