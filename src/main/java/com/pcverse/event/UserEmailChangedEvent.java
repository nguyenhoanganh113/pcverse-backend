package com.pcverse.event;

public record UserEmailChangedEvent(
        String keycloakUserId,
        String username
) {
}
