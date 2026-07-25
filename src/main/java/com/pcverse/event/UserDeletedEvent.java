package com.pcverse.event;

public record UserDeletedEvent(
        String email,
        String username
) {
}
