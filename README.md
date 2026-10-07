# Mall

一个用于学习和面试展示的简易多商家电商微服务项目。

## 当前阶段

第一步只建立基础工程：

```text
mall
├── mall-common
├── mall-gateway
└── mall-user-service
```

暂时不连接 MySQL、Redis 和 RabbitMQ。

## 技术版本

- JDK 17
- Spring Boot 3.5.13
- Spring Cloud 2025.0.3
- Spring Cloud Alibaba 2025.0.0.0
- Maven 3.9+

## 模块职责

- `mall-common`：统一返回结构、通用错误码和业务异常。
- `mall-gateway`：统一入口，监听 `8080`，将 `/api/users/**` 转发给用户服务。
- `mall-user-service`：用户服务，监听 `8101`，当前只有一个连通性接口。

## 当前测试接口

用户服务直连：

```http
GET http://localhost:8101/api/users/ping
```

通过网关访问：

```http
GET http://localhost:8080/api/users/ping
```

成功响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "user-service is running"
}
```

## 启动前提

后续需要先启动 Nacos，然后依次启动：

1. `UserServiceApplication`
2. `GatewayApplication`

详细步骤见 `docs/step-01-project-scaffold.md`。


## 技术栈
开发语言：
Java

基础框架：
Spring Boot
Spring MVC
Spring AOP
Jakarta Validation

微服务：
Spring Cloud Alibaba
Nacos
OpenFeign
Spring Cloud Gateway
Sentinel

数据访问：
MyBatis-Plus
MyBatis XML

数据库：
MySQL

缓存：
Redis

消息队列：
RabbitMQ

项目管理：
Maven
Git

接口测试：
Postman
Swagger / OpenAPI

前端： Vue，后期再接入
