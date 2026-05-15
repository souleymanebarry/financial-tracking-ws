package com.barry.bank.financial.tracking_ws.config;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import com.nimbusds.jose.jwk.RSAKey;

import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.InvalidKeySpecException;


@Configuration
public class JwtEncoderAndDecoderConfig {

    @Value("${rsa.private-key}")
    private String privateKeyPem;

    private final RsaKeyProperties rsaKeyProperties;

    public JwtEncoderAndDecoderConfig(RsaKeyProperties rsaKeyProperties) {
        this.rsaKeyProperties = rsaKeyProperties;
    }

    @Bean
    public JwtEncoder jwtEncoder() throws NoSuchAlgorithmException, InvalidKeySpecException {
        RSAPrivateKey privateKey = KeyUtils.parsePrivateKey(privateKeyPem);

        JWK jwk = new RSAKey.Builder(rsaKeyProperties.publicKey())
                .privateKey(privateKey)
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(jwk));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withPublicKey(rsaKeyProperties.publicKey())
                .build();
    }

}
