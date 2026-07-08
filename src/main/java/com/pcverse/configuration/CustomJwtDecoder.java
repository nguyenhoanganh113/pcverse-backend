package com.pcverse.configuration;

import com.pcverse.repository.TokenRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Component
@Slf4j
@RequiredArgsConstructor
public class CustomJwtDecoder implements JwtDecoder {

    @Value("${jwt.secret-key}")
    private String secretKey;

    private NimbusJwtDecoder nimbusJwtDecoder;

    private final TokenRepository tokenRepository;

    @PostConstruct
    public void init() {
        //Chuyển secret key từ String sang SecretKey object mà Java Crypto API hiểu được
        SecretKey secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
        nimbusJwtDecoder = NimbusJwtDecoder
                .withSecretKey(secretKeySpec) //Tạo một NimbusJwtDecoder với cùng secretKey
                //và cùng thuật toán HMAC mà bạn đã dùng khi ký token lúc login.
                .macAlgorithm(MacAlgorithm.HS512) //Chỉ định đúng thuật toán
                // — decoder từ chối token nếu dùng thuật toán khác
                .build();
    }

    @Override
    public Jwt decode(@NonNull String token) throws JwtException {

        Jwt jwt = nimbusJwtDecoder.decode(token);

        String jwtId = jwt.getId();
        if (jwtId == null) {
            throw new JwtException("JWT ID is missing");
        }

        if (tokenRepository.existsById(jwtId)) {
            throw new JwtException("Token has been revoked");
        }

        return jwt;
    }
}
