package com.pcverse.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record AssignUserRoleRequest(

        @NotBlank(message = "Role name is required")
        String roleName
) {

    public AssignUserRoleRequest {
        roleName = roleName == null ? null : roleName.strip().toUpperCase(Locale.ROOT);
    }
}
