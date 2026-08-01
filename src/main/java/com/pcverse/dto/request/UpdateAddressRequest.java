package com.pcverse.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateAddressRequest(

        @Pattern(regexp = ".*\\S.*", message = "Recipient name must not be blank")
        @Size(max = 255, message = "Recipient name must contain at most 255 characters")
        String recipientName,

        @Pattern(regexp = ".*\\S.*", message = "Recipient phone must not be blank")
        @Size(max = 30, message = "Recipient phone must contain at most 30 characters")
        String recipientPhone,

        @Pattern(regexp = ".*\\S.*", message = "Province must not be blank")
        @Size(max = 255, message = "Province must contain at most 255 characters")
        String province,

        @Pattern(regexp = ".*\\S.*", message = "District must not be blank")
        @Size(max = 255, message = "District must contain at most 255 characters")
        String district,

        @Pattern(regexp = ".*\\S.*", message = "Ward must not be blank")
        @Size(max = 255, message = "Ward must contain at most 255 characters")
        String ward,

        @Size(max = 255, message = "Street detail must contain at most 255 characters")
        String streetDetail,

        Boolean isDefault
) {

    public UpdateAddressRequest {
        recipientName = trim(recipientName);
        recipientPhone = trim(recipientPhone);
        province = trim(province);
        district = trim(district);
        ward = trim(ward);
        streetDetail = trim(streetDetail);
    }

    @JsonIgnore
    @AssertTrue(message = "At least one address field must be provided")
    public boolean isAnyFieldPresent() {
        return recipientName != null
                || recipientPhone != null
                || province != null
                || district != null
                || ward != null
                || streetDetail != null
                || isDefault != null;
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }
}
