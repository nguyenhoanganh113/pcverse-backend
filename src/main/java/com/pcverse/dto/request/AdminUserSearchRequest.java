package com.pcverse.dto.request;

import com.pcverse.enums.Gender;
import com.pcverse.enums.UserSearchMode;
import com.pcverse.enums.UserStatus;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public record AdminUserSearchRequest(
        UserSearchMode mode,
        @Size(max = 255) String search,
        @Size(max = 255) String username,
        @Size(max = 255) String email,
        @Size(max = 255) String firstName,
        @Size(max = 255) String lastName,
        @Size(max = 255) String phoneNumber,
        Gender gender,
        UserStatus userStatus,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfBirthFrom,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateOfBirthTo,
        Boolean exact
) {

    public AdminUserSearchRequest {
        // Không truyền mode thì mặc định tìm kiếm theo username.
        mode = mode == null ? UserSearchMode.DEFAULT : mode;
        // Constructor binding truyền null khi query parameter exact bị bỏ trống.
        exact = exact != null && exact;
    }

    @AssertTrue(message = "dateOfBirthFrom must be before or equal to dateOfBirthTo")
    public boolean isDateOfBirthRangeValid() {
        return dateOfBirthFrom == null
                || dateOfBirthTo == null
                || !dateOfBirthFrom.isAfter(dateOfBirthTo);
    }
}
