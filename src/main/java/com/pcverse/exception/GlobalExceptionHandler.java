package com.pcverse.exception;

import com.pcverse.dto.response.ErrorResponse;
import com.pcverse.dto.response.FieldErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.util.Date;
import java.util.List;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(UserServiceException.class)
    public ResponseEntity<ErrorResponse> handleUserServiceException(UserServiceException ex, WebRequest request) {
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
