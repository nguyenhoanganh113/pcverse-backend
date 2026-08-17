package com.pcverse.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pcverse.enums.Gender;
import com.pcverse.enums.UserStatus;
import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Builder
@JsonInclude(NON_NULL)
public record AdminUserResponse(

        String id,

        String username,

        String email,

        String lastName,

        String firstName,

        String phoneNumber,

        String urlAvatar,

        Gender gender,

        LocalDate dateOfBirth,

        UserStatus userStatus,

        List<String> roles,

        Instant deletedAt

) {
}
