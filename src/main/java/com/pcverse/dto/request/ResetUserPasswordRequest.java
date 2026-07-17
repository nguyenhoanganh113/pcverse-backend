package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record ResetUserPasswordRequest(

        @NotBlank(message = "New password is required")
        @Length(min = 8, message = "Password must be at least 8 characters long")
        String newPassword,

        Boolean temporary
) {

    public boolean isTemporary() {
        return Boolean.TRUE.equals(temporary);
    }
}
