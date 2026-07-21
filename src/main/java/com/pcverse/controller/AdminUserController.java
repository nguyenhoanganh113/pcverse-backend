package com.pcverse.controller;

import com.pcverse.dto.request.AssignUserRoleRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        CreateUserResponse data = userService.createUser(request);

        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{userId}/roles")
    ApiResponse<UserDetailsResponse> assignRole(
            @PathVariable String userId,
            @RequestBody @Valid AssignUserRoleRequest request
    ) {
        UserDetailsResponse data = userService.assignRole(userId, request.roleName());

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Role assigned successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{userId}")
    ApiResponse<UserDetailsResponse> updateUser(
            @PathVariable String userId,
            @RequestBody @Valid UpdateAdminUserRequest request
    ) {
        UserDetailsResponse data = userService.updateUser(userId, request);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User updated successfully")
                .data(data)
                .build();
    }

}
