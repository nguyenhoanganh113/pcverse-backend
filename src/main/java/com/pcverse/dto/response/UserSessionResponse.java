package com.pcverse.dto.response;

import java.time.Instant;
import java.util.List;

public record UserSessionResponse(

        String sessionId,

        String ipAddress,

        Instant startedAt,

        Instant lastAccessAt,

        boolean rememberMe,

        List<String> clients

) {
}
