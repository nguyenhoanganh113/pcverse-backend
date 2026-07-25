package com.pcverse.exception;

import org.springframework.security.oauth2.jwt.BadJwtException;

public class TokenRevokedException extends BadJwtException {

    public TokenRevokedException() {
        super(ErrorCode.TOKEN_REVOKED.getMessage());
    }
}
