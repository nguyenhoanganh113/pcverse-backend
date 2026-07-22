package com.pcverse.controller;

import com.pcverse.dto.request.AssignUserRoleRequest;
import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.ApiResponse;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @DeleteMapping("/{userId}/roles/{roleName}")
    ApiResponse<UserDetailsResponse> removeRole(@PathVariable String userId,
                                                @PathVariable String roleName) {
        UserDetailsResponse data = userService.removeRole(userId, roleName);

        return ApiResponse.<UserDetailsResponse>builder()
                .code(HttpStatus.OK.value())
                .message("Role removed successfully")
                .data(data)
                .build();
    }

    @PostMapping("/{userId}/logout")
    ApiResponse<Void> logoutUser(@PathVariable String userId) {
        userService.logoutUser(userId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User logged out successfully")
                .build();
    }

    @GetMapping("/{userId}/sessions")
    ApiResponse<List<UserSessionResponse>> getUserSessions(@PathVariable String userId) {
        List<UserSessionResponse> data = userService.getUserSessions(userId);

        return ApiResponse.<List<UserSessionResponse>>builder()
                .code(HttpStatus.OK.value())
                .message("User sessions retrieved successfully")
                .data(data)
                .build();
    }

    @DeleteMapping("/{userId}/sessions/{sessionId}")
    ApiResponse<Void> terminateUserSession(
            @PathVariable String userId,
            @PathVariable String sessionId
    ) {
        userService.terminateUserSession(userId, sessionId);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("User session terminated successfully")
                .build();
    }

    @PostMapping("/{userId}/required-actions-email")
    ApiResponse<Void> sendRequiredActionsEmail(
            @PathVariable String userId,
            @RequestBody @Valid SendRequiredActionsEmailRequest request
    ) {
        userService.sendRequiredActionsEmail(userId, request);

        return ApiResponse.<Void>builder()
                .code(HttpStatus.OK.value())
                .message("Required-actions email sent successfully")
                .build();
    }

}
