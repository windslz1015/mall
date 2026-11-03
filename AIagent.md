## 1. 项目说明
本项目是一个基于 Java / Spring Boot 的微服务商城系统。
项目处于持续开发和学习完善阶段。
在分析或修改代码之前，应优先阅读现有代码、配置和模块结构，以仓库中的实际实现为准，不应仅根据技术栈说明推测项目已经实现某项能力。
## 2. AI代理：
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
