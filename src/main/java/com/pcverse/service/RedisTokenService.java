package com.pcverse.service;

import com.pcverse.entity.RedisToken;

public interface RedisTokenService {

    void saveToken(RedisToken token);

    void deleteTokenByJwtId(String jwtId);

    boolean existsByJwtId(String jwtId);


}
