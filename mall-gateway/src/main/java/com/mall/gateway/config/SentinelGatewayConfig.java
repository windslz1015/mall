
package com.mall.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayFlowRule;
import com.alibaba.csp.sentinel.adapter.gateway.common.rule.GatewayRuleManager;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

import java.util.Map;
import java.util.Set;

@Configuration
public class SentinelGatewayConfig {

    @PostConstruct
    public void init() {

        // 1注册接口专用限流：每秒最多10个请求
        GatewayFlowRule registerRule =
                new GatewayFlowRule("iam-register-route")
                        .setCount(10)
                        .setIntervalSec(1);

        // 2加载Gateway限流规则
        GatewayRuleManager.loadRules(
                Set.of(registerRule)
        );

        // 3自定义Sentinel限流响应：HTTP 429
        GatewayCallbackManager.setBlockHandler(
                (exchange, throwable) ->
                        ServerResponse.status(
                                        HttpStatus.TOO_MANY_REQUESTS)
                                .contentType(
                                        MediaType.APPLICATION_JSON)
                                .bodyValue(
                                        Map.of(
                                                "code", 429,
                                                "message", "请求过于频繁")
                                            )
        );//ServerResponse 帮你把这些都处理好了，它最后内部也是走 writeWith，只是由框架在你交出 Mono 之后统一调用。
    }
}