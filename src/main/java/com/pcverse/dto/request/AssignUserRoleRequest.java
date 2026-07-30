package com.pcverse.dto.request;

import com.pcverse.enums.RealmRole;
import com.pcverse.validation.annotation.EnumPattern;
import jakarta.validation.constraints.NotBlank;

import java.util.Locale;

public record AssignUserRoleRequest(

        @NotBlank(message = "Role name is required")
        @EnumPattern(
                enumClass = RealmRole.class,
                fieldName = "roleName"
        )
        String roleName
) {

    public AssignUserRoleRequest {
        roleName = roleName == null ? null : roleName.strip().toUpperCase(Locale.ROOT);
    }
}
