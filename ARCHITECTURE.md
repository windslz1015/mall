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

项目目前处于持续开发阶段。

本文档同时记录：

- 当前已经实现的架构
- 正在开发的能力
- 后续规划能力

文档中的“目标架构”不代表当前全部已经实现。

---

# 2. 总体架构

目标总体结构：

```text
                       Vue
                        │
                        ↓
                     Nginx
                        │
                        ↓
              Spring Cloud Gateway
                        │
          ┌─────────────┼─────────────┐
          ↓             ↓             ↓
        IAM           Product       Order
      Service         Service       Service
          │             │             │
          └─────────────┼─────────────┘
                        │
                Spring Cloud Alibaba
                        │
              ┌─────────┼─────────┐
              ↓         ↓         ↓
            Nacos    Sentinel   OpenFeign


基础设施：

MySQL
Redis
RabbitMQ
Elasticsearch
MinIO
XXL-JOB