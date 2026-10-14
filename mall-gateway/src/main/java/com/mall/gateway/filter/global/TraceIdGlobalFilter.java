package com.mall.gateway.filter.global;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
public class TraceIdGlobalFilter implements GlobalFilter, Ordered {

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    // 只接受32位十六进制TraceId
    private static final Pattern TRACE_ID_PATTERN =
            Pattern.compile("^[a-fA-F0-9]{32}$");
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        // 1尝试获取上游传来的 TraceId
        String traceId = exchange.getRequest()
                .getHeaders()
                .getFirst(TRACE_ID_HEADER);
        // 2不存在或格式不合法时重新生成
        if (traceId == null ||
                !TRACE_ID_PATTERN.matcher(traceId).matches()) {
            traceId = UUID.randomUUID()
                    .toString().replace("-", "");
        }

        // lambda 中使用，所以保存为 final 变量
        String finalTraceId = traceId;

        // 3设置请求的的请求头：将TraceId加入
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .headers(headers ->
                        headers.set(TRACE_ID_HEADER, finalTraceId))
                .build();
        // 4设置响应的响应头：同时将TraceId放入
        exchange.getResponse()
                .getHeaders()
                .set(TRACE_ID_HEADER, finalTraceId);
        // 5使用修改后的 request 继续执行过滤器链
        return chain.filter(
                exchange.mutate()
                        .request(request)
                        .build()
        );
    }

    @Override
    public int getOrder() {
        return -100;
    }
}