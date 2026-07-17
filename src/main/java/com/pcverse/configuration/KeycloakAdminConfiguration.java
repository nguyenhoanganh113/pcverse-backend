package com.pcverse.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(KeycloakAdminProperties.class)
public class KeycloakAdminConfiguration {

    @Bean
    RestClient keycloakRestClient(KeycloakAdminProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.serverUrl())
                .build();
    }
}
