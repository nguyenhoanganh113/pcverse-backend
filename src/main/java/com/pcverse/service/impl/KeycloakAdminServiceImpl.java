package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.enums.KeycloakRequiredAction;
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
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

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
        userRepresentation.setEmailVerified(false);
        userRepresentation.setRequiredActions(List.of(
                KeycloakRequiredAction.VERIFY_EMAIL.providerId()
        ));

        CredentialRepresentation credentialRepresentation = new CredentialRepresentation();
        credentialRepresentation.setValue(request.password());
        credentialRepresentation.setType(CredentialRepresentation.PASSWORD);
        credentialRepresentation.setTemporary(false);

        List<CredentialRepresentation> list = new ArrayList<>();
        list.add(credentialRepresentation);
        userRepresentation.setCredentials(list);

        UsersResource usersResource = keycloak.realm(keycloakAdminProperties.realm()).users();

        try (Response response = usersResource.create(userRepresentation)) {
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
    public void updateUserEnabledStatus(
            String keycloakUserId,
            boolean enabled
    ) {
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
            RealmResource realmResource = keycloak.realm(keycloakAdminProperties.realm());

            UserRepresentation userRepresentation = new UserRepresentation();

            userRepresentation.setEmail(request.email());
            userRepresentation.setFirstName(request.firstName());
            userRepresentation.setLastName(request.lastName());

            realmResource.users()
                    .get(keycloakUserId)
                    .update(userRepresentation);

        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null
                    && exception.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            log.error("Keycloak rejected updating user {}", keycloakUserId, exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error("Unable to connect to Keycloak while updating user {}", keycloakUserId, exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RuntimeException exception) {
            log.error("Unexpected error while updating user {} in Keycloak", keycloakUserId, exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public boolean deleteUser(String keycloakUserId) {
        try {
            userResource(keycloakUserId).remove();
            return true;
        } catch (WebApplicationException exception) {
            if (exception.getResponse() != null
                    && exception.getResponse().getStatus()
                    == Response.Status.NOT_FOUND.getStatusCode()) {
                log.info("Keycloak user {} was already deleted", keycloakUserId);
                return false;
            }

            throw translateException(
                    "delete user",
                    keycloakUserId,
                    exception
            );
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

    @Override
    public void removeClientRole(String keycloakUserId, String roleName) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            // Tìm UUID nội bộ của client pc-verse-api.
            List<ClientRepresentation> clients = realmResource.clients()
                    .findByClientId(keycloakAdminProperties.resourceClientId());

            ClientRepresentation clientRepresentation = clients.stream()
                    .filter(client ->
                            keycloakAdminProperties.resourceClientId()
                                    .equals(client.getClientId())
                    )
                    .findFirst()
                    .orElseThrow(() -> {
                        log.error(
                                "Keycloak resource client {} was not found",
                                keycloakAdminProperties.resourceClientId()
                        );
                        return new AppException(
                                ErrorCode.KEYCLOAK_ADMIN_API_ERROR
                        );
                    });

            // Lấy representation của role cần gỡ.
            ClientResource clientResource = realmResource.clients()
                    .get(clientRepresentation.getId());

            RoleRepresentation role = clientResource.roles()
                    .get(roleName)
                    .toRepresentation();

            UserResource userResource = realmResource.users()
                    .get(keycloakUserId);

            // DELETE /admin/realms/{realm}/users/{user-id}
            //        /role-mappings/clients/{client-id}
            userResource.roles()
                    .clientLevel(clientRepresentation.getId())
                    .remove(List.of(role));

        } catch (AppException exception) {
            throw exception;

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected removing client role {} from user {} with status {}",
                    roleName,
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while removing client role {} from user {}",
                    roleName,
                    keycloakUserId,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while removing client role {} from user {} in Keycloak",
                    roleName,
                    keycloakUserId,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );
        }
    }

    @Override
    public void logoutUser(String keycloakUserId) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            UserResource userResource = realmResource.users()
                    .get(keycloakUserId);

            // POST /admin/realms/{realm}/users/{user-id}/logout
            // Xóa toàn bộ session và vô hiệu khả năng refresh token của user.
            userResource.logout();
        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected logging out user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while logging out user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );
        } catch (RuntimeException exception) {log.error(
                "Unexpected error while logging out user {} from Keycloak",
                keycloakUserId,
                exception
        );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );
        }
    }

    @Override
    public List<UserSessionResponse> getUserSessions(String keycloakUserId) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            List<UserSessionRepresentation> sessions = realmResource.users()
                    .get(keycloakUserId)
                    .getUserSessions();

            return sessions.stream()
                    .map(session -> new UserSessionResponse(
                            session.getId(),
                            session.getIpAddress(),
                            Instant.ofEpochMilli(session.getStart()),
                            Instant.ofEpochMilli(session.getLastAccess()),
                            session.isRememberMe(),
                            session.getClients() == null
                                    ? List.of()
                                    : session.getClients().values().stream()
                                            .sorted()
                                            .toList()
                    ))
                    .toList();

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected getting sessions for user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while getting sessions for user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while getting sessions for user {} from Keycloak",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public List<UserCredentialResponse> getUserCredentials(String keycloakUserId) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            return realmResource.users()
                    .get(keycloakUserId)
                    .credentials()
                    .stream()
                    .map(credential -> new UserCredentialResponse(
                            credential.getId(),
                            credential.getType(),
                            credential.getUserLabel(),
                            credential.getCreatedDate() == null
                                    ? null
                                    : Instant.ofEpochMilli(credential.getCreatedDate())
                    ))
                    .toList();

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected getting credentials for user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while getting credentials for user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while getting credentials for user {} from Keycloak",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void deleteUserCredential(
            String keycloakUserId,
            String credentialId
    ) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            // DELETE /admin/realms/{realm}/users/{user-id}/credentials/{credential-id}
            realmResource.users()
                    .get(keycloakUserId)
                    .removeCredential(credentialId);

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            if (status != null
                    && status == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new AppException(ErrorCode.USER_CREDENTIAL_NOT_FOUND);
            }

            log.error(
                    "Keycloak rejected deleting credential {} for user {} with status {}",
                    credentialId,
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while deleting credential {} for user {}",
                    credentialId,
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while deleting credential {} for user {} from Keycloak",
                    credentialId,
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void deleteUserSession(String sessionId) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            // DELETE /admin/realms/{realm}/sessions/{session}?isOffline=false
            realmResource.deleteSession(sessionId, false);

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            if (status != null
                    && status == Response.Status.NOT_FOUND.getStatusCode()) {
                throw new AppException(ErrorCode.USER_SESSION_NOT_FOUND);
            }

            log.error(
                    "Keycloak rejected deleting user session {} with status {}",
                    sessionId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while deleting user session {}",
                    sessionId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while deleting user session {} from Keycloak",
                    sessionId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void sendRequiredActionsEmail(
            String keycloakUserId,
            List<KeycloakRequiredAction> actions,
            int lifespanSeconds
    ) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            List<String> actionNames = actions.stream()
                    .map(KeycloakRequiredAction::providerId)
                    .distinct()
                    .toList();

            // PUT /admin/realms/{realm}/users/{user-id}/execute-actions-email
            realmResource.users()
                    .get(keycloakUserId)
                    .executeActionsEmail(actionNames, lifespanSeconds);

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected sending required-actions email to user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while sending required-actions email to user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while sending required-actions email to user {} from Keycloak",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void updateRequiredActions(
            String keycloakUserId,
            List<KeycloakRequiredAction> actions
    ) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            List<String> actionNames = actions.stream()
                    .map(KeycloakRequiredAction::providerId)
                    .distinct()
                    .toList();

            UserRepresentation userRepresentation = new UserRepresentation();
            userRepresentation.setRequiredActions(actionNames);

            // PUT /admin/realms/{realm}/users/{user-id}
            realmResource.users()
                    .get(keycloakUserId)
                    .update(userRepresentation);

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected updating required actions for user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while updating required actions for user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while updating required actions for user {} in Keycloak",
                    keycloakUserId,
                    exception
            );

            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
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
