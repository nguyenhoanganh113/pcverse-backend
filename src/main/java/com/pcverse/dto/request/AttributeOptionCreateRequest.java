package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Locale;

public record AttributeOptionCreateRequest(

        @NotBlank(message = "Option code must not be blank")
        @Size(max = 100, message = "Option code must not exceed 100 characters")
        String code,

        @NotBlank(message = "Option label must not be blank")
        @Size(max = 150, message = "Option label must not exceed 150 characters")
        String label,

        @NotNull(message = "Display order must not be null")
        @PositiveOrZero(message = "Display order must be greater than or equal to 0")
        Integer displayOrder

) {

        public AttributeOptionCreateRequest {
            if (code != null) {
                code = code.strip().toLowerCase(Locale.ROOT);
            }

            if (label != null) {
                label = label.strip();
            }
        }

}
