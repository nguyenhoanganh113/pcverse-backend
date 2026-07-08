package com.pcverse.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.pcverse.enums.TokenType;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.service.JwtService;
import org.springframework.beans.factory.annotation.Value;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static com.pcverse.constant.AppConstant.*;

public class JwtServiceImpl implements JwtService {

    @Value("${jwt.secret-key}")
    private String secretKey;

    @Override
    public String generateAccessToken(String userId, Set<String> roles) {

        // Header
        JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

        // Payload
        Instant issuedAt = Instant.now(); // Thời điểm token được phát hành
        Instant expiredAt = issuedAt.plus(15, ChronoUnit.MINUTES);
        String jwtId = UUID.randomUUID().toString();

        // Tạo payload cho JWT: token thuộc user nào, phát hành lúc nào,
        // hết hạn khi nào, và mang các quyền/role nào.
        JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer(JWT_ISSUER)
                .issueTime(Date.from(issuedAt))
                .expirationTime(Date.from(expiredAt))
                .claim(ROLES, roles)
                .claim(TOKEN_TYPE, TokenType.ACCESS_TOKEN)
                .jwtID(jwtId)
                .build();

        Payload payload = new Payload(claimsSet.toJSONObject());

        // Signature
        JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new UserServiceException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
        return jwsObject.serialize();

    }

    @Override
    public String generateRefreshToken(String userId) {

         // Header
         JWSHeader header = new JWSHeader(JWSAlgorithm.HS512);

         // Payload
         Instant issuedAt = Instant.now();
         Instant expiredAt = issuedAt.plus(7, ChronoUnit.DAYS);
         String jwtId = UUID.randomUUID().toString();

         JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                 .subject(userId)
                 .issuer(JWT_ISSUER)
                 .issueTime(Date.from(issuedAt))
                 .expirationTime(Date.from(expiredAt))
                 .claim(TOKEN_TYPE, TokenType.REFRESH_TOKEN)
                 .jwtID(jwtId)
                 .build();

         Payload payload = new Payload(claimsSet.toJSONObject());

         // Signature
         JWSObject jwsObject = new JWSObject(header, payload);
        try {
            jwsObject.sign(new MACSigner(secretKey));
        } catch (JOSEException e) {
            throw new UserServiceException(ErrorCode.TOKEN_GENERATION_FAILED);
        }
        return jwsObject.serialize();

    }
}
