package com.pcverse.service;

public interface KeycloakEmailService {

    void sendVerifyEmail(String keycloakUserId, String username);

    void sendVerifyEmailAsync(String keycloakUserId, String username);

}
