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
    USER_NOT_FOUND(404,"User not found", HttpStatus.NOT_FOUND),
    PROFILE_INCOMPLETE(409, "User profile has not been completed", HttpStatus.CONFLICT),
    PROFILE_ALREADY_COMPLETED(409, "User profile has already been completed", HttpStatus.CONFLICT),
    TOKEN_INVALID(401, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "Token is expired", HttpStatus.UNAUTHORIZED),
        TOKEN_REVOKED(401, "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_GENERATION_FAILED(500, "Token generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    KEYCLOAK_ADMIN_API_ERROR(502, "Unable to complete the user operation in Keycloak", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_USER_NOT_FOUND(404, "User does not exist in Keycloak", HttpStatus.NOT_FOUND),
    KEYCLOAK_RESOURCE_CLIENT_NOT_FOUND(
            502,
            "Configured resource client does not exist in Keycloak",
            HttpStatus.BAD_GATEWAY
    ),
    KEYCLOAK_PERMISSION_DENIED(502, "The Keycloak service account does not have permission to perform this operation", HttpStatus.BAD_GATEWAY),
    PASSWORD_POLICY_VIOLATION(
            400,
            "Password does not meet the configured password policy",
            HttpStatus.BAD_REQUEST
    ),
    KEYCLOAK_USER_NOT_LINKED(409, "User is not linked to a Keycloak account", HttpStatus.CONFLICT),
    USER_STATUS_NOT_SUPPORTED(400, "This user status cannot be synchronized with Keycloak", HttpStatus.BAD_REQUEST),
    USER_ACCOUNT_INACTIVE(403, "User account is not active", HttpStatus.FORBIDDEN),
    UNAUTHORIZED(401, "Vui lòng đăng nhập để truy cập", HttpStatus.UNAUTHORIZED),
    FORBIDDEN(403, "Không có quyền truy cập", HttpStatus.FORBIDDEN),
    MISSING_LOGOUT_INFO(400, "Authorization header or refresh token is missing", HttpStatus.BAD_REQUEST),
    USER_ROLE_NOT_ASSIGNED(404,"Role is not assigned to this user", HttpStatus.NOT_FOUND),
    USER_SESSION_NOT_FOUND(404, "User session not found", HttpStatus.NOT_FOUND),
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),;

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
