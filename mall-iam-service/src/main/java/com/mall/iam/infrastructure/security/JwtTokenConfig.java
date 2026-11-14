package com.mall.iam.infrastructure.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Configuration
public class JwtTokenConfig {
    @Bean
    public JwtEncoder jwtEncoder(@Value("${iam.jwt.hmac-secret-base64}") String base64Secret) {
        byte[] key;
        try { key = Base64.getDecoder().decode(base64Secret); }
        catch (IllegalArgumentException e) { throw new IllegalStateException("JWT secret must be Base64", e); }
        if (key.length < 32) throw new IllegalStateException("JWT secret requires at least 32 random bytes");
        return new NimbusJwtEncoder(new ImmutableSecret<>(new SecretKeySpec(key, "HmacSHA256")));
    }
}
