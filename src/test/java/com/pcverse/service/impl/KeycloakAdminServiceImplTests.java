package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.dto.request.CreateAdminUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.enums.Gender;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URI;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
    void createAdminUserCreatesUserAndAssignsAdminRole() {
        when(realm.users()).thenReturn(users);
        when(users.create(any(UserRepresentation.class))).thenReturn(Response.created(
                URI.create("http://localhost:8090/admin/realms/pc-verse/users/keycloak-user-id")
        ).build());
        prepareRoleAssignment("keycloak-user-id", "ADMIN");

        String userId = service.createAdminUser(createRequest());

        assertThat(userId).isEqualTo("keycloak-user-id");

        ArgumentCaptor<UserRepresentation> userCaptor = ArgumentCaptor.forClass(UserRepresentation.class);
        verify(users).create(userCaptor.capture());
        UserRepresentation createdUser = userCaptor.getValue();
        assertThat(createdUser.getUsername()).isEqualTo("admin");
        assertThat(createdUser.getEmail()).isEqualTo("admin@pcverse.com");
        assertThat(createdUser.isEnabled()).isTrue();
        assertThat(createdUser.getAttributes())
                .containsEntry("phoneNumber", List.of("0900000000"))
                .containsEntry("gender", List.of("MALE"));
        assertThat(createdUser.getCredentials()).singleElement().satisfies(credential -> {
            assertThat(credential.getType()).isEqualTo("password");
            assertThat(credential.getValue()).isEqualTo("password123");
            assertThat(credential.isTemporary()).isFalse();
        });
        verify(roleScope).add(any());
    }

    @Test
    void createAdminUserMapsConflictToUserAlreadyExists() {
        when(realm.users()).thenReturn(users);
        when(users.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(Response.Status.CONFLICT).build());

        assertThatThrownBy(() -> service.createAdminUser(createRequest()))
                .isInstanceOf(UserServiceException.class)
                .extracting(exception -> ((UserServiceException) exception).getErrorCode())
                .isEqualTo(ErrorCode.USER_ALREADY_EXISTS);
    }

    @Test
    void updateUserPreservesUnmanagedAttributes() {
        when(realm.users()).thenReturn(users);
        when(users.get("keycloak-user-id")).thenReturn(userResource);

        UserRepresentation current = new UserRepresentation();
        current.setUsername("unchanged-admin");
        current.setAttributes(new HashMap<>(Map.of("department", List.of("sales"))));
        when(userResource.toRepresentation()).thenReturn(current);

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
        assertThat(updated.getUsername()).isEqualTo("unchanged-admin");
        assertThat(updated.getAttributes())
                .containsEntry("department", List.of("sales"))
                .containsEntry("phoneNumber", List.of("0911111111"))
                .containsEntry("gender", List.of("FEMALE"))
                .doesNotContainKey("picture");
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

    private CreateAdminUserRequest createRequest() {
        return new CreateAdminUserRequest(
                "admin@pcverse.com",
                "admin",
                "password123",
                "PCVerse",
                "Admin",
                "0900000000",
                Gender.MALE,
                LocalDate.of(1990, 1, 1),
                "https://cdn.pcverse.com/admin.png"
        );
    }
}
