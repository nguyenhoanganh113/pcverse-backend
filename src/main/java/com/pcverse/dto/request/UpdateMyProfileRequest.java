package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateMyProfileRequest(

        @Pattern(regexp = ".*\\S.*", message = "First name must not be blank")
        @Size(max = 255, message = "First name must contain at most 255 characters")
        String firstName,

        @Pattern(regexp = ".*\\S.*", message = "Last name must not be blank")
        @Size(max = 255, message = "Last name must contain at most 255 characters")
        String lastName,

        @Size(max = 255, message = "Phone number must contain at most 255 characters")
        String phoneNumber,

        Gender gender,

        @Past(message = "Date of birth must be in the past")
        LocalDate dateOfBirth,

        @Size(max = 255, message = "Avatar URL must contain at most 255 characters")
        String urlAvatar

) {

    public UpdateMyProfileRequest {
        firstName = trim(firstName);
        lastName = trim(lastName);
        phoneNumber = trim(phoneNumber);
        urlAvatar = trim(urlAvatar);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }

}
