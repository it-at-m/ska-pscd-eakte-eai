package de.muenchen.oss.ska_pscd_eakte_eai.swimLibs.handlercore.domain.model;

public sealed interface FileEvent
        permits SingleFileEvent, MultiFileEvent {
    String useCase();
}
