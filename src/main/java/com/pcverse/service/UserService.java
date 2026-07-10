package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;
import com.pcverse.dto.response.UserDetailsResponse;

import java.util.List;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

    UserDetailsResponse myInfo(String userId);

    List<UserDetailsResponse> getAllUsers();

}
