package com.pcverse.service;

import com.pcverse.dto.request.UpdateAdminUserRequest;

public interface KeycloakAdminService {

    void setUserEnabled(String keycloakUserId, boolean enabled);

    void updateUser(String keycloakUserId, UpdateAdminUserRequest request);

    void deleteUser(String keycloakUserId);

    void resetPassword(String keycloakUserId, String newPassword, boolean temporary);

    void assignClientRole(String keycloakUserId, String roleName);
}
