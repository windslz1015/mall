## 1. 项目说明
本项目是一个基于 Java / Spring Boot 的微服务商城系统。
项目处于持续开发和学习完善阶段。
在分析或修改代码之前，应优先阅读现有代码、配置和模块结构，以仓库中的实际实现为准，不应仅根据技术栈说明推测项目已经实现某项能力。
## 2. 核心原则
### 2.1 先检查，再建议
任何涉及项目实现的问题之前，应先检查相关现有代码。<br>
例如讨论用户注册功能时，应尽可能检查：
- Controller
- Request / Response DTO
- Application Service
- Domain Model
- Domain Service / Policy
- Repository 接口
- Repository 实现
- Mapper
- PO / DO
- 数据库表结构或 migration
- ErrorCode
- BusinessException
- GlobalExceptionHandler
- application.yml
- pom.xml
- 相关测试
不要在没有检查的情况下直接假设某项能力不存在。
---
### 2.2 不重复实现已有能力
新增代码之前，应先搜索项目中是否已经存在类似能力，例如：
- 通用 Result
- BusinessException
- ErrorCode
- 密码编码器
- 日志组件
- Redis 工具
- 分布式锁
- 全局异常处理
- 通用分页模型
- ID 生成器
- 时间工具类
如果已有能力可以复用，应优先复用。
---
### 2.3 不为了“生产级”而堆技术
技术方案必须服务于实际问题。
不要因为项目技术栈中存在 Redis、RabbitMQ、Seata、Sentinel 等组件，就默认某个业务必须使用这些组件。
引入新组件或复杂设计之前，应说明：
1. 当前存在什么问题；
2. 为什么现有方案不足；
3. 新方案解决什么问题；
4. 当前阶段是否真的需要。
---
## 3. 技术栈
项目当前采用或计划采用以下技术栈：
### 3.1 编程语言
- Java / JDK：作为项目主要开发语言与运行环境。
### 3.2 交互
- Vue：负责系统前端页面开发与用户交互。
### 3.3 接入
- - Nginx：负责反向代理、静态资源访问和负载均衡。
### 3.4 应用
- Maven：负责项目依赖管理与构建。
- Spring Boot：作为各微服务的基础开发框架。
- Spring MVC：负责 HTTP 接口开发与请求响应处理。
- Lombok：用于减少 Getter、Setter、构造方法等重复代码。
- MyBatis：负责数据库访问与 SQL 映射。
- MyBatis-Plus：在 MyBatis 基础上提供通用 CRUD 等能力。
### 3.5 微服务基础设施
- Spring Cloud Alibaba：作为微服务体系基础框架。
- Nacos：负责服务注册、服务发现和配置中心。
- Spring Cloud Gateway：作为系统统一 API 入口，负责路由、鉴权、过滤等。
- OpenFeign：用于微服务之间的 HTTP 远程调用。
- Sentinel：用于限流、熔断、降级和系统保护。
- Seata：用于部分需要跨服务事务一致性的业务场景。
### 3.6 数据
- MySQL：存储用户、商品、订单、库存、支付等核心业务数据。
- Redis：用于缓存、验证码、登录状态、热点数据、库存预扣等高频访问场景。
- Redisson：用于分布式锁、信号量、限流等 Redis 分布式能力。
- Elasticsearch：用于商品全文检索、条件搜索和复杂查询。
- MinIO：用于商品图片等对象文件存储。
### 3.7 异步与任务调度
- RabbitMQ：用于业务事件异步处理、服务解耦和削峰填谷。
- XXL-JOB：用于订单超时关闭、数据同步、定时统计等任务调度。
### 3.8 部署
- Docker：负责基础组件及应用服务的容器化部署。
### 3.9 可观测性
项目后续逐步完善日志、指标和链路追踪能力：
- Micrometer：作为应用指标采集门面。
- Prometheus：负责指标采集、存储和告警。
- Grafana：负责指标数据可视化。
- ELK / Loki：用于日志集中采集、存储和查询。
- SkyWalking / Zipkin / Micrometer Tracing：用于分布式链路追踪。
- Spring Boot Actuator：提供健康检查、指标等应用运维管理端点。
### 技术栈说明
说明：技术栈列表表示项目当前使用或计划使用的技术方向，不代表所有组件均已完成集成。<br>
判断某项技术是否已经实际使用时，应优先检查：
- pom.xml
- application.yml / application-*.yml
- 配置类
- 实际业务代码
- Docker / 部署配置

不要仅根据本文档中的技术栈列表，直接假设某项能力已经存在。
---
## 4. 项目分层原则
业务模块优先遵循以下分层结构：
```text
interfaces
    ↓
application
    ↓
domain
    ↑
infrastructure