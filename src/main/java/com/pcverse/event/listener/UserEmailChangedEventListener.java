package com.pcverse.event.listener;

import com.pcverse.event.UserEmailChangedEvent;
import com.pcverse.service.KeycloakEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserEmailChangedEventListener {

    private final KeycloakEmailService keycloakEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserEmailChanged(UserEmailChangedEvent event) {
        keycloakEmailService.sendVerifyEmailAsync(
                event.keycloakUserId(),
                event.username()
        );
    }
}
