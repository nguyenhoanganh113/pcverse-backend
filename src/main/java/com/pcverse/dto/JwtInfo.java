package com.pcverse.dto;

import lombok.Builder;

import java.util.Date;

@Builder
public record JwtInfo(

        String tokenId,
        Date expirationDate

) {
}
