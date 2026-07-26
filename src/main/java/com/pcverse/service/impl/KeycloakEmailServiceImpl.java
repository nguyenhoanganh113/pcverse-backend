package com.pcverse.service.impl;

import com.pcverse.configuration.KeycloakAdminProperties;
import com.pcverse.service.KeycloakEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.admin.client.Keycloak;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeycloakEmailServiceImpl implements KeycloakEmailService {

    private final Keycloak keycloak;
    private final KeycloakAdminProperties keycloakAdminProperties;

    @Override
    public void sendVerifyEmail(String keycloakUserId, String username) {
        doSendVerifyEmail(keycloakUserId, username);
    }

    @Async("emailTaskExecutor")
    @Override
    public void sendVerifyEmailAsync(String keycloakUserId, String username) {
        doSendVerifyEmail(keycloakUserId, username);
    }

    private void doSendVerifyEmail(String keycloakUserId, String username) {
        try {
            keycloak.realm(keycloakAdminProperties.realm())
                    .users()
                    .get(keycloakUserId)
                    .sendVerifyEmail();
            log.info("Verify email sent for user {}", username);
        } catch (Exception e) {
            log.error("Failed to send verify email for user {}", username, e);
        }
    }
}
