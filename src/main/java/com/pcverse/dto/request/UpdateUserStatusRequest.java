package com.pcverse.dto.request;

import com.pcverse.enums.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(

        @NotNull(message = "User status must not be null")
        UserStatus status
) {
}
