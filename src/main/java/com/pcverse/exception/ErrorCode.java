package com.pcverse.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@AllArgsConstructor
@Getter
public enum ErrorCode {

    INTERNAL_ERROR(500, "Unexpected error occurred while processing request in backend service", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR(400, "Invalid request data", HttpStatus.BAD_REQUEST),
    UNSUPPORTED_MEDIA_TYPE(415, "Unsupported request Content-Type", HttpStatus.UNSUPPORTED_MEDIA_TYPE),
    USER_ALREADY_EXISTS(409, "User already exists", HttpStatus.CONFLICT),
    USER_NOT_FOUND(404,"User not found", HttpStatus.NOT_FOUND),
    USER_SEARCH_FIELD_REQUIRED(400, "User search field is required", HttpStatus.BAD_REQUEST),
    USER_SEARCH_VALUE_REQUIRED(400, "User search value is required", HttpStatus.BAD_REQUEST),
    TOKEN_INVALID(401, "Invalid token", HttpStatus.UNAUTHORIZED),
    TOKEN_EXPIRED(401, "Token is expired", HttpStatus.UNAUTHORIZED),
        TOKEN_REVOKED(401, "Token has been revoked", HttpStatus.UNAUTHORIZED),
    TOKEN_GENERATION_FAILED(500, "Token generation failed", HttpStatus.INTERNAL_SERVER_ERROR),
    KEYCLOAK_ADMIN_API_ERROR(502, "Unable to complete the user operation in Keycloak", HttpStatus.BAD_GATEWAY),
    KEYCLOAK_USER_NOT_FOUND(404, "User does not exist in Keycloak", HttpStatus.NOT_FOUND),
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
    ROLE_NOT_FOUND(404, "Role not found", HttpStatus.NOT_FOUND),
    ADDRESS_NOT_FOUND(404, "Address not found", HttpStatus.NOT_FOUND),
    CATEGORY_EXISTED(409, "Category already existed", HttpStatus.CONFLICT),
    CATEGORY_CONCURRENT_MODIFICATION(
            409,
            "Category was modified by another request. Please retry",
            HttpStatus.CONFLICT
    ),
    CATEGORY_NAME_REQUIRED(400, "Category name must not be null or blank", HttpStatus.BAD_REQUEST),
    CATEGORY_NAME_INVALID(400, "Category name must produce a non-empty slug", HttpStatus.BAD_REQUEST),
    CATEGORY_NOT_FOUND(404, "Category not found", HttpStatus.NOT_FOUND),
    CATEGORY_SEARCH_FIELD_REQUIRED(400, "Category search field is required", HttpStatus.BAD_REQUEST),
    CATEGORY_SEARCH_VALUE_REQUIRED(400, "Category search value is required", HttpStatus.BAD_REQUEST);

    private final int code;
    private final String message;
    private final HttpStatus httpStatus;


}
