package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@JsonInclude(NON_NULL)
public record UserCredentialResponse(

        String credentialId,

        String type,

        String userLabel,

        Instant createdAt

) {
}
