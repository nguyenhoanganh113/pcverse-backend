package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;

public interface KeycloakAdminService {

    String createUser(CreateUserRequest request);

    void setUserEnabled(String keycloakUserId, boolean enabled);

    void updateUser(String keycloakUserId, UpdateAdminUserRequest request);

    void deleteUser(String keycloakUserId);

    void resetPassword(String keycloakUserId, String newPassword, boolean temporary);

    void assignClientRole(String keycloakUserId, String roleName);

    void removeClientRole(String keycloakUserId, String roleName);
}
