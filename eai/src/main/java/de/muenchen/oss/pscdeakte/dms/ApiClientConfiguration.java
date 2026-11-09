package de.muenchen.oss.pscdeakte.dms;

import de.muenchen.oss.eakte.v1.generated.api.ApentriesApi;
import de.muenchen.oss.eakte.v1.generated.api.FilesApi;
import de.muenchen.oss.eakte.v1.generated.api.ProceduresApi;
import de.muenchen.oss.eakte.v1.generated.api.SubjectAreaUnitsApi;
import de.muenchen.oss.eakte.v1.generated.api.UserFormsDataApi;
import de.muenchen.oss.eakte.v1.generated.dms.ApiClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
class ApiClientConfiguration {
    @Bean
    public ApiClient apiClient(final DmsProperties dmsProperties) {
        final RestClient webClient = RestClient.builder().build();
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
    public SubjectAreaUnitsApi subjectAreaUnitsApi(final ApiClient apiClient) {
        return new SubjectAreaUnitsApi(apiClient);
    }

    @Bean
    public FilesApi filesApi(final ApiClient apiClient) {
        return new FilesApi(apiClient);
    }

    @Bean
    public ProceduresApi proceduresApi(final ApiClient apiClient) {
        return new ProceduresApi(apiClient);
    }

    @Bean
    public UserFormsDataApi userFormsDataApi(final ApiClient apiClient) {
        return new UserFormsDataApi(apiClient);
    }

}
