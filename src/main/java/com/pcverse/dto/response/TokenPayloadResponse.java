package com.pcverse.dto.response;

import java.util.Set;

public record TokenPayloadResponse(

        Boolean isValid,
        String id,
        Set<String> authorities

) {
}
