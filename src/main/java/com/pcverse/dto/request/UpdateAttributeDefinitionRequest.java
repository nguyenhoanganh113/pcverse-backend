package com.pcverse.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record UpdateAttributeDefinitionRequest(

        @Pattern(
                regexp = ".*\\S.*",
                message = "Tên thuộc tính không được để trống"
        )
        @Size(
                max = 150,
                message = "Tên thuộc tính không được vượt quá 150 ký tự"
        )
        String name,

        @NotNull(message = "Version must not be null")
        @PositiveOrZero(
                message = "Version must be greater than or equal to 0"
        )
        Long version

) {

        public UpdateAttributeDefinitionRequest {
                name = strip(name);
        }

        public boolean hasAnyField() {
                return name != null;
        }

        private static String strip(String value) {
                return value == null ? null : value.strip();
        }
}
