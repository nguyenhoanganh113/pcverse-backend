package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateAdminUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.service.KeycloakAdminService;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@Service
public class KeycloakAdminServiceImpl implements KeycloakAdminService {

    private static final String PASSWORD_CREDENTIAL_TYPE = "password";
    private static final String ADMIN_ROLE = "ADMIN";

    private final Keycloak keycloakAdminClient;
    private final KeycloakAdminProperties properties;

    @Override
    public void setUserEnabled(String keycloakUserId, boolean enabled) {
        try {
            UserResource userResource = userResource(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();
            user.setEnabled(enabled);
            userResource.update(user);
        } catch (RuntimeException exception) {
            throw translateException("update enabled status", keycloakUserId, exception);
        }
    }

    @Override
    public String createAdminUser(CreateAdminUserRequest request) {
        String keycloakUserId = null;

        try {
            UserRepresentation user = toUserRepresentation(request);

            try (Response response = realm().users().create(user)) {
                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                    throw new UserServiceException(ErrorCode.USER_ALREADY_EXISTS);
                }
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    log.error("Keycloak returned status {} while creating user", response.getStatus());
                    throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
                }

                keycloakUserId = CreatedResponseUtil.getCreatedId(response);
            }

            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                throw new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            assignClientRole(keycloakUserId, ADMIN_ROLE);
            return keycloakUserId;
        } catch (RuntimeException exception) {
            cleanUpCreatedUser(keycloakUserId);
            throw translateException("create admin user", keycloakUserId, exception);
        }
    }

    @Override
    public void updateUser(String keycloakUserId, UpdateAdminUserRequest request) {
        try {
            UserResource userResource = userResource(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();

            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setEmailVerified(true);
            user.setAttributes(mergeAttributes(user.getAttributes(), request));

            userResource.update(user);
        } catch (RuntimeException exception) {
            throw translateException("update user", keycloakUserId, exception);
        }
    }

    @Override
    public void deleteUser(String keycloakUserId) {
        try {
            userResource(keycloakUserId).remove();
        } catch (RuntimeException exception) {
            throw translateException("delete user", keycloakUserId, exception);
        }
    }

    @Override
    public void resetPassword(String keycloakUserId, String newPassword, boolean temporary) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PASSWORD_CREDENTIAL_TYPE);
        credential.setValue(newPassword);
        credential.setTemporary(temporary);

        try {
            userResource(keycloakUserId).resetPassword(credential);
        } catch (RuntimeException exception) {
            throw translateException("reset password", keycloakUserId, exception);
        }
    }

    @Override
    public void assignClientRole(String keycloakUserId, String roleName) {
        try {
            ClientRepresentation client = findResourceClient();
            ClientResource clientResource = realm().clients().get(client.getId());
            RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();

            userResource(keycloakUserId)
                    .roles()
                    .clientLevel(client.getId())
                    .add(List.of(role));
        } catch (RuntimeException exception) {
            throw translateException("assign client role " + roleName, keycloakUserId, exception);
        }
    }

    private UserRepresentation toUserRepresentation(CreateAdminUserRequest request) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.username());
        user.setEmail(request.email());
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEnabled(true);
        user.setEmailVerified(true);
        user.setAttributes(userAttributes(
                request.phoneNumber(),
                request.gender().name(),
                request.dateOfBirth().toString(),
                request.urlAvatar()
        ));

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(PASSWORD_CREDENTIAL_TYPE);
        credential.setValue(request.password());
        credential.setTemporary(false);
        user.setCredentials(List.of(credential));
        return user;
    }

    private Map<String, List<String>> mergeAttributes(
            Map<String, List<String>> currentAttributes,
            UpdateAdminUserRequest request
    ) {
        Map<String, List<String>> attributes = currentAttributes == null
                ? new HashMap<>()
                : new HashMap<>(currentAttributes);

        attributes.put("phoneNumber", List.of(request.phoneNumber()));
        attributes.put("gender", List.of(request.gender().name()));
        attributes.put("birthdate", List.of(request.dateOfBirth().toString()));

        if (request.urlAvatar() == null) {
            attributes.remove("picture");
        } else {
            attributes.put("picture", List.of(request.urlAvatar()));
        }
        return attributes;
    }

    private Map<String, List<String>> userAttributes(
            String phoneNumber,
            String gender,
            String birthdate,
            String picture
    ) {
        Map<String, List<String>> attributes = new HashMap<>();
        attributes.put("phoneNumber", List.of(phoneNumber));
        attributes.put("gender", List.of(gender));
        attributes.put("birthdate", List.of(birthdate));
        if (picture != null) {
            attributes.put("picture", List.of(picture));
        }
        return attributes;
    }

    private ClientRepresentation findResourceClient() {
        List<ClientRepresentation> clients = realm().clients()
                .findByClientId(properties.resourceClientId());

        return clients.stream()
                .filter(client -> properties.resourceClientId().equals(client.getClientId()))
                .findFirst()
                .orElseThrow(() -> {
                    log.error("Keycloak resource client {} was not found", properties.resourceClientId());
                    return new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
                });
    }

    private RealmResource realm() {
        return keycloakAdminClient.realm(properties.realm());
    }

    private UserResource userResource(String keycloakUserId) {
        return realm().users().get(keycloakUserId);
    }

    private void cleanUpCreatedUser(String keycloakUserId) {
        if (keycloakUserId == null) {
            return;
        }

        try {
            userResource(keycloakUserId).remove();
        } catch (RuntimeException cleanupException) {
            log.error("Failed to clean up Keycloak user {} after creation failed",
                    keycloakUserId, cleanupException);
        }
    }

    private UserServiceException translateException(
            String operation,
            String keycloakUserId,
            RuntimeException exception
    ) {
        if (exception instanceof UserServiceException userServiceException) {
            return userServiceException;
        }

        if (exception instanceof WebApplicationException webException
                && webException.getResponse() != null
                && webException.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
            return new UserServiceException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (exception instanceof WebApplicationException || exception instanceof ProcessingException) {
            log.error("Failed to {} in Keycloak for user {}", operation, keycloakUserId, exception);
        } else {
            log.error("Unexpected Keycloak Admin Client error while trying to {} for user {}",
                    operation, keycloakUserId, exception);
        }
        return new UserServiceException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
    }
}
