package com.pcverse.service;

public interface KeycloakEmailService {

    void sendVerifyEmailAsync(String keycloakUserId, String username);

}
