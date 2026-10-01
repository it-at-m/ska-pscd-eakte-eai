package de.muenchen.oss.pscdeakte.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    /**
     * Deaktivierung der Security.
     *
     * @param http HttpSecurity
     * @return SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) {
        http
                .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::disable))
                .authorizeHttpRequests(request -> request.anyRequest().permitAll());
        return http.build();
    }

}
