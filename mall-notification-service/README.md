# mall-notification-service（DDD + Flyway + RabbitMQ 注册欢迎站内信）

## 定位与当前实现

本模块可以作为你当前 `mall` 父 Maven 工程的第 12 个业务模块，命名和版本约定与 `mall-product-service` 一致：

| 项目 | 配置 |
| --- | --- |
| Maven 模块 | `mall-notification-service` |
| Spring 应用名 | `notification-service` |
| Java 包名 | `com.mall.notification` |
| 默认端口 | `8110` |
| Java / Spring Boot | Java 17 / Spring Boot 3.5.13（继承父 POM） |
| 数据库 | 独立 MySQL 库 `mall_notification` |
| 注册发现 | Nacos |
| RabbitMQ 来源 | IAM `iam.events` 交换机，`account.registered` 路由键 |
| Notification 自己的队列 | `notification.account.registered` |
| 失败死信队列 | `notification.account.registered.dlq` |

**这不是只有空目录的骨架**：已经实现 IAM `AccountRegistered` 消息消费、消息头/事件字段验证、Inbox 幂等、站内欢迎消息持久化、失败有限重试与死信队列、列表和标已读接口。按 DDD 四层拆分；MySQL 使用 `JdbcTemplate` Repository（保留 MyBatis-Plus 依赖供后续开发）。

**关于“发送欢迎通知”**：当前实际实现的是**站内信**（写入数据库，用户登录后可以查询），**不是**向用户手机或电子邮箱发送真实消息。后者需要额外购买/接入短信、邮件服务商及模板，不能仅凭 IAM 的当前事件完成；IAM 当前事件不包含手机号或邮箱。

## 注册后数据流

```text
Vue -> Nginx -> Gateway -> IAM -> MySQL (账号、资料、角色、IAM Outbox)
                                     |
                                     v
                                  RabbitMQ
                                iam.events
                        routing: account.registered
                                     |
                                     v
                    notification.account.registered (独立队列)
                                     |
                                     v
                          Notification Service
                           MySQL (Inbox + 站内欢迎消息)

用户查询：Vue -> Nginx -> Gateway -> Notification -> mall_notification -> Vue
```

消息体与 IAM 代码 `com.mall.iam.application.account.event.AccountRegisteredEvent` 兼容：

```json
{
  "eventId": "6c95d48a-4b9a-4c0a-84f2-e137db4b5c63",
  "accountId": 42,
  "accountNo": "0a1b2c3d4e5f67890123456789abcdef",
  "username": "demo",
  "accountType": 1,
  "occurredAt": "2026-11-16T10:00:00Z"
}
```

事件必须包含消息头 `eventType=ACCOUNT_REGISTERED`、`messageId=eventId`。IAM 已采用这两个消息属性，无需更改其事件生产代码。账号类型 `1` 为普通用户；商家/管理员不会产生欢迎站内信。

**注意：** IAM 自己已经声明过 `iam.account.registered` 队列，Notification **不能去消费该队列**，否则可能和未来消费者竞争同一消息。这个模块自己声明 `notification.account.registered` 队列，通过相同交换机/路由键订阅一份消息。

## 接入现有工程

