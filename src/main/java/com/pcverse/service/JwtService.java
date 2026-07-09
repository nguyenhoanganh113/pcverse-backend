package com.pcverse.service;

import com.nimbusds.jose.JOSEException;
import com.pcverse.dto.TokenDetails;
import com.pcverse.dto.response.TokenPayloadResponse;

import java.text.ParseException;
import java.util.Set;

public interface JwtService {

    String generateAccessToken(String userId, Set<String> roles);

    TokenDetails generateRefreshToken(String userId);

    TokenPayloadResponse verifyRefreshToken(String refreshToken) throws ParseException, JOSEException;

}
