package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;

public record Metadata(
        JsonNode jsonNode,
        Map<String, String> indexFields) {
}
