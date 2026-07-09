package com.pcverse.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.pcverse.enums.TokenType;
import com.pcverse.exception.ErrorCode;
import com.pcverse.exception.UserServiceException;
import com.pcverse.service.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

import static com.pcverse.constant.AppConstant.*;

@Service
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

    @Override
    public SignedJWT validateToken(String token) throws ParseException, JOSEException {

        /*
         * chuyển JWT string thành SignedJWT object
         * kiểm tra format JWT
         * sai format thì throw ParseException
         * chưa verify chữ ký
         * chưa kiểm tra hết hạn
        */
        SignedJWT signedJWT = SignedJWT.parse(token);

        /*
        Lấy thời gian hết hạn từ claim exp
        So sánh với thời gian hiện tại
        Nếu token đã hết hạn → throw exception
         */
        Date expirationTime = signedJWT.getJWTClaimsSet().getExpirationTime();
        if (expirationTime.before(Date.from(Instant.now())))
            throw new UserServiceException(ErrorCode.TOKEN_EXPIRED);

        /*
        Verify signature bằng secret key
        MACVerifier: Verifier cho thuật toán HMAC (HS512)
        Nếu signature không khớp → token bị giả mạo → throw exception
         */
        boolean verify = signedJWT.verify(new MACVerifier(secretKey));
        if(!verify)
            throw new UserServiceException(ErrorCode.TOKEN_INVALID);

        /*
        Nếu validate thành công, trả về SignedJWT object
        Caller có thể extract claims từ object này
         */
        return signedJWT;

    }
}
