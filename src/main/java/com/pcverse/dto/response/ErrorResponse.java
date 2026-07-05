package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ErrorResponse(
        long timestamp,
        int status,
        String error,
        int code,
        String message,
        String path,
        List<FieldErrorResponse> details
) {
}
