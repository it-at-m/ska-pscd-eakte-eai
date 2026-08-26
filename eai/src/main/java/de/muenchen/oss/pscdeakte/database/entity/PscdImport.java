package de.muenchen.oss.pscdeakte.database.entity;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.LastModifiedDate;

@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "pscd_import", schema = "pscdeakte")
@NoArgsConstructor
public class PscdImport extends BaseEntity {

    //   GP-ID,Name,Vorname,Geb.-Datum,Zentralakt

    @Column(name = "geschaeftspartner_id")
    private String geschaeftspartnerId;

    @Column(name = "name")
    private String name;

    @Column(name = "vorname")
    private String vorname;

    @Column(name = "geburtsdatum")
    private String geburtsdatum;

    @Column(name = "zentralakt")
    private String zentralakt;

    @Column(name = "betreffseinheit")
    private String betreffseinheit;

    @Column(name = "akte")
    private String akte;

    @Column(name = "bestandsakt")
    private String bestandsakt;

    @Column(name = "av")
    private String av;

    @Column(name = "status")
    private DatensatzStatus status;

    @Column(name = "statustext")
    private String statustext;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant lastUpdate;

    @PrePersist
    protected void onCreate() {
        lastUpdate = Instant.now();
    }

}



