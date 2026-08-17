package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateAttributeDefinitionRequest(

        @NotBlank(message = "Code không được để trống")
        @Size(max = 100, message = "Code không được vượt quá 100 ký tự")
        @Pattern(
                regexp = "^[a-z][a-z0-9_]*$",
                message = "Code phải viết thường, bắt đầu bằng chữ và chỉ chứa chữ, số hoặc dấu gạch dưới"
        )
        String code,

        @NotBlank(message = "Tên thuộc tính không được để trống")
        @Size(max = 150, message = "Tên thuộc tính không được vượt quá 150 ký tự")
        String name

) {
        public CreateAttributeDefinitionRequest {
                code = stripToNull(code);
                name = stripToNull(name);
        }

        private static String stripToNull(String value) {
                return (value == null || value.isBlank()) ? null : value.strip();
        }
}
