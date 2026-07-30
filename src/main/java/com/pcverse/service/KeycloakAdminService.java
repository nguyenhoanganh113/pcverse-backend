package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.enums.KeycloakRequiredAction;

import java.util.List;

public interface KeycloakAdminService {

    String createUser(CreateUserRequest request);

    void updateUserEnabledStatus(
            String keycloakUserId,
            boolean enabled
    );

    void updateUser(String keycloakUserId, UpdateAdminUserRequest request);

    boolean deleteUser(String keycloakUserId);

    void resetPassword(String keycloakUserId, String newPassword, boolean temporary);

    void assignRealmRole(String keycloakUserId, String roleName);

    void removeRealmRole(String keycloakUserId, String roleName);

    void logoutUser(String keycloakUserId);

    List<UserSessionResponse> getUserSessions(String keycloakUserId);

    void deleteUserSession(String sessionId);

    void sendRequiredActionsEmail(
            String keycloakUserId,
            List<KeycloakRequiredAction> actions,
            int lifespanSeconds
    );

    void updateRequiredActions(
            String keycloakUserId,
            List<KeycloakRequiredAction> actions
    );
}
