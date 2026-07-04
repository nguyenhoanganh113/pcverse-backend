package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record ApiResponse<T>(

    int code,
    String message,
    T data

){
}
