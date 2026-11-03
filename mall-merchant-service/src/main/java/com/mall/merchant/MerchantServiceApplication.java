package com.mall.merchant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 商家业务域微服务启动入口。
 * 本阶段仅初始化工程与 Flyway，不开放入驻、审核、商家管理业务接口。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class MerchantServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(MerchantServiceApplication.class, args);
    }
}
