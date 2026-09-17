package de.muenchen.oss.pscdeakte;


import de.muenchen.oss.pscdeakte.s3.S3Properties;
import de.muenchen.oss.refarch.integration.s3.application.port.out.S3OutPort;
import de.muenchen.oss.refarch.integration.s3.domain.exception.S3Exception;
import de.muenchen.oss.refarch.integration.s3.domain.model.ListResult;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Component;
import de.muenchen.oss.pscdeakte.service.CsvProcessingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class CsvToDb {

    @Getter
    private final S3OutPort s3;
    private final S3Properties props;
    private final CsvProcessingService csvProcessingService;


    public void processFiles() throws S3Exception {
        log.info("Processing CSV files");
        final ListResult list = s3.getFilesWithPrefix(props.getBucket(), props.getPrefix(), true);
        log.info("{} files found", list.files().size());
        list.files().forEach(file -> csvProcessingService.saveFileToDb(file.path()));
    }


}
