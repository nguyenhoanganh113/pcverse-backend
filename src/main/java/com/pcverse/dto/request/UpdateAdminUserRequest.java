package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.Locale;

public record UpdateAdminUserRequest(

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotNull(message = "Gender is required")
        Gender gender,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        String urlAvatar
) {

    public UpdateAdminUserRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        username = trim(username);
        firstName = trim(firstName);
        lastName = trim(lastName);
        phoneNumber = trim(phoneNumber);
        urlAvatar = trimToNull(urlAvatar);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
