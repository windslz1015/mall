
package com.mall.iam.infrastructure.config;

import com.xxl.job.core.executor.impl.XxlJobSpringExecutor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class XxlJobConfig {

    // XXL-JOB Admin 地址
    @Value("${xxl.job.admin.addresses}")
    private String adminAddresses;

    // 通信 Token
    @Value("${xxl.job.admin.accessToken}")
    private String accessToken;

    // 通信超时时间
    @Value("${xxl.job.admin.timeout:3}")
    private int timeout;

    // 是否启用执行器
    @Value("${xxl.job.executor.enabled:true}")
    private Boolean enabled;

    // 执行器名称
    @Value("${xxl.job.executor.appname}")
    private String appname;

    // 执行器注册地址
    @Value("${xxl.job.executor.address:}")
    private String address;

    // 执行器 IP
    @Value("${xxl.job.executor.ip:}")
    private String ip;

    // 执行器端口
    @Value("${xxl.job.executor.port:9998}")
    private int port;

    // 执行日志目录
    @Value("${xxl.job.executor.logpath}")
    private String logPath;

    // 日志保留天数
    @Value("${xxl.job.executor.logretentiondays:30}")
    private int logRetentionDays;

    // 排除扫描的包
    @Value("${xxl.job.executor.excludedpackage:}")
    private String excludedPackage;

    @Bean
    public XxlJobSpringExecutor xxlJobExecutor() {

        XxlJobSpringExecutor executor =
                new XxlJobSpringExecutor();

        // 1. 配置调度中心地址
        executor.setAdminAddresses(adminAddresses);

        // 2. 配置通信 Token
        executor.setAccessToken(accessToken);

        // 3. 配置通信超时时间
        executor.setTimeout(timeout);

        // 4. 配置执行器开关
        executor.setEnabled(enabled);

        // 5. 配置执行器名称
        executor.setAppname(appname);

        // 6. 配置注册地址
        executor.setAddress(address);

        // 7. 配置IP和端口
        executor.setIp(ip);
        executor.setPort(port);

        // 8. 配置日志
        executor.setLogPath(logPath);
        executor.setLogRetentionDays(logRetentionDays);

        // 9. 配置扫描排除包
        executor.setExcludedPackage(excludedPackage);

        return executor;
    }
}
