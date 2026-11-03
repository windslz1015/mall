package com.mall.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 购物车服务的启动入口。
 * 当前阶段只启动 Spring 容器、Nacos Discovery 和 Flyway，不提供购物车业务 API。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class CartServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CartServiceApplication.class, args);
    }
}
