package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public record CreateUserRequest(

        @NotBlank(message = "Email is required")
        String email,

        @NotBlank(message = "Password is required")
        @Length(min = 8, message = "Password must be at least 8 characters long")
        String password,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotNull(message = "Gender is required")
        Gender gender

) {
}