解压缩到 `mall` 根目录后，按照压缩包根目录的 `ADD_TO_PROJECT.md` 操作，或在 Windows PowerShell 执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\install-notification.ps1
```

脚本只会在根 `pom.xml` 中加入模块，并在 `mall-gateway/src/main/resources/application.yml` 中加入 Notification 路由；**不会创建数据库、覆盖已有业务文件或修改 IAM 源码**。

## 环境变量

启动前为 `NotificationServiceApplication` 设置：

```dotenv
NOTIFICATION_DB_URL=jdbc:mysql://localhost:3306/mall_notification?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai
NOTIFICATION_DB_USERNAME=notification_app
NOTIFICATION_DB_PASSWORD=CHANGE_ME
NACOS_SERVER_ADDR=localhost:8848
NACOS_USERNAME=nacos
NACOS_PASSWORD=CHANGE_ME
RABBITMQ_HOST=localhost
RABBITMQ_PORT=5672
RABBITMQ_USERNAME=CHANGE_ME
RABBITMQ_PASSWORD=CHANGE_ME
NOTIFICATION_SERVER_PORT=8110
```

这些 `CHANGE_ME` 是示例占位符；请自行填入你环境里的真实凭证，不要将密码提交到 Git。IAM 和 Notification 需要连接到**同一个 RabbitMQ 虚拟主机**（默认 `/`）。MySQL 数据库要先执行 `docs/create-database.sql`，Flyway 才能自动创建表。

## 用户站内通知接口（需经过 Gateway）

Gateway 路由 `/api/notification/**` 会转到本服务 `/api/**`。

1. `GET /api/notification/messages?page=1&size=20`：查询**当前已登录用户**的站内通知列表。参数 `page=1..10000`、`size=1..100`。结果采用项目统一的 `com.mall.common.api.Result`。
2. `POST /api/notification/messages/{notificationId}/read`：当前用户标记指定通知为已读，成功时 `data=true`；不属于当前用户或不存在则 `data=false`。
3. `GET /actuator/health`：健康检查，可用于部署验证。

接口从现有 Gateway `AuthGlobalFilter` 注入的 `X-Authenticated-Account-Id` 获取当前账号ID，不接受前端自报的accountId参数。**务必将Notification端口仅暴露于受信任的内部服务网络，禁止公网/用户绕过Gateway直接访问端口并伪造认证请求头**。这是当前项目现有网关用户身份传递模式的部署前提。

## 消息幂等和故障

- `inbox_event` `(consumer_name, event_id)` 唯一；遇到相同ID不同原始JSON指纹，拒绝处理。
- `notification_message.biz_key` 唯一，防止同一账号因为事件重发而有多条欢迎消息。
- **Inbox写入和欢迎消息写入在同一个 `@Transactional` MySQL事务中完成**：提交成功才由Spring AMQP回复消费ACK；崩溃后重投不会重复生成。
- 接收失败时最多尝试3次，仍失败则路由至 `notification.account.registered.dlq`，需要检查原因后**手动受控重放**；初始化版本不自动从死信队列循环重放。
- RabbitMQ订阅队列要在用户注册前已声明（启动Notification一次即可），RabbitMQ对队列声明前的历史事件**不会补发**；老账号需要专门的数据回填，不要误认注册事务失败。
- IAM Outbox 才是注册事件可靠生产方；本模块是消费方，因此当前只建 Inbox，不需要为没有下游事件的通知服务额外设计Outbox或XXL-JOB。

## 项目结构

```text
com.mall.notification
├── NotificationServiceApplication.java
├── interfaces/controller/NotificationController.java       # 站内信查询/已读
├── application/notification/
│   ├── command/AccountRegisteredEvent.java               # 匹配IAM JSON
│   ├── result/NotificationPage.java
│   └── service/
│       ├── WelcomeNotificationApplicationService.java    # 注册欢迎通知
│       └── NotificationQueryApplicationService.java
├── domain/
│   ├── inbox/repository/InboxEventRepository.java
│   └── notification/
│       ├── model/NotificationMessage.java
│       └── repository/NotificationRepository.java
└── infrastructure/
    ├── config/NotificationRabbitConfig.java
    ├── messaging/rabbit/AccountRegisteredListener.java
    └── persistence/
        ├── inbox/JdbcInboxEventRepository.java
        └── notification/JdbcNotificationRepository.java
```

## 启动和检查

在根目录完成父POM配置和数据库创建后：

```bash
mvn -pl mall-notification-service -am clean install
mvn -pl mall-notification-service spring-boot:run
```

从IDEA也可以直接启动 `NotificationServiceApplication`。确保RabbitMQ、MySQL、Nacos已启动；启动日志中不应出现Rabbit队列声明冲突。

然后通过IAM已有注册接口注册一位新普通用户，登录并用Access Token请求站内消息查询接口。可以用 `docs/verify-schema.sql` 检查是否有 `WELCOME` 类型消息。异常消息在RabbitMQ管理页面查看 `notification.account.registered.dlq`。

## 暂不实现

短信/邮件服务商、实时WebSocket推送、Redis缓存、消息订阅偏好、通知模板配置、站外推送补偿、运营群发。若需要这些能力，可在当前站内消息模块上扩展，不应把外部短信发送放进IAM注册事务。
