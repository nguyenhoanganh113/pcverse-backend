package com.pcverse.service.impl;

import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.User;
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
