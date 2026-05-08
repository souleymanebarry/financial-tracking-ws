package com.barry.bank.financial.tracking_ws.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

@Configuration
public class JwtEncoderAndDecodeConfig {

    private static final String HMACSHA256 = "HmacSHA256";
    @Value("${security.jwt.secret}")
    private String secret;

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(getKey()));
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withSecretKey(getKey())
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private SecretKey getKey() {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMACSHA256);
    }
}
