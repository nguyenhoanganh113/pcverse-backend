package com.pcverse.dto.request;

import com.pcverse.validation.annotation.PasswordMatches;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

@PasswordMatches
public record ResetUserPasswordRequest(

        @NotBlank(message = "New password is required")
        @Length(min = 8, message = "Password must be at least 8 characters long")
        String newPassword,

        @NotBlank(message = "Confirm new password is required")
        String confirmNewPassword,

        Boolean temporary
) {

    public ResetUserPasswordRequest {
        temporary = Boolean.TRUE.equals(temporary);
    }

    public boolean isTemporary() {
        return temporary;
    }
}
