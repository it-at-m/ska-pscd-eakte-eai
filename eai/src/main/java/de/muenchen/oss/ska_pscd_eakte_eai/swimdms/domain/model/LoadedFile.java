package de.muenchen.oss.ska_pscd_eakte_eai.swimdms.domain.model;

import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.FileReference;
import de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model.Metadata;
import java.io.InputStream;

public record LoadedFile(
        FileReference fileReference,
        InputStream content,
        Metadata metadata) {
}
