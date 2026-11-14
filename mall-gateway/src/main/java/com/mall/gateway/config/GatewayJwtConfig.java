package com.mall.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class GatewayJwtConfig {
    @Bean
    public JwtDecoder jwtDecoder(@Value("${gateway.jwt.hmac-secret-base64}") String base64Secret,
                                 @Value("${gateway.jwt.issuer:mall-iam}") String issuer) {
        byte[] raw;
        try { raw = Base64.getDecoder().decode(base64Secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("JWT secret must be Base64", e); }
        if (raw.length < 32) throw new IllegalStateException("JWT secret requires at least 32 random bytes");
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(new SecretKeySpec(raw, "HmacSHA256"))
                .macAlgorithm(MacAlgorithm.HS256).build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(issuer));
        return decoder;
    }
}
