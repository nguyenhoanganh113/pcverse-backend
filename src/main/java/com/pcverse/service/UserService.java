package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.SendRequiredActionsEmailRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.request.UpdateUserRequiredActionsRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.PaginationResponse;
import com.pcverse.dto.response.UserCredentialResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.dto.response.UserSessionResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailsResponse myInfo(Jwt jwt);

    PaginationResponse<UserDetailsResponse> getAllUsers(Pageable pageable);

    UserDetailsResponse getUserById(String userId);

    UserDetailsResponse updateUserStatus(String userId, UserStatus status);

    UserDetailsResponse updateUser(String userId, UpdateAdminUserRequest request);

    void deleteUser(String userId);

    void resetPassword(String userId, ResetUserPasswordRequest request);

    UserDetailsResponse assignRole(String userId, String roleName);

    User ensureUserExistsFromToken(Jwt jwt);

    UserDetailsResponse removeRole(String userId, String roleName);

    void logoutUser(String userId);

    List<UserSessionResponse> getUserSessions(String userId);

    List<UserCredentialResponse> getUserCredentials(String userId);

    void deleteUserCredential(String userId, String credentialId);

    void terminateUserSession(String userId, String sessionId);

    void sendRequiredActionsEmail(
            String userId,
            SendRequiredActionsEmailRequest request
    );

    void updateRequiredActions(
            String userId,
            UpdateUserRequiredActionsRequest request
    );

}
