package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateBrandRequest(

        @Size(max = 120, message = "Name must not exceed 120 characters")
        @NotBlank(message = "Name is required")
        String name,

        @Size(max = 2048, message = "Logo URL must not exceed 2048 characters")
        String logoUrl

) {
        public CreateBrandRequest{
                name = stripToNull(name);
                logoUrl = stripToNull(logoUrl);
        }

        private static String stripToNull(String value) {
                return value == null || value.isBlank() ? null : value.strip();
        }

}
