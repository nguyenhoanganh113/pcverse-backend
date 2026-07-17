package com.pcverse.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateAdminUserRequest;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.service.KeycloakAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
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

    @Override
    public String createAdminUser(CreateAdminUserRequest request) {
        String keycloakUserId = null;

        try {
            String accessToken = getServiceAccountAccessToken();
            keycloakUserId = createKeycloakUser(accessToken, request);
            assignClientRole(accessToken, keycloakUserId, "ADMIN");
            return keycloakUserId;
        } catch (RuntimeException exception) {
            if (keycloakUserId != null) {
                try {
                    deleteUser(keycloakUserId);
                } catch (RuntimeException cleanupException) {
                    log.error("Failed to clean up Keycloak user {} after admin creation failed",
                            keycloakUserId, cleanupException);
                }
            }
            throw exception;
        }
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        try {
            keycloakRestClient.delete()
                    .uri("/admin/realms/{realm}/users/{userId}", properties.realm(), keycloakUserId)
                    .headers(headers -> headers.setBearerAuth(getServiceAccountAccessToken()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.error("Failed to delete Keycloak user {}", keycloakUserId, exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private String createKeycloakUser(String accessToken, CreateAdminUserRequest request) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put("phoneNumber", List.of(request.phoneNumber()));
        attributes.put("gender", List.of(request.gender().name()));
        attributes.put("birthdate", List.of(request.dateOfBirth().toString()));

        if (request.urlAvatar() != null && !request.urlAvatar().isBlank()) {
            attributes.put("picture", List.of(request.urlAvatar()));
        }

        KeycloakUserRepresentation representation = new KeycloakUserRepresentation(
                request.username(),
                request.email(),
                request.firstName(),
                request.lastName(),
                true,
                true,
                attributes,
                List.of(new KeycloakCredential("password", request.password(), false))
        );

        try {
            ResponseEntity<Void> response = keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/users", properties.realm())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(representation)
                    .retrieve()
                    .toBodilessEntity();

            URI location = response.getHeaders().getLocation();
            if (location == null || location.getPath() == null) {
                throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            String path = location.getPath();
            String userId = path.substring(path.lastIndexOf('/') + 1);
            if (userId.isBlank()) {
                throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            return userId;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() == 409) {
                throw new UserServiceException(ErrorCode.USER_ALREADY_EXISTS);
            }

            log.error("Failed to create user in Keycloak", exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RestClientException exception) {
            log.error("Failed to create user in Keycloak", exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private void assignClientRole(String accessToken, String keycloakUserId, String roleName) {
        KeycloakClientRepresentation client = findResourceClient(accessToken);

        KeycloakRoleRepresentation role;
        try {
            role = keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/clients/{clientId}/roles/{roleName}",
                            properties.realm(), client.id(), roleName)
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(KeycloakRoleRepresentation.class);
        } catch (RestClientException exception) {
            log.error("Failed to find Keycloak client role {}", roleName, exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }

        if (role == null || role.id() == null || role.name() == null) {
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }

        try {
            keycloakRestClient.post()
                    .uri("/admin/realms/{realm}/users/{userId}/role-mappings/clients/{clientId}",
                            properties.realm(), keycloakUserId, client.id())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(role))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            log.error("Failed to assign Keycloak role {} to user {}", roleName, keycloakUserId, exception);
            throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private KeycloakClientRepresentation findResourceClient(String accessToken) {
        try {
            List<KeycloakClientRepresentation> clients = keycloakRestClient.get()
                    .uri("/admin/realms/{realm}/clients?clientId={clientId}",
                            properties.realm(), properties.resourceClientId())
                    .headers(headers -> headers.setBearerAuth(accessToken))
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (clients == null || clients.isEmpty() || clients.get(0).id() == null) {
                throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            return clients.get(0);
        } catch (RestClientException exception) {
            log.error("Failed to find Keycloak resource client {}", properties.resourceClientId(), exception);
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakUserRepresentation(
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified,
            Map<String, List<String>> attributes,
            List<KeycloakCredential> credentials
    ) {
    }

    private record KeycloakCredential(
            String type,
            String value,
            boolean temporary
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakClientRepresentation(
            String id,
            String clientId
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record KeycloakRoleRepresentation(
            String id,
            String name
    ) {
    }
}
