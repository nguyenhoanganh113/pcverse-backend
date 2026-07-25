package com.pcverse.event.listener;

import com.pcverse.event.UserDeletedEvent;
import com.pcverse.service.UserDeletionEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserDeletedEventListener {

    private final UserDeletionEmailService userDeletionEmailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleUserDeleted(UserDeletedEvent event) {
        userDeletionEmailService.sendDeletionNoticeAsync(
                event.email(),
                event.username()
        );
    }
}
