package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAddressRequest(

        @NotBlank(message = "Recipient name is required")
        @Size(max = 255, message = "Recipient name must contain at most 255 characters")
        String recipientName,

        @NotBlank(message = "Recipient phone is required")
        @Size(max = 30, message = "Recipient phone must contain at most 30 characters")
        String recipientPhone,

        @NotBlank(message = "Province is required")
        @Size(max = 255, message = "Province must contain at most 255 characters")
        String province,

        @NotBlank(message = "District is required")
        @Size(max = 255, message = "District must contain at most 255 characters")
        String district,

        @NotBlank(message = "Ward is required")
        @Size(max = 255, message = "Ward must contain at most 255 characters")
        String ward,

        @Size(max = 255, message = "Street detail must contain at most 255 characters")
        String streetDetail,

        Boolean isDefault
) {

    public CreateAddressRequest {
        recipientName = trim(recipientName);
        recipientPhone = trim(recipientPhone);
        province = trim(province);
        district = trim(district);
        ward = trim(ward);
        streetDetail = trimToNull(streetDetail);
    }

    private static String trim(String value) {
        return value == null ? null : value.strip();
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
