package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.constraints.NotBlank;
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

        String phoneNumber,

        @NotBlank(message = "Gender is required")
        @EnumPattern(fieldName = "gender", enumClass = Gender.class)
        String gender

) {

        public CreateUserRequest {
                email = trim(email);
                firstName = trim(firstName);
                lastName = trim(lastName);
                phoneNumber = trim(phoneNumber);
                gender = trim(gender);
        }

        private static String trim(String value) {
                return value == null ? null : value.trim();
        }

}
