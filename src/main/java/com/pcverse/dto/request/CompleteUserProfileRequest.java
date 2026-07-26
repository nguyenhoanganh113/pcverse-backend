package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;

import java.time.LocalDate;

public record CompleteUserProfileRequest(

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Gender is required")
        @EnumPattern(fieldName = "gender", enumClass = Gender.class)
        String gender,

        @NotNull(message = "Date of birth is required")
        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        String urlAvatar

) {

    public CompleteUserProfileRequest {
        phoneNumber = trim(phoneNumber);
        gender = trim(gender);
        urlAvatar = trimToNull(urlAvatar);
    }

    private static String trim(String value) {
        return value == null ? null : value.trim();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
