package com.pcverse.service;

import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);

    LoginResponse refreshToken(String refreshToken);

}
