package com.pcverse.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(400, "Invalid request data", HttpStatus.BAD_REQUEST),
    USER_ALREADY_EXISTS(409, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(400,"User not found", HttpStatus.NOT_FOUND),
    TOKEN_INVALID(401, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "Token is expired", HttpStatus.UNAUTHORIZED),
    TOKEN_GENERATION_FAILED(500, "Token generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    UNAUTHORIZED(401, "Unauthorized", HttpStatus.UNAUTHORIZED);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
