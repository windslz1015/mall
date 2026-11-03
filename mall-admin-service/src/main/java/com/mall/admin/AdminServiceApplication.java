package com.mall.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 平台运营管理服务启动入口。当前仅加载基础配置、Nacos 和 Flyway。
 * 尚未提供后台查询、监管案件处置或跨服务管理接口。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class AdminServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AdminServiceApplication.class, args);
    }
}
