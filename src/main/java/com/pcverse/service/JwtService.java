package com.pcverse.service;

import com.nimbusds.jose.JOSEException;
import com.pcverse.dto.response.TokenPayloadResponse;

import java.text.ParseException;
import java.util.Set;

public interface JwtService {

    String generateAccessToken(String userId, Set<String> roles);

    String generateRefreshToken(String userId);

    TokenPayloadResponse verifyRefreshToken(String refreshToken) throws ParseException, JOSEException;

}
