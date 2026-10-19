# Mall 系统架构说明

## 1. 项目定位

本项目是一个基于 Java / Spring Boot 构建的微服务商城系统。

主要目标包括：

- 用户与身份认证
- 商品管理
- 商品搜索
- 购物车
- 订单
- 库存
- 支付
- 消息通知
- 后台管理
- 系统监控与运维


## 2. 项目架构
目标总体结构：

```text
                                 Vue
                                   │
                                   ↓
                                Nginx
                                   │
                                   ↓
                      Spring Cloud Gateway
                 （统一入口、路由、鉴权、过滤、跨域）
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        ↓              ↓           ↓           ↓              ↓
  账号与权限服务    商家服务     商品服务    购物车服务         交易订单服务
  IAM           Merchant     Product      Cart          Order/Trade
        │              │           │           │              │
        └──────────────┴───────────┼───────────┴──────────────┘
                                   │
        ┌──────────────┬───────────┼───────────┬──────────────┐
        ↓              ↓           ↓           ↓              ↓
    库存服务        支付服务    平台运营服务  业务保障服务   通知服务（可选）
   Inventory       Payment     Admin/Ops     Assurance     Notification
        │              │           │           │              │
        └──────────────┴───────────┴───────────┴──────────────┘
                                   │
                        Spring Cloud Alibaba
                                   │
              ┌────────────────────┼────────────────────┐
              ↓                    ↓                    ↓
            Nacos              Sentinel             OpenFeign
                                   │
                                 Seata
基础设施：
MySQL
Redis
RabbitMQ
Elasticsearch
MinIO
XXL-JOB
```
## 3. 项目技术栈
目标总体结构：

项目技术栈：  
开发语言：JDK：Java 应用运行与开发环境。  
应用：  
入口层：vue  
接入层：  
Nginx：负责反向代理、静态资源访问、负载均衡等。  
应用层：
应用框架：
Spring Boot：作为各微服务的基础开发框架。  
Web框架：  
Spring MVC：负责 HTTP 接口开发与请求响应处理。  
持久化框架：  
MyBatis，MyBatis-Plus：负责 Java 对象与数据库表之间的数据访问与持久化操作。  
异步与任务调度：  
RabbitMQ：用于订单、库存、支付等业务事件的异步处理、解耦和削峰填谷。  
XXL-JOB：用于订单超时关闭、数据同步、定时统计等任务调度。  
认证和安全：  
JWT：用于用户身份认证与 Token 机制。可结合 Redis 实现 Token 状态管理、登录失效控制等。
微服务基础设施：  
Spring Cloud Alibaba：作为微服务体系基础框架。  
Nacos：负责服务注册、服务发现和配置中心。  
Spring Cloud Gateway：作为系统统一 API 入口，完成路由、鉴权、过滤等。   
OpenFeign：用于微服务之间的 HTTP 远程调用。  
Sentinel：用于限流、熔断、降级和系统保护。  
Seata：用于部分跨服务场景下的分布式事务处理。  
数据：
MySQL：存储用户、商品、订单、库存、支付等核心业务数据。    
Redis：用于缓存、验证码、登录状态、热点数据、库存预扣等高频访问场景。  
Redisson：用于分布式锁、信号量、限流等 Redis 分布式能力。  
Elasticsearch：用于商品全文检索、条件搜索和复杂查询。                        
MinIO：用于商品图片等对象文件存储。  
部署：  
Docker：负责 MySQL、Redis、RabbitMQ、Nacos 等基础组件及应用服务的容器化部署。  
运维：  
Micrometer门面：作为应用指标采集门面，统一采集 JVM、HTTP 请求、数据库连接池、Redis、RabbitMQ 等指标，其中对接Prometheus 的实现叫PrometheusMeterRegistry。  
Prometheus：负责定时拉取并存储指标数据，支持告警规则配置。  
Grafana：负责监控大盘可视化，接入Prometheus数据源，展示 JVM、接口 QPS、响应时间、错误率、连接池、系统资源等指标。
链路追踪：SkyWalking / Zipkin / Micrometer Tracing 等。  
日志：ELK / Loki等。  
Spring Boot Actuator：提供应用健康检查、指标、线程、堆内存、HTTP 端点等运维监控端点。（springboot运维管理框架）  

ai工具：ChatGPT，codex  

## 4. 项目分层结构
DDD分层：  
interfaces  
    ↓  
application  
    ↓  
domain  
    ↑  
infrastructure  