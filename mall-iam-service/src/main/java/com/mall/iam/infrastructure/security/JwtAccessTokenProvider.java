package com.mall.iam.infrastructure.security;

import com.mall.iam.application.account.port.security.TokenProvider;
import com.mall.iam.application.account.query.GrantedLoginAuthorization;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAccessTokenProvider implements TokenProvider {
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final long ttlSeconds;

    public JwtAccessTokenProvider(JwtEncoder jwtEncoder,
            @Value("${iam.jwt.issuer:mall-iam}") String issuer,
            @Value("${iam.jwt.audience:mall-gateway}") String audience,
            @Value("${iam.jwt.access-ttl-seconds:900}") long ttlSeconds) {
        if (ttlSeconds <= 0 || ttlSeconds > 3600) throw new IllegalArgumentException("invalid access token TTL");
        this.jwtEncoder = jwtEncoder;
        this.issuer = issuer;
        this.audience = audience;
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String issueAccessToken(Long accountId, String sessionId, GrantedLoginAuthorization authorization) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(accountId.toString())
                .audience(List.of(audience))
                .issuedAt(now)
                .expiresAt(now.plusSeconds(ttlSeconds))
                .id(UUID.randomUUID().toString())
                .claim("token_type", "access")
                .claim("sid", sessionId)
                .claim("account_type", 1)
                .claim("roles", authorization.roles())
                .claim("permissions", authorization.permissions())
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }

    @Override
    public long accessTokenSeconds() { return ttlSeconds; }
}
