package com.mall.gateway.filter.login;

import com.mall.gateway.service.login.LoginRiskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import java.net.InetAddress;
import java.net.InetSocketAddress;

@Component
public class LoginRiskGatewayFilterFactory extends AbstractGatewayFilterFactory<LoginRiskGatewayFilterFactory.Config> {
    private final LoginRiskService service;
    private final String trustedNginxIp;

    public LoginRiskGatewayFilterFactory(LoginRiskService service,
            @Value("${gateway.security.trusted-nginx-ip}") String trustedNginxIp) {
        super(Config.class);
        this.service = service;
        this.trustedNginxIp = trustedNginxIp;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String clientIp = clientIp(exchange);
            if (clientIp == null) return reject(exchange, HttpStatus.FORBIDDEN);
            return service.checkIp(clientIp)
                    .onErrorMap(RiskUnavailableException::new)
                    .flatMap(allowed -> {
                        if (!allowed) return reject(exchange, HttpStatus.TOO_MANY_REQUESTS);
                        ServerHttpRequest changed = exchange.getRequest().mutate()
                                .headers(h -> h.set("X-Verified-Client-IP", clientIp)).build();
                        return chain.filter(exchange.mutate().request(changed).build());
                    })
                    .onErrorResume(RiskUnavailableException.class,
                            error -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE));
        };
    }

    private String clientIp(ServerWebExchange exchange) {
        InetSocketAddress remote = exchange.getRequest().getRemoteAddress();
        if (remote == null || remote.getAddress() == null
                || !trustedNginxIp.equals(remote.getAddress().getHostAddress())) return null;
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Real-IP");
        if (forwarded == null || !forwarded.matches("[0-9a-fA-F:.]{1,45}")) return null;
        try { return InetAddress.getByName(forwarded).getHostAddress(); }
        catch (Exception ignored) { return null; }
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }
    private static class RiskUnavailableException extends RuntimeException {
        RiskUnavailableException(Throwable cause) { super(cause); }
    }
    public static class Config {}
}
