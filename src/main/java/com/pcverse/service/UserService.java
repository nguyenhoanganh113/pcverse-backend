package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.request.CreateAdminUserRequest;
import com.pcverse.dto.request.ResetUserPasswordRequest;
import com.pcverse.dto.request.UpdateAdminUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;
import com.pcverse.entity.User;
import com.pcverse.enums.UserStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailsResponse createAdminUser(CreateAdminUserRequest request);

    UserDetailsResponse myInfo(Jwt jwt);

    List<UserDetailsResponse> getAllUsers();

    UserDetailsResponse updateUserStatus(String userId, UserStatus status);

    UserDetailsResponse updateUser(String userId, UpdateAdminUserRequest request);

    void deleteUser(String userId);

    void resetPassword(String userId, ResetUserPasswordRequest request);

    UserDetailsResponse assignRole(String userId, String roleName);

    User ensureUserExistsFromToken(Jwt jwt);

}
