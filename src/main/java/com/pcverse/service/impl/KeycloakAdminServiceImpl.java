package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
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
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
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
            UserResource userResource = userResource(keycloakUserId);
            UserRepresentation userRepresentation =
                    userResource.toRepresentation();
            boolean emailChanged = userRepresentation.getEmail() == null
                    || !userRepresentation.getEmail().equalsIgnoreCase(request.email());

            userRepresentation.setEmail(request.email());
            userRepresentation.setFirstName(request.firstName());
            userRepresentation.setLastName(request.lastName());

            if (emailChanged) {
                userRepresentation.setEmailVerified(false);

                List<String> requiredActions =
                        userRepresentation.getRequiredActions() == null
                                ? new ArrayList<>()
                                : new ArrayList<>(
                                        userRepresentation.getRequiredActions()
                                );

                String verifyEmailAction =
                        KeycloakRequiredAction.VERIFY_EMAIL.providerId();
                if (!requiredActions.contains(verifyEmailAction)) {
                    requiredActions.add(verifyEmailAction);
                }
                userRepresentation.setRequiredActions(requiredActions);
            }

            userResource.update(userRepresentation);

        } catch (WebApplicationException exception) {
            Integer status = exception.getResponse() == null
                    ? null
                    : exception.getResponse().getStatus();

            if (status != null
                    && status == Response.Status.CONFLICT.getStatusCode()) {
                throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            log.error(
                    "Keycloak rejected updating user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );
            throw new AppException(resolveUserOperationError(status));

        } catch (ProcessingException exception) {
            log.error("Unable to connect to Keycloak while updating user {}", keycloakUserId, exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RuntimeException exception) {
            log.error("Unexpected error while updating user {} in Keycloak", keycloakUserId, exception);
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void updateUserProfile(
            String keycloakUserId,
            String firstName,
            String lastName
    ) {
        try {
            UserResource userResource = userResource(keycloakUserId);
            UserRepresentation userRepresentation = userResource.toRepresentation();

            userRepresentation.setFirstName(firstName);
            userRepresentation.setLastName(lastName);
            userResource.update(userRepresentation);
        } catch (RuntimeException exception) {
            throw translateException(
                    "update user profile",
                    keycloakUserId,
                    exception
            );
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
        } catch (WebApplicationException exception) {
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();

            ErrorCode errorCode = status == 400
                    ? ErrorCode.PASSWORD_POLICY_VIOLATION
                    : resolveUserOperationError(status);

            log.error("Keycloak rejected resetting password for user {} "
                            + "with status {}",
                    keycloakUserId,
                    status,
                    exception
            );
            throw new AppException(errorCode);
        } catch (RuntimeException exception) {
            throw translateException("reset password", keycloakUserId, exception);
        }
    }

    @Override
    public void assignRealmRole(String keycloakUserId, String roleName) {
        try {
            RealmResource realmResource = realm();
            RoleRepresentation role = requireRealmRole(realmResource, roleName);

            // Tương ứng với Keycloak Admin REST API:
            // POST /admin/realms/{realm}/users/{user-id}/role-mappings/realm
            // Body là một mảng RoleRepresentation, nên dù chỉ gán một role vẫn truyền List.of(role).
            UserResource userResource = realmResource.users().get(keycloakUserId);
            userResource.roles()
                    .realmLevel()
                    .add(List.of(role));
        } catch (AppException exception) {
            throw exception;
        } catch (WebApplicationException exception) {
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();
            log.error(
                    "Keycloak rejected assigning realm role {} to user {} with status {}",
                    roleName,
                    keycloakUserId,
                    status,
                    exception
            );
            throw new AppException(resolveUserOperationError(status));
        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while assigning realm role {} to user {}",
                    roleName,
                    keycloakUserId,
                    exception
            );
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while assigning realm role {} to user {} in Keycloak",
                    roleName,
                    keycloakUserId,
                    exception
            );
            throw new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
        }
    }

    @Override
    public void removeRealmRole(String keycloakUserId, String roleName) {
        try {
            RealmResource realmResource = realm();
            RoleRepresentation role = requireRealmRole(realmResource, roleName);

            UserResource userResource = realmResource.users()
                    .get(keycloakUserId);

            // DELETE /admin/realms/{realm}/users/{user-id}
            //        /role-mappings/realm
            userResource.roles()
                    .realmLevel()
                    .remove(List.of(role));

        } catch (AppException exception) {
            throw exception;

        } catch (WebApplicationException exception) {
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected removing realm role {} from user {} with status {}",
                    roleName,
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(resolveUserOperationError(status));

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while removing realm role {} from user {}",
                    roleName,
                    keycloakUserId,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );

        } catch (RuntimeException exception) {
            log.error(
                    "Unexpected error while removing realm role {} from user {} in Keycloak",
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
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected logging out user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(resolveUserOperationError(status));

        } catch (ProcessingException exception) {
            log.error(
                    "Unable to connect to Keycloak while logging out user {}",
                    keycloakUserId,
                    exception
            );

            throw new AppException(
                    ErrorCode.KEYCLOAK_ADMIN_API_ERROR
            );
        } catch (RuntimeException exception) {
            log.error(
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
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected getting sessions for user {} with status {}",
                    keycloakUserId,
                    status,
                    exception
            );

            throw new AppException(resolveUserOperationError(status));

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
    public void deleteUserSession(String sessionId) {
        try {
            RealmResource realmResource =
                    keycloak.realm(keycloakAdminProperties.realm());

            // DELETE /admin/realms/{realm}/sessions/{session}?isOffline=false
            realmResource.deleteSession(sessionId, false);

        } catch (WebApplicationException exception) {
            int status = exception.getResponse() == null
                    ? 0
                    : exception.getResponse().getStatus();

            log.error(
                    "Keycloak rejected deleting user session {} with status {}",
                    sessionId,
                    status,
                    exception
            );

            ErrorCode errorCode = status == 404
                    ? ErrorCode.USER_SESSION_NOT_FOUND
                    : resolveUserOperationError(status);

            throw new AppException(errorCode);

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

            throw new AppException(resolveUserOperationError(status));

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

            throw new AppException(resolveUserOperationError(status));

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

    private RoleRepresentation requireRealmRole(
            RealmResource realmResource,
            String roleName
    ) {
        try {
            return realmResource.roles()
                    .get(roleName)
                    .toRepresentation();
        } catch (WebApplicationException exception) {
            if (hasStatus(exception, Response.Status.NOT_FOUND)) {
                log.info(
                        "Keycloak realm role {} was not found",
                        roleName
                );
                throw new AppException(ErrorCode.ROLE_NOT_FOUND);
            }
            throw exception;
        }
    }

    private boolean hasStatus(
            WebApplicationException exception,
            Response.Status expectedStatus
    ) {
        return exception.getResponse() != null
                && exception.getResponse().getStatus()
                == expectedStatus.getStatusCode();
    }

    private ErrorCode resolveUserOperationError(Integer status) {
        if (status == null) {
            return ErrorCode.KEYCLOAK_ADMIN_API_ERROR;
        }

        return switch (status) {
            case 403 -> ErrorCode.KEYCLOAK_PERMISSION_DENIED;
            case 404 -> ErrorCode.KEYCLOAK_USER_NOT_FOUND;
            default -> ErrorCode.KEYCLOAK_ADMIN_API_ERROR;
        };
    }

    private AppException translateException(
            String operation,
            String keycloakUserId,
            RuntimeException exception
    ) {
        if (exception instanceof AppException appException) {
            return appException;
        }

        if (exception instanceof WebApplicationException webException) {
            Integer status = webException.getResponse() == null
                    ? null
                    : webException.getResponse().getStatus();

            if (status != null
                    && status == Response.Status.CONFLICT.getStatusCode()) {
                return new AppException(ErrorCode.USER_ALREADY_EXISTS);
            }

            log.error(
                    "Failed to {} in Keycloak for user {} with status {}",
                    operation,
                    keycloakUserId,
                    status,
                    exception
            );
            return new AppException(resolveUserOperationError(status));
        }

        if (exception instanceof ProcessingException) {
            log.error("Failed to {} in Keycloak for user {}", operation, keycloakUserId, exception);
        } else {
            log.error("Unexpected Keycloak Admin Client error while trying to {} for user {}",
                    operation, keycloakUserId, exception);
        }
        return new AppException(ErrorCode.KEYCLOAK_ADMIN_API_ERROR);
    }
}
