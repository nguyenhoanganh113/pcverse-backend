package com.pcverse.dto.response;

import lombok.Builder;

import java.time.Instant;

@Builder
public record AdminAttributeDefinitionResponse(

        String id,

        String code,

        String name,

        boolean active,

        Long version,

        Instant createdAt,

        Instant updatedAt

) {
}
