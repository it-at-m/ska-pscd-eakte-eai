package de.muenchen.oss.pscdeakte.database.entity;

import de.muenchen.oss.pscdeakte.database.DatensatzStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.io.Serial;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Persistable;

@Getter
@Setter
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "pscd_import", schema = "pscdeakte")
@NoArgsConstructor
public class PscdImport extends BaseEntity implements Persistable<String> {

    @Serial
    private static final long serialVersionUID = 1L;
    //   GP-ID,Name,Vorname,Geb.-Datum,Zentralakt

    @Id
    @Column(name = "geschaeftspartner_id", nullable = false)
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
    @Enumerated(EnumType.STRING)
    private DatensatzStatus status;

    @Override
    public @Nullable String getId() {
        return geschaeftspartnerId;
    }

    @Override
    public boolean isNew() {
        return getCreatedTime() == null;
    }
}
