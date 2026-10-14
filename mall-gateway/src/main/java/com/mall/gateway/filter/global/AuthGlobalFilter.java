
package com.mall.gateway.filter.global;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String REGISTER_PATH = "/api/iam/auth/register";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getRawPath();

        // 1注册接口加入白名单，跳过登录Token认证
        if (REGISTER_PATH.equals(path)) {
            return chain.filter(exchange);
        }
        // 2其他接口需要进行token认证
        //token认证:
        String authorization =
                request.getHeaders().getFirst("Authorization");

        if (authorization == null
                || !authorization.startsWith("Bearer ")
                || authorization.substring(7).isBlank()) {

            exchange.getResponse()
                    .setStatusCode(HttpStatus.UNAUTHORIZED);

            return exchange.getResponse().setComplete();
        }

        // TODO: 调用真正的Token校验逻辑
        // 当前仅检查Authorization格式，不代表认证通过
        // 未接入认证服务前，不应直接放行受保护接口

        exchange.getResponse()
                .setStatusCode(HttpStatus.UNAUTHORIZED);

        return exchange.getResponse().setComplete();
    }

    @Override
    public int getOrder() {
        return -90;
    }
}
