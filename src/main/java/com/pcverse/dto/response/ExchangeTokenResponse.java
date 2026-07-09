package com.pcverse.dto.response;

import lombok.Builder;

import java.util.Set;

@Builder
public record ExchangeTokenResponse(

        String accessToken,

        Set<String> roles

) {
}
