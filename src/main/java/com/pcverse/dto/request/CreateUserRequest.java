package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record CreateUserRequest(

        @NotBlank(message = "Username is required")
        String username,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be valid")
        String email,

        @NotBlank(message = "First name is required")
        String firstName,

        @NotBlank(message = "Last name is required")
        String lastName,

        @NotBlank(message = "Phone number is required")
        String phoneNumber,

        @NotBlank(message = "Gender is required")
        @EnumPattern(fieldName = "gender", enumClass = Gender.class)
        String gender,

        @NotNull(message = "Date of birth is required")
        LocalDate dateOfBirth,

        String urlAvatar

) {

        public CreateUserRequest {
                username = trim(username);
                email = trim(email);
                firstName = trim(firstName);
                lastName = trim(lastName);
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
