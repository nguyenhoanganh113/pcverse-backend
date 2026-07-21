package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
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

    private final Keycloak keycloak;
    private final KeycloakAdminProperties keycloakAdminProperties;

    @Override
    public String createUser(CreateUserRequest request) {
        UserRepresentation userRepresentation = new UserRepresentation();
        userRepresentation.setUsername(request.username());
        userRepresentation.setEmail(request.email());
        userRepresentation.setFirstName(request.firstName());
        userRepresentation.setLastName(request.lastName());
        userRepresentation.setEnabled(true);
        userRepresentation.setEmailVerified(true);

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setValue(request.password());
        credentialRepresentation.setTemporary(false);
        userRepresentation.setCredentials(List.of(credentialRepresentation));

        try (Response response = keycloak
                .realm(keycloakAdminProperties.realm())
                .users()
                .create(userRepresentation)) {
            int status = response.getStatus();

            if (status == Response.Status.CONFLICT.getStatusCode()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            if (status != Response.Status.CREATED.getStatusCode()) {
                log.error("Keycloak returned status {} while creating user {}", status, request.username());
                throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            String keycloakUserId = CreatedResponseUtil.getCreatedId(response);
            if (keycloakUserId == null || keycloakUserId.isBlank()) {
                log.error("Keycloak did not return the created user ID for {}", request.username());
                throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
            }

            return keycloakUserId;
        } catch (AppException exception) {
            throw exception;
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null
                    && exception.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            log.error("Keycloak rejected the create-user request for {}", request.username(), exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (ProcessingException exception) {
            // Lỗi kết nối Keycloak
            log.error("Unable to connect to Keycloak while creating user {}", request.username(), exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RuntimeException exception) {
            log.error("Unexpected error while creating user {} in Keycloak", request.username(), exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

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
    public void updateUser(String keycloakUserId, UpdateAdminUserRequest request) {
        try {
            UserResource userResource = userResource(keycloakUserId);
            UserRepresentation user = userResource.toRepresentation();

            user.setEmail(request.email());
            user.setFirstName(request.firstName());
            user.setLastName(request.lastName());
            user.setEmailVerified(true);
            user.setAttributes(removeLocalProfileAttributes(user.getAttributes()));

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
        credential.setType(CredentialRepresentation.PASSWORD);
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
            // Lấy UUID nội bộ của client pc-verse-api và representation của role đã tồn tại.
            RealmResource realmResource = keycloak.realm(keycloakAdminProperties.realm());
            List<ClientRepresentation> clients = realmResource.clients()
                    .findByClientId(keycloakAdminProperties.resourceClientId());
            ClientRepresentation clientRepresentation = clients.stream()
                    .filter(client -> keycloakAdminProperties.resourceClientId().equals(client.getClientId()))
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error("Keycloak resource client {} was not found", keycloakAdminProperties.resourceClientId());
                        return new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
                    });

            ClientResource clientResource = realmResource.clients().get(clientRepresentation.getId());
            RoleRepresentation role = clientResource.roles().get(roleName).toRepresentation();

            // Tương ứng với Keycloak Admin REST API:
            // POST /admin/realms/{realm}/users/{user-id}/role-mappings/clients/{client-id}
            // Body là một mảng RoleRepresentation, nên dù chỉ gán một role vẫn truyền List.of(role).
            UserResource userResource = realmResource.users().get(keycloakUserId);
            userResource.roles()
                    .clientLevel(clientRepresentation.getId())
                    .add(List.of(role));
        } catch (AppException exception) {
            throw exception;
        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();
            log.error(
                    "Keycloak rejected assigning client role {} to user {} with status {}",
                    roleName,
                    keycloakUserId,
                    status,
                    exception
            );
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while assigning client role {} to user {}",
                    roleName,
                    keycloakUserId,
                    exception
            );
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while assigning client role {} to user {} in Keycloak",
                    roleName,
                    keycloakUserId,
                    exception
            );
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    private Map<String, List<String>> removeLocalProfileAttributes(
            Map<String, List<String>> currentAttributes
    ) {
        Map<String, List<String>> attributes = currentAttributes == null
                ? new HashMap<>()
                : new HashMap<>(currentAttributes);

        attributes.remove("phoneNumber");
        attributes.remove("gender");
        attributes.remove("birthdate");
        attributes.remove("picture");
        return attributes;
    }

    private RealmResource realm() {
        return keycloak.realm(keycloakAdminProperties.realm());
    }

    private UserResource userResource(String keycloakUserId) {
        return realm().users().get(keycloakUserId);
    }

    private AppException translateException(
            String operation,
            String keycloakUserId,
            RuntimeException exception
    ) {
        if (exception instanceof AppException appException) {
            return appException;
        }

        if (exception instanceof WebApplicationException webException
                && webException.getResponse() != null
                && webException.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
            return new AppException(ErrorCode.USER_ALREADY_EXISTS);
        }

        if (exception instanceof WebApplicationException || exception instanceof ProcessingException) {
            log.error("Failed to {} in Keycloak for user {}", operation, keycloakUserId, exception);
        } else {
            log.error("Unexpected Keycloak Admin Client error while trying to {} for user {}",
                    operation, keycloakUserId, exception);
        }
        return new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
    }
}
