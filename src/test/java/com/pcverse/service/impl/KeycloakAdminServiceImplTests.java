package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.enums.Gender;
import com.pcverse.enums.KeycloakRequiredAction;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.ClientResource;
import org.keycloak.admin.client.resource.ClientsResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.representations.idm.UserSessionRepresentation;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KeycloakAdminServiceImplTests {

    private static final String REALM_NAME = "pc-verse";
    private static final String RESOURCE_CLIENT_ID = "pc-verse-api";

    @Mock
    private Keycloak keycloak;
    @Mock
    private RealmResource realm;
    @Mock
    private UsersResource users;
    @Mock
    private UserResource userResource;
    @Mock
    private ClientsResource clients;
    @Mock
    private ClientResource clientResource;
    @Mock
    private RolesResource roles;
    @Mock
    private RoleResource roleResource;
    @Mock
    private RoleMappingResource roleMapping;
    @Mock
    private RoleScopeResource roleScope;
    @Mock
    private Response createUserResponse;
    private KeycloakAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        KeycloakAdminProperties properties = new KeycloakAdminProperties(
                "http://localhost:8090",
                REALM_NAME,
                "pc-verse-admin",
                "secret",
                RESOURCE_CLIENT_ID
        );
        service = new KeycloakAdminServiceImpl(keycloak, properties);
        when(keycloak.realm(REALM_NAME)).thenReturn(realm);
    }

    @Test
    void createUserReturnsKeycloakUserId() {
        when(realm.users()).thenReturn(users);
        when(users.create(org.mockito.ArgumentMatchers.any(UserRepresentation.class)))
                .thenReturn(createUserResponse);
        when(createUserResponse.getStatus()).thenReturn(Response.Status.CREATED.getStatusCode());
        when(createUserResponse.getStatusInfo()).thenReturn(Response.Status.CREATED);
        when(createUserResponse.getLocation()).thenReturn(
                URI.create("http://localhost:8090/admin/realms/pc-verse/users/keycloak-user-id")
        );

        String keycloakUserId = service.createUser(createUserRequest());

        assertThat(keycloakUserId).isEqualTo("keycloak-user-id");

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        UserRepresentation createdUser = userCaptor.getValue();
        assertThat(createdUser.getUsername()).isEqualTo("new-user");
        assertThat(createdUser.getEmail()).isEqualTo("new-user@pcverse.com");
        assertThat(createdUser.isEnabled()).isTrue();
        assertThat(createdUser.isEmailVerified()).isFalse();
        assertThat(createdUser.getCredentials()).isNullOrEmpty();
        assertThat(createdUser.getRequiredActions())
                .containsExactly(
                        KeycloakRequiredAction.VERIFY_EMAIL.providerId(),
                        KeycloakRequiredAction.UPDATE_PASSWORD.providerId()
                );
        verify(createUserResponse).close();
    }

    @Test
    void createUserMapsConflictToUserAlreadyExists() {
        when(realm.users()).thenReturn(users);
        when(users.create(org.mockito.ArgumentMatchers.any(UserRepresentation.class)))
                .thenReturn(createUserResponse);
        when(createUserResponse.getStatus()).thenReturn(Response.Status.CONFLICT.getStatusCode());

        assertThatThrownBy(() -> service.createUser(createUserRequest()))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS));
    }

    @Test
    void updateUserSendsOnlyRequestedIdentityFields() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        UpdateAdminUserRequest request = new UpdateAdminUserRequest(
                "updated@pcverse.com",
                "Updated",
                "Admin",
                "0911111111",
                Gender.FEMALE,
                LocalDate.of(1995, 5, 20),
                null
        );

        service.updateUser("keycloak-user-id", request);

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(userCaptor.capture());
        UserRepresentation updated = userCaptor.getValue();
        assertThat(updated.getUsername()).isNull();
        assertThat(updated.getEmail()).isEqualTo("updated@pcverse.com");
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("Admin");
        assertThat(updated.getAttributes()).isNull();
    }

    @Test
    void resetPasswordUsesRequestedTemporaryFlag() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        service.resetPassword("keycloak-user-id", "new-password", true);

        ArgumentCaptor<CredentialRepresentation> credentialCaptor =
                ArgumentCaptor.forClass(CredentialRepresentation.class);
        verify(userResource).resetPassword(credentialCaptor.capture());
        CredentialRepresentation credential = credentialCaptor.getValue();
        assertThat(credential.getType()).isEqualTo("password");
        assertThat(credential.getValue()).isEqualTo("new-password");
        assertThat(credential.isTemporary()).isTrue();
    }

    @Test
    void assignClientRoleMapsExistingRoleToUser() {
        prepareRoleAssignment("keycloak-user-id", "CUSTOMER");

        service.assignClientRole("keycloak-user-id", "CUSTOMER");

        verify(roleScope).add(argThat(assignedRoles ->
                assignedRoles.size() == 1
                        && "CUSTOMER".equals(assignedRoles.get(0).getName())
        ));
    }

    @Test
    void getUserSessionsMapsKeycloakRepresentationToResponse() {
        UserSessionRepresentation session = new UserSessionRepresentation();
        session.setId("session-id");
        session.setIpAddress("127.0.0.1");
        session.setStart(1_720_000_000_000L);
        session.setLastAccess(1_720_000_300_000L);
        session.setRememberMe(true);
        session.setClients(Map.of(
                "client-uuid-2", "pc-verse-client",
                "client-uuid-1", "account"
        ));

        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);
        when(userResource.getUserSessions()).thenReturn(List.of(session));

        List<UserSessionResponse> result = service.getUserSessions("keycloak-user-id");

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.sessionId()).isEqualTo("session-id");
            assertThat(response.ipAddress()).isEqualTo("127.0.0.1");
            assertThat(response.startedAt()).isEqualTo(Instant.ofEpochMilli(1_720_000_000_000L));
            assertThat(response.lastAccessAt()).isEqualTo(Instant.ofEpochMilli(1_720_000_300_000L));
            assertThat(response.rememberMe()).isTrue();
            assertThat(response.clients()).containsExactly("account", "pc-verse-client");
        });
    }

    @Test
    void getUserCredentialsMapsOnlySafeCredentialMetadata() {
        CredentialRepresentation password = new CredentialRepresentation();
        password.setId("password-credential-id");
        password.setType(CredentialRepresentation.PASSWORD);
        password.setCreatedDate(1_720_000_000_000L);
        password.setSecretData("must-not-be-exposed");
        password.setCredentialData("must-not-be-exposed");

        CredentialRepresentation otp = new CredentialRepresentation();
        otp.setId("otp-credential-id");
        otp.setType(CredentialRepresentation.TOTP);
        otp.setUserLabel("Authenticator app");

        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);
        when(userResource.credentials()).thenReturn(List.of(password, otp));

        List<UserCredentialResponse> result =
                service.getUserCredentials("keycloak-user-id");

        assertThat(result).containsExactly(
                new UserCredentialResponse(
                        "password-credential-id",
                        "password",
                        null,
                        Instant.ofEpochMilli(1_720_000_000_000L)
                ),
                new UserCredentialResponse(
                        "otp-credential-id",
                        "totp",
                        "Authenticator app",
                        null
                )
        );
    }

    @Test
    void deleteUserCredentialRemovesCredentialFromTargetUser() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        service.deleteUserCredential("keycloak-user-id", "credential-id");

        verify(userResource).removeCredential("credential-id");
    }

    @Test
    void deleteUserCredentialMapsKeycloakNotFoundToUserCredentialNotFound() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);
        doThrow(new NotFoundException())
                .when(userResource)
                .removeCredential("missing-credential-id");

        assertThatThrownBy(() ->
                service.deleteUserCredential(
                        "keycloak-user-id",
                        "missing-credential-id"
                ))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_CREDENTIAL_NOT_FOUND));
    }

    @Test
    void deleteUserSessionDeletesOnlineSession() {
        service.deleteUserSession("session-id");

        verify(realm).deleteSession("session-id", false);
    }

    @Test
    void deleteUserSessionMapsKeycloakNotFoundToUserSessionNotFound() {
        doThrow(new NotFoundException())
                .when(realm)
                .deleteSession("missing-session-id", false);

        assertThatThrownBy(() -> service.deleteUserSession("missing-session-id"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_SESSION_NOT_FOUND));
    }

    @Test
    void sendRequiredActionsEmailMapsEnumsAndRemovesDuplicates() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        service.sendRequiredActionsEmail(
                "keycloak-user-id",
                List.of(
                        KeycloakRequiredAction.UPDATE_PASSWORD,
                        KeycloakRequiredAction.VERIFY_EMAIL,
                        KeycloakRequiredAction.UPDATE_PASSWORD
                ),
                900
        );

        verify(userResource).executeActionsEmail(
                List.of("UPDATE_PASSWORD", "VERIFY_EMAIL"),
                900
        );
    }

    @Test
    void updateRequiredActionsReplacesActionsAndRemovesDuplicates() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        service.updateRequiredActions(
                "keycloak-user-id",
                List.of(
                        KeycloakRequiredAction.UPDATE_PASSWORD,
                        KeycloakRequiredAction.CONFIGURE_TOTP,
                        KeycloakRequiredAction.VERIFY_PROFILE,
                        KeycloakRequiredAction.UPDATE_PASSWORD
                )
        );

        ArgumentCaptor<UserRepresentation> userCaptor =
                ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(userCaptor.capture());

        assertThat(userCaptor.getValue().getRequiredActions())
                .containsExactly(
                        "UPDATE_PASSWORD",
                        "CONFIGURE_TOTP",
                        "VERIFY_PROFILE"
                );
    }

    @Test
    void updateRequiredActionsAcceptsEmptyListToClearActions() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        service.updateRequiredActions("keycloak-user-id", List.of());

        ArgumentCaptor<UserRepresentation> userCaptor =
                ArgumentCaptor.forClass(UserRepresentation.class);
        verify(userResource).update(userCaptor.capture());

        assertThat(userCaptor.getValue().getRequiredActions()).isEmpty();
    }

    private void prepareRoleAssignment(String userId, String roleName) {
        ClientRepresentation client = new ClientRepresentation();
        client.setId("resource-client-uuid");
        client.setClientId(RESOURCE_CLIENT_ID);

        RoleRepresentation role = new RoleRepresentation();
        role.setId("role-id");
        role.setName(roleName);

        when(realm.clients()).thenReturn(clients);
        when(clients.findByClientId(RESOURCE_CLIENT_ID)).thenReturn(List.of(client));
        when(clients.get("resource-client-uuid")).thenReturn(clientResource);
        when(clientResource.roles()).thenReturn(roles);
        when(roles.get(roleName)).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(role);
        when(realm.users()).thenReturn(users);
        when(users.get(userId)).thenReturn(userResource);
        when(userResource.roles()).thenReturn(roleMapping);
        when(roleMapping.clientLevel("resource-client-uuid")).thenReturn(roleScope);
    }

    private CreateUserRequest createUserRequest() {
        return new CreateUserRequest(
                "new-user",
                "new-user@pcverse.com",
                "New",
                "User",
                "0900000000",
                "MALE",
                LocalDate.of(2000, 1, 1),
                null
        );
    }

}
