package com.pcverse.event.listener;

import com.pcverse.event.UserCreatedEvent;
import com.pcverse.service.KeycloakEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCreatedEventListener {

    private final KeycloakEmailService keycloakEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {
        keycloakEmailService.sendVerifyEmailAsync(
                event.keycloakUserId(),
                event.username()
        );
    }

}
