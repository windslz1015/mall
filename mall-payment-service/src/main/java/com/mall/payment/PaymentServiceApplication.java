package com.mall.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 支付服务启动入口：当前仅初始化 DDD 包、MySQL、Nacos 和 Flyway。
 * 不含支付请求、第三方回调校验、消息投递、对账或退款业务。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
