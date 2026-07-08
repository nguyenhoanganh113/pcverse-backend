package com.pcverse.service;

import java.util.Set;

public interface JwtService {

    String generateAccessToken(String userId, Set<String> roles);

    String generateRefreshToken(String userId);

}
