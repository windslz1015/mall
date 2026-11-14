package com.mall.gateway.filter.global;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuples;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

/** Central JWT verification and session revocation check. Internal headers are never trusted from clients. */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    private static final String SESSION_PREFIX = "iam:auth:session:";
    private static final Set<String> PUBLIC = Set.of(
            "/api/iam/auth/login", "/api/iam/auth/register", "/api/iam/auth/register/code",
            "/api/iam/auth/refresh", "/api/iam/auth/logout", "/api/iam/auth/ping");
    private final JwtDecoder decoder;
    private final ReactiveStringRedisTemplate redis;
    private final String expectedAudience;

    public AuthGlobalFilter(JwtDecoder decoder, ReactiveStringRedisTemplate redis,
            @Value("${gateway.jwt.audience:mall-gateway}") String expectedAudience) {
        this.decoder = decoder;
        this.redis = redis;
        this.expectedAudience = expectedAudience;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest cleaned = exchange.getRequest().mutate().headers(h -> {
            h.remove("X-Verified-Client-IP");
            h.remove("X-Authenticated-Account-Id");
            h.remove("X-Authenticated-Account-Type");
            h.remove("X-Authenticated-Roles");
            h.remove("X-Authenticated-Permissions");
            h.remove("X-Authenticated-Session-Id");
        }).build();
        ServerWebExchange current = exchange.mutate().request(cleaned).build();
        String path = cleaned.getURI().getPath();
        if (!path.startsWith("/api/") || PUBLIC.contains(path)) return chain.filter(current);

        String authorization = cleaned.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.length() > 8192 || authorization.length() <= 7) {
            return reject(current, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        String token = authorization.substring(7);
        return Mono.fromCallable(() -> decoder.decode(token))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(jwt -> authenticated(jwt, current, chain))
                .onErrorResume(JwtException.class,
                        error -> reject(current, HttpStatus.UNAUTHORIZED, "未登录或登录已失效"))
                .onErrorResume(IllegalArgumentException.class,
                        error -> reject(current, HttpStatus.UNAUTHORIZED, "未登录或登录已失效"));
    }

    private Mono<Void> authenticated(Jwt jwt, ServerWebExchange exchange, GatewayFilterChain chain) {
        String accountId = jwt.getSubject();
        String sid = jwt.getClaimAsString("sid");
        List<String> roles = jwt.getClaimAsStringList("roles");
        List<String> permissions = jwt.getClaimAsStringList("permissions");
        Number accountType = jwt.getClaim("account_type");
        if (accountId == null || !accountId.matches("[1-9][0-9]{0,18}")
                || sid == null || !sid.matches("[a-f0-9-]{36}")
                || !"access".equals(jwt.getClaimAsString("token_type"))
                || !jwt.getAudience().contains(expectedAudience)
                || accountType == null || accountType.intValue() != 1
                || roles == null || !roles.contains("NORMAL_USER")
                || permissions == null
                || !safeHeaderClaims(roles, 1024) || !safeHeaderClaims(permissions, 2048)
                || jwt.getId() == null || jwt.getId().isBlank()) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
        }
        // Revocation is effective immediately: Redis key deletion invalidates all access JWTs of the session.
        return Mono.zip(
                    redis.opsForHash().get(SESSION_PREFIX + sid, "accountId")
                            .map(Object::toString).defaultIfEmpty(""),
                    redis.opsForHash().get(SESSION_PREFIX + sid, "status")
                            .map(Object::toString).defaultIfEmpty("")
                )
                .onErrorReturn(Tuples.of("__redis_unavailable__", ""))
                .flatMap(session -> {
                    if ("__redis_unavailable__".equals(session.getT1())) {
                        return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, "认证服务暂不可用");
                    }
                    if (!accountId.equals(session.getT1()) || !"ACTIVE".equals(session.getT2())) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, "未登录或登录已失效");
                    }
                    ServerHttpRequest downstream = exchange.getRequest().mutate().headers(h -> {
                        h.set("X-Authenticated-Account-Id", accountId);
                        h.set("X-Authenticated-Account-Type", "NORMAL_USER");
                        h.set("X-Authenticated-Roles", String.join(",", roles));
                        h.set("X-Authenticated-Permissions", String.join(",", permissions));
                        h.set("X-Authenticated-Session-Id", sid);
                        h.remove(HttpHeaders.AUTHORIZATION);
                    }).build();
                    return chain.filter(exchange.mutate().request(downstream).build());
                });
    }

    private boolean safeHeaderClaims(List<String> values, int maxLength) {
        if (values.size() > 100) return false;
        int length = 0;
        for (String value : values) {
            if (value == null || !value.matches("[A-Za-z0-9:_./-]{1,128}")) return false;
            length += value.length() + 1;
        }
        return length <= maxLength;
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        exchange.getResponse().getHeaders().setCacheControl("no-store");
        byte[] body = ("{\"code\":" + status.value() + ",\"message\":\"" + message
                + "\",\"data\":null}").getBytes(StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() { return -90; }
}
