package com.pcverse.exception;

import com.pcverse.dto.response.ErrorResponse;
import com.pcverse.dto.response.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.InvalidFormatException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            WebRequest request
    ) {
        ErrorResponse response = buildErrorCodeResponse(ErrorCode.FORBIDDEN, request, null);
        return ResponseEntity.status(ErrorCode.FORBIDDEN.getHttpStatus()).body(response);
    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ErrorResponse> handleAppException(AppException ex, WebRequest request) {
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse response = buildErrorCodeResponse(errorCode, request, null);

        return ResponseEntity.status(errorCode.getHttpStatus()).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAllExceptions(Exception ex, WebRequest request) {
        log.error("Exception occurred: ", ex);
        ErrorResponse response = buildErrorCodeResponse(ErrorCode.INTERNAL_ERROR, request, null);

        return ResponseEntity.status(ErrorCode.INTERNAL_ERROR.getHttpStatus()).body(response);
    }

    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handlerMethodArgumentNotValidException(
            MethodArgumentNotValidException e, WebRequest request) {

        List<FieldErrorResponse> details = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(fieldError -> new FieldErrorResponse(
                        fieldError.getField(),
                        fieldError.getDefaultMessage()
                ))
                .toList();

        ErrorResponse response = buildErrorCodeResponse(ErrorCode.VALIDATION_ERROR, request, details);

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);

    }

    @ExceptionHandler(value = HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(
            HttpMessageNotReadableException e, WebRequest request) {

        FieldErrorResponse detail = buildHttpMessageNotReadableDetail(e);
        ErrorResponse response = buildErrorCodeResponse(ErrorCode.VALIDATION_ERROR, request, List.of(detail));

        return ResponseEntity.status(ErrorCode.VALIDATION_ERROR.getHttpStatus()).body(response);
    }

    private FieldErrorResponse buildHttpMessageNotReadableDetail(HttpMessageNotReadableException e) {
        Throwable cause = e.getCause();

        if (cause instanceof InvalidFormatException invalidFormatException
                && invalidFormatException.getTargetType() != null
                && invalidFormatException.getTargetType().isEnum()) {
            String field = extractFieldName(invalidFormatException);
            String acceptedValues = extractAcceptedEnumValues(invalidFormatException.getTargetType());
            String rejectedValue = Objects.toString(invalidFormatException.getValue(), "null");

            return new FieldErrorResponse(
                    field,
                    "Invalid value '" + rejectedValue + "' for field '" + field
                            + "'. Accepted values: " + acceptedValues
            );
        }

        return new FieldErrorResponse("requestBody", "Invalid request body or JSON format");
    }

    private String extractFieldName(JacksonException exception) {
        String field = exception.getPath()
                .stream()
                .map(JacksonException.Reference::getPropertyName)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("."));

        return field.isBlank() ? "requestBody" : field;
    }

    private String extractAcceptedEnumValues(Class<?> enumClass) {
        return Arrays.stream(enumClass.getEnumConstants())
                .map(enumValue -> ((Enum<?>) enumValue).name())
                .collect(Collectors.joining(", "));
    }

    private ErrorResponse buildErrorCodeResponse(
            ErrorCode errorCode,
            WebRequest request,
            List<FieldErrorResponse> details
    ) {
        return ErrorResponse.builder()
                .timestamp(new Date().getTime())
                .status(errorCode.getHttpStatus().value()) // 400, 401, 403, 404....
                .errorCode(errorCode.name()) // mã lỗi nghiệp vụ mà dự án đặt ra
                .error(errorCode.getHttpStatus().getReasonPhrase()) // Conflict, bad request, ....
                .message(errorCode.getMessage())
                .path(request.getDescription(false).replace("uri=", ""))
                .details(details)
                .build();
    }


}
