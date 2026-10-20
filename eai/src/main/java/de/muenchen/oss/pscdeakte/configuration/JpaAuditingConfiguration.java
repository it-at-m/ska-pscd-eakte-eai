package de.muenchen.oss.pscdeakte.configuration;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Die Annotation {@link EnableJpaAuditing} ist erforderlich um z.B. die Annotation
 * {@link CreatedDate} in Klasse {@link de.muenchen.oss.pscdeakte.database.entity.BaseEntity}
 * funktionsfähig zu machen.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfiguration {
}
