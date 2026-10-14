
package com.mall.gateway.filter.register;

import com.mall.gateway.service.register.RegisterRiskService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Component
public class RegisterRiskGatewayFilterFactory
        extends AbstractGatewayFilterFactory<
        RegisterRiskGatewayFilterFactory.Config> {

    private final RegisterRiskService riskService;
    private final String trustedNginxIp;

    public RegisterRiskGatewayFilterFactory(
            RegisterRiskService riskService,
            @Value("${gateway.security.trusted-nginx-ip}")
            String trustedNginxIp) {

        super(Config.class);
        this.riskService = riskService;
        this.trustedNginxIp = trustedNginxIp;
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            // 1. 获取真实客户端IP
            String clientIp = getClientIp(exchange);

            // 2. 无法确认客户端IP，拒绝请求
            if (clientIp == null) {
                return reject(exchange, HttpStatus.FORBIDDEN);
            }

            // 3. 调用Redis风控服务，检查IP注册频率
            // Redis异常时拒绝请求，避免绕过风控
            return riskService.checkIp(clientIp)
                    .onErrorResume(error ->
                            Mono.error(
                                    new RiskCheckException(
                                            "注册风控服务不可用",
                                            error
                                    )
                            )
                    )
                    .flatMap(allowed -> {

                        // 4. 超出注册频率，返回429
                        if (!allowed) {
                            return reject(
                                    exchange,
                                    HttpStatus.TOO_MANY_REQUESTS
                            );
                        }

                        // 5. 风控通过，继续执行过滤器链
                        return chain.filter(exchange);
                    });
        };
    }

    // 获取客户端IP
    private String getClientIp(ServerWebExchange exchange) {

        InetSocketAddress remoteAddress =
                exchange.getRequest().getRemoteAddress();

        if (remoteAddress == null ||
                remoteAddress.getAddress() == null) {
            return null;
        }

        String remoteIp = remoteAddress.getAddress()
                .getHostAddress();

        // 只有请求来自可信Nginx时，才使用X-Real-IP
        if (trustedNginxIp.equals(remoteIp)) {

            String forwardedIp = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Real-IP");

            if (!StringUtils.hasText(forwardedIp)) {
                return null;
            }

            return parseIp(forwardedIp);
        }

        // 非可信Nginx来源不允许访问注册路由
        return null;
    }

    // 校验IP地址格式
    private String parseIp(String ip) {

        String value = ip.trim();

        // 简单限制输入字符和长度
        if (value.length() > 45 ||
                !value.matches("[0-9a-fA-F:.]+")) {
            return null;
        }

        try {
            InetAddress address =
                    InetAddress.getByName(value);

            return address.getHostAddress();

        } catch (Exception e) {
            return null;
        }
    }

    // 拒绝请求
    private Mono<Void> reject(
            ServerWebExchange exchange,
            HttpStatus status) {

        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    // 风控异常
    private static class RiskCheckException
            extends RuntimeException {

        public RiskCheckException(
                String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class Config {
    }
}