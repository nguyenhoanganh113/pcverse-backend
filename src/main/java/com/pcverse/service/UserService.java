package com.pcverse.service;

import com.pcverse.dto.request.CreateUserRequest;
import com.pcverse.dto.response.CreateUserResponse;

public interface UserService {

    CreateUserResponse createUser(CreateUserRequest request);

}
