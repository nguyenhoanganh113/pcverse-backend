package com.pcverse.controller;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.CreateAdminUserRequest;
import com.pcverse.dto.request.AssignUserRoleRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateUserStatusRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<CreateUserResponse> createUser(@RequestBody @Valid CreateUserRequest request) {
        var data = userService.createUser(request);

        return ApiResponse.<CreateUserResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("User created successfully")
                .data(data)
                .build();
    }

    @PostMapping("/admin")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<UserDetailsResponse> createAdminUser(@RequestBody @Valid CreateAdminUserRequest request) {
        UserDetailsResponse data = userService.createAdminUser(request);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.CREATED.value())
                .message("Admin user created successfully")
                .data(data)
                .build();
    }

    @GetMapping("/me")
    ApiResponse<UserDetailsResponse> getMyInfo(@AuthenticationPrincipal Jwt jwt) {
        var data = userService.myInfo(jwt);
        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User info retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<UserDetailsResponse>> getAllUsers() {

        List<UserDetailsResponse> users = userService.getAllUsers();

        return ApiResponse.<List<UserDetailsResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("Get all users successfully")
                .data(users)
                .build();

    }

    @PatchMapping("/{userId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<UserDetailsResponse> updateUserStatus(
            @PathVariable String userId,
            @RequestBody @Valid UpdateUserStatusRequest request
    ) {
        UserDetailsResponse data = userService.updateUserStatus(userId, request.status());

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("User status updated successfully")
                .data(data)
                .build();
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
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

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> deleteUser(@PathVariable String userId) {
        userService.deleteUser(userId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User deleted successfully")
                .build();
    }

    @PutMapping("/{userId}/password")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<Void> resetPassword(
            @PathVariable String userId,
            @RequestBody @Valid ResetUserPasswordRequest request
    ) {
        userService.resetPassword(userId, request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Password reset successfully")
                .build();
    }

    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasRole('ADMIN')")
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

}
