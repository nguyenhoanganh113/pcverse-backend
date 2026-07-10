package com.pcverse.configuration;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Getter
@Component
public class JwtKeyProvider {

    private final RSAPrivateKey accessTokenPrivateKey;
    private final RSAPublicKey accessTokenPublicKey;
    private final SecretKey refreshTokenSecretKey;

    public JwtKeyProvider(
            @Value("${jwt.access-token.private-key}") String accessTokenPrivateKey,
            @Value("${jwt.access-token.public-key}") String accessTokenPublicKey,
            @Value("${jwt.refresh-token.secret-key}") String refreshTokenSecretKey
    ) {
        this.accessTokenPrivateKey = parsePrivateKey(accessTokenPrivateKey);
        this.accessTokenPublicKey = parsePublicKey(accessTokenPublicKey);
        this.refreshTokenSecretKey = parseRefreshSecretKey(refreshTokenSecretKey);
    }

    private RSAPrivateKey parsePrivateKey(String privateKeyBase64) {
        try {
            String privateKeyPem = new String(Base64.getDecoder().decode(privateKeyBase64), StandardCharsets.UTF_8);
            String privateKeyContent = privateKeyPem
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(privateKeyContent);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(keySpec);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid access token private key", e);
        }
    }

    private RSAPublicKey parsePublicKey(String publicKeyBase64) {
        try {
            String publicKeyPem = new String(Base64.getDecoder().decode(publicKeyBase64), StandardCharsets.UTF_8);
            String publicKeyContent = publicKeyPem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");

            byte[] keyBytes = Base64.getDecoder().decode(publicKeyContent);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);

            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(keySpec);
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("Invalid access token public key", e);
        }
    }

    private SecretKey parseRefreshSecretKey(String refreshSecretKeyBase64) {
        byte[] keyBytes = Base64.getDecoder().decode(refreshSecretKeyBase64);
        return new SecretKeySpec(keyBytes, "HmacSHA512");
    }
}
