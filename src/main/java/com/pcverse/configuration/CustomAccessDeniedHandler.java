package com.pcverse.configuration;

import com.pcverse.dto.response.ErrorResponse;
import com.pcverse.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    @Override
    public void handle(@NonNull HttpServletRequest request,
                       @NonNull HttpServletResponse response,
                       @NonNull AccessDeniedException accessDeniedException)
            throws IOException, ServletException {

        // 1. Lấy error code FORBIDDEN (403)
        ErrorCode errorCode = ErrorCode.FORBIDDEN;

        // 2. Set HTTP status code
        response.setStatus(errorCode.getCode());

        // 3. Set content type là JSON
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        // 4. Tạo ErrorResponse object
        ErrorResponse errorResponse = ErrorResponse.builder()
                .timestamp(System.currentTimeMillis()) // LocalDateTime.now().toString()
                .status(errorCode.getCode())
                .errorCode(errorCode.name())
                .error(errorCode.getHttpStatus().getReasonPhrase())
                .message(errorCode.getMessage())
                .path(request.getRequestURI())
                .build();

        // 5. Convert ErrorResponse → JSON và write vào response
        response.getWriter().write(jsonMapper.writeValueAsString(errorResponse));

        // 6. Flush buffer
        response.flushBuffer();
    }
}
