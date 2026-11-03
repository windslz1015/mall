package com.mall.reliability;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * 业务保障服务启动类：当前只有基本依赖、注册发现和 Flyway 初始化。
 * 不会自动扫描订单、消费队列、执行重试或修改其他服务的数据。
 */
@EnableDiscoveryClient
@SpringBootApplication
public class ReliabilityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ReliabilityServiceApplication.class, args);
    }
}
