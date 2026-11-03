package com.mall.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 库存服务启动入口；本阶段仅负责容器、服务发现和 Flyway 初始化。
 * 不暴露库存锁定、确认、释放等业务 API。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class InventoryServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
