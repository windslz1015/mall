
package com.mall.gateway.filter.register;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class RegisterValidationGatewayFilterFactory extends AbstractGatewayFilterFactory<RegisterValidationGatewayFilterFactory.Config> {

    //注册校验过滤器
    public RegisterValidationGatewayFilterFactory() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {

        return (exchange, chain) -> {

            //1只允许POST请求
            if (exchange.getRequest().getMethod() != HttpMethod.POST) {
                exchange.getResponse().setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);
                return exchange.getResponse().setComplete();
            }
            // 2校验请求Content-Type
            MediaType contentType = exchange.getRequest().getHeaders().getContentType();
            if (contentType == null || !MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
                exchange.getResponse().setStatusCode(HttpStatus.UNSUPPORTED_MEDIA_TYPE);
                return exchange.getResponse().setComplete();
            }
            // 3检查请求体是否明确声明为空
            long contentLength = exchange.getRequest().getHeaders().getContentLength();
            if (contentLength == 0) {
                exchange.getResponse().setStatusCode(HttpStatus.BAD_REQUEST);
                return exchange.getResponse().setComplete();
            }
            // 4继续执行注册链路
            return chain.filter(exchange);
        };
    }

    public static class Config { }
}
