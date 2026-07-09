package com.pcverse.service;

import com.pcverse.dto.request.LoginRequest;
import com.pcverse.dto.response.ExchangeTokenResponse;
import com.pcverse.dto.response.LoginResponse;

public interface AuthenticationService {

    LoginResponse login(LoginRequest request);

    ExchangeTokenResponse refreshToken(String refreshToken);

    void logout(String refreshToken);

}
