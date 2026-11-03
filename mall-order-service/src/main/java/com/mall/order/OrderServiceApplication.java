package com.mall.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 交易订单服务启动入口。当前阶段仅初始化 DDD 包、数据源、Nacos 与 Flyway。
 * 尚无下单、取消、支付回执、超时任务等业务接口或执行代码。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
