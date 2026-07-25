package com.pcverse.service.impl;

import com.pcverse.service.UserDeletionEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserDeletionEmailServiceImpl
        implements UserDeletionEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String sender;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Async
    @Override
    public void sendDeletionNoticeAsync(
            String email,
            String username
    ) {
        if (sender == null || sender.isBlank()
                || mailPassword == null || mailPassword.isBlank()) {
            log.error(
                    "Deletion notice for user {} was not sent because "
                            + "MAIL_USERNAME or MAIL_PASSWORD is not configured",
                    username
            );
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(sender);
            message.setTo(email);
            message.setSubject("Your PC Verse account has been deleted");
            message.setText(buildMessage(username));

            mailSender.send(message);
            log.info("Deletion notice sent to user {}", username);
        } catch (RuntimeException exception) {
            log.error(
                    "Failed to send deletion notice to user {}",
                    username,
                    exception
            );
        }
    }

    private String buildMessage(String username) {
        return """
                Hello %s,

                An administrator has deleted your PC Verse account.
                Your account has been removed from the identity system and
                you can no longer sign in.

                If you believe this action was made in error, please contact
                PC Verse support.

                PC Verse
                """.formatted(username);
    }
}
