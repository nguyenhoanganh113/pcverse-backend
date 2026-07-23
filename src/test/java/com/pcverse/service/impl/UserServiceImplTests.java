package com.pcverse.service.impl;

import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.KeycloakRequiredAction;
import com.pcverse.exception.AppException;
import com.pcverse.exception.ErrorCode;
import com.pcverse.mapper.UserMapper;
import com.pcverse.repository.UserRepository;
import com.pcverse.service.KeycloakAdminService;
import com.pcverse.service.RoleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private UserMapper userMapper;
    @Mock
    private RoleService roleService;
    @Mock
    private KeycloakAdminService keycloakAdminService;

    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        userService = new UserServiceImpl(
                userRepository,
                userMapper,
                roleService,
                keycloakAdminService
        );
    }

    @Test
    void getUserByIdReturnsMappedLocalUser() {
        User user = userWithKeycloakId();
        UserDetailsResponse response = UserDetailsResponse.builder()
                .id("local-user-id")
                .username("test-user")
                .roles(List.of("CUSTOMER"))
                .build();

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(userMapper.toUserDetailResponse(user)).thenReturn(response);

        UserDetailsResponse result = userService.getUserById("local-user-id");

        assertThat(result).isSameAs(response);
        verify(userMapper).toUserDetailResponse(user);
    }

    @Test
    void getUserByIdThrowsUserNotFoundWhenLocalUserDoesNotExist() {
        when(userRepository.findById("missing-user-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById("missing-user-id"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_NOT_FOUND));
    }

    @Test
    void getUserSessionsUsesKeycloakIdFromLocalUser() {
        User user = User.builder()
                .id("local-user-id")
                .keycloakId("keycloak-user-id")
                .build();
        UserSessionResponse session = new UserSessionResponse(
                "session-id",
                "127.0.0.1",
                Instant.ofEpochSecond(1_000),
                Instant.ofEpochSecond(2_000),
                false,
                List.of("pc-verse-client")
        );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserSessions("keycloak-user-id"))
                .thenReturn(List.of(session));

        List<UserSessionResponse> result = userService.getUserSessions("local-user-id");

        assertThat(result).containsExactly(session);
        verify(keycloakAdminService).getUserSessions("keycloak-user-id");
    }

    @Test
    void getUserCredentialsUsesKeycloakIdFromLocalUser() {
        User user = userWithKeycloakId();
        UserCredentialResponse credential = new UserCredentialResponse(
                "credential-id",
                "password",
                null,
                Instant.ofEpochMilli(1_720_000_000_000L)
        );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserCredentials("keycloak-user-id"))
                .thenReturn(List.of(credential));

        List<UserCredentialResponse> result =
                userService.getUserCredentials("local-user-id");

        assertThat(result).containsExactly(credential);
        verify(keycloakAdminService).getUserCredentials("keycloak-user-id");
    }

    @Test
    void deleteUserCredentialDeletesCredentialOwnedByTargetUser() {
        User user = userWithKeycloakId();
        UserCredentialResponse credential = new UserCredentialResponse(
                "credential-id",
                "otp",
                "Authenticator app",
                null
        );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserCredentials("keycloak-user-id"))
                .thenReturn(List.of(credential));

        userService.deleteUserCredential("local-user-id", "credential-id");

        verify(keycloakAdminService).deleteUserCredential(
                "keycloak-user-id",
                "credential-id"
        );
    }

    @Test
    void deleteUserCredentialRejectsCredentialOwnedByAnotherUser() {
        User user = userWithKeycloakId();

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserCredentials("keycloak-user-id"))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                userService.deleteUserCredential("local-user-id", "credential-id"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_CREDENTIAL_NOT_FOUND));

        verify(keycloakAdminService, never()).deleteUserCredential(
                "keycloak-user-id",
                "credential-id"
        );
    }

    @Test
    void deleteUserCredentialRejectsPasswordCredential() {
        User user = userWithKeycloakId();
        UserCredentialResponse credential = new UserCredentialResponse(
                "password-credential-id",
                "password",
                null,
                null
        );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserCredentials("keycloak-user-id"))
                .thenReturn(List.of(credential));

        assertThatThrownBy(() ->
                userService.deleteUserCredential(
                        "local-user-id",
                        "password-credential-id"
                ))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(
                                        ErrorCode.USER_CREDENTIAL_DELETE_NOT_ALLOWED
                                ));

        verify(keycloakAdminService, never()).deleteUserCredential(
                "keycloak-user-id",
                "password-credential-id"
        );
    }

    @Test
    void terminateUserSessionDeletesSessionOwnedByTargetUser() {
        User user = userWithKeycloakId();
        UserSessionResponse session = sessionResponse("session-id");

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserSessions("keycloak-user-id"))
                .thenReturn(List.of(session));

        userService.terminateUserSession("local-user-id", "session-id");

        verify(keycloakAdminService).deleteUserSession("session-id");
    }

    @Test
    void terminateUserSessionRejectsSessionOwnedByAnotherUser() {
        User user = userWithKeycloakId();

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));
        when(keycloakAdminService.getUserSessions("keycloak-user-id"))
                .thenReturn(List.of(sessionResponse("another-session-id")));

        assertThatThrownBy(() ->
                userService.terminateUserSession("local-user-id", "session-id"))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode())
                                .isEqualTo(ErrorCode.USER_SESSION_NOT_FOUND));

        verify(keycloakAdminService, never()).deleteUserSession("session-id");
    }

    @Test
    void sendRequiredActionsEmailUsesKeycloakIdAndDefaultLifespan() {
        User user = userWithKeycloakId();
        SendRequiredActionsEmailRequest request =
                new SendRequiredActionsEmailRequest(
                        List.of(KeycloakRequiredAction.UPDATE_PASSWORD),
                        null
                );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));

        userService.sendRequiredActionsEmail("local-user-id", request);

        verify(keycloakAdminService).sendRequiredActionsEmail(
                "keycloak-user-id",
                List.of(KeycloakRequiredAction.UPDATE_PASSWORD),
                900
        );
    }

    @Test
    void updateRequiredActionsUsesKeycloakIdFromLocalUser() {
        User user = userWithKeycloakId();
        UpdateUserRequiredActionsRequest request =
                new UpdateUserRequiredActionsRequest(
                        List.of(
                                KeycloakRequiredAction.UPDATE_PASSWORD,
                                KeycloakRequiredAction.CONFIGURE_TOTP
                        )
                );

        when(userRepository.findById("local-user-id")).thenReturn(Optional.of(user));

        userService.updateRequiredActions("local-user-id", request);

        verify(keycloakAdminService).updateRequiredActions(
                "keycloak-user-id",
                List.of(
                        KeycloakRequiredAction.UPDATE_PASSWORD,
                        KeycloakRequiredAction.CONFIGURE_TOTP
                )
        );
    }

    private User userWithKeycloakId() {
        return User.builder()
                .id("local-user-id")
                .keycloakId("keycloak-user-id")
                .build();
    }

    private UserSessionResponse sessionResponse(String sessionId) {
        return new UserSessionResponse(
                sessionId,
                "127.0.0.1",
                Instant.ofEpochSecond(1_000),
                Instant.ofEpochSecond(2_000),
                false,
                List.of("pc-verse-client")
        );
    }
}
