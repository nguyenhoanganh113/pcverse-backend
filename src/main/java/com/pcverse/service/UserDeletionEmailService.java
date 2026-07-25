package com.pcverse.service;

public interface UserDeletionEmailService {

    void sendDeletionNoticeAsync(
            String email,
            String username
    );
}
