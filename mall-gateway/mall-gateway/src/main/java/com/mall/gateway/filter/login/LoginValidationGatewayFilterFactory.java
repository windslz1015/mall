package com.mall.gateway.filter.login;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

@Component
public class LoginValidationGatewayFilterFactory
        extends AbstractGatewayFilterFactory<LoginValidationGatewayFilterFactory.Config> {
    public LoginValidationGatewayFilterFactory() { super(Config.class); }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            if (exchange.getRequest().getMethod() != HttpMethod.POST) {
                exchange.getResponse().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                return exchange.getResponse().setComplete();
            }
            MediaType type = exchange.getRequest().getHeaders().getContentType();
            if (type == null || !MediaType.APPLICATION_JSON.isCompatibleWith(type)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return exchange.getResponse().setComplete();
            }
            if (exchange.getRequest().getHeaders().getContentLength() == 0) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
            return chain.filter(exchange);
        };
    }
    public static class Config {}
}
