package com.pcverse.service;

import com.pcverse.dto.request.CreateAdminUserRequest;

public interface KeycloakAdminService {

    void setUserEnabled(String keycloakUserId, boolean enabled);

    String createAdminUser(CreateAdminUserRequest request);

    void deleteUser(String keycloakUserId);
}
