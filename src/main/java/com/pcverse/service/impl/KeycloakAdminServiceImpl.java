package com.pcverse.service.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private final RestClient keycloakRestClient;
    private final KeycloakAdminProperties properties;

    @Override
    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        try {
            String accessToken = getServiceAccountAccessToken();

            keycloakRestClient.put()
                    .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), keycloakUserId)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of("enabled", enabled))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.error("Failed to update enabled status for Keycloak user {}", keycloakUserId, exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private String getServiceAccountAccessToken() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");

        try {
            KeycloakTokenResponse response = keycloakRestClient.post()
                    .uri("/realms/{realm}/protocol/openid-connect/token", properties.realm())
                    .headers(headers -> headers.setBasicAuth(properties.clientId(), properties.clientSecret()))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
                throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            return response.accessToken();
        } catch (RestClientException exception) {
            log.error("Failed to obtain Keycloak Admin API access token", exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private record KeycloakTokenResponse(
            @JsonProperty("access_token") String accessToken
    ) {
    }
}
