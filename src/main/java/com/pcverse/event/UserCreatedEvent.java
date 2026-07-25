package com.pcverse.event;

public record UserCreatedEvent(
        String keycloakUserId,
        String username
) {
}
