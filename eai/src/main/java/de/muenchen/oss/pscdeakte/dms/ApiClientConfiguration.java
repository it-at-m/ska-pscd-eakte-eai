package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.refarch.integration.dms.ApiClient;
import de.muenchen.oss.refarch.integration.dms.api.ApentriesApi;
import de.muenchen.oss.refarch.integration.dms.api.FilesApi;
import de.muenchen.oss.refarch.integration.dms.api.ProceduresApi;
import de.muenchen.oss.refarch.integration.dms.api.SubjectAreaUnitsApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
class ApiClientConfiguration {
    @Bean
    public ApiClient apiClient(final DmsProperties dmsProperties) {
        final WebClient webClient = WebClient.builder().build();
        final ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(dmsProperties.getBaseUrl());
        apiClient.setUsername(dmsProperties.getUsername());
        apiClient.setPassword(dmsProperties.getPassword());
        return apiClient;
    }

    @Bean
    public ApentriesApi apentriesApi(final ApiClient apiClient) {
        return new ApentriesApi(apiClient);
    }

    @Bean
    public SubjectAreaUnitsApi subjectAreaUnitsApi(final ApiClient apiClient) { return new SubjectAreaUnitsApi(apiClient); }

    @Bean
    public FilesApi filesApi(final ApiClient apiClient) { return new FilesApi(apiClient); }

    @Bean
    public ProceduresApi proceduresApi(final ApiClient apiClient) { return new ProceduresApi(apiClient); }

}
