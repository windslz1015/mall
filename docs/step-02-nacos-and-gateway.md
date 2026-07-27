# Step 02：Nacos and Gateway（服务注册、发现与网关路由）

> 文件名：`step-02-nacos-and-gateway.md`  
> 前置条件：已经独立完成 Step 01，并且根目录执行 Maven 构建成功。  
> 本步原则：**自己创建 Compose、脚本和配置，然后亲自启动、观察、请求和排错。**

---

## 1. 本步要完成什么

本步完成一条最小微服务调用链：

```text
客户端
  ↓
Gateway :8080
  ↓ 通过 Nacos 查询 user-service 实例
User Service :8101
  ↓
返回统一 JSON
```

具体目标：

1. 使用 Docker Compose 启动单机 Nacos；
2. 用户服务注册到 Nacos；
3. Gateway 注册到 Nacos；
4. Gateway 通过服务名发现用户服务；
5. 使用 `lb://user-service` 转发请求；
6. 分别验证用户服务直连和 Gateway 转发；
7. 学会看 Nacos、Java 服务和 Gateway 的第一处错误日志。

### 本步仍然不做

- 不连接 MySQL；
- 不建用户表；
- 不实现注册登录；
- 不接 Redis；
- 不接 RabbitMQ；
- 不做鉴权过滤器；
- 不做 Sentinel 限流。

---

## 2. 先理解 Nacos 在这里负责什么

### 2.1 服务注册

用户服务启动时，会向 Nacos 上报：

```text
服务名：user-service
IP：当前服务实例 IP
端口：8101
健康状态：healthy
```

Gateway 也会以 `gateway-service` 的名称注册。

### 2.2 服务发现

Gateway 不需要把用户服务地址写死成：

```text
http://127.0.0.1:8101
```

而是写：

```text
lb://user-service
```

Gateway 根据服务名从 Nacos 获取健康实例，再由 LoadBalancer 选择一个实例。

### 2.3 为什么要先做 Ping 接口

真正的注册、登录业务会引入数据库、密码加密、参数校验和事务。当前用一个简单接口隔离这些复杂度，只验证微服务基础链路。

---

## 3. 本步端口规划

| 组件 | 宿主机端口 | 用途 |
|---|---:|---|
| Nacos Console | 8849 | 浏览器管理控制台 |
| Nacos Server HTTP | 8848 | Nacos API 和客户端主端口 |
| Nacos Client gRPC | 9848 | 客户端与服务端的 gRPC 通信 |
| Gateway | 8080 | 商城统一入口 |
| User Service | 8101 | 用户服务直连端口 |

Nacos 3.x 的新控制台默认使用容器端口 `8080`，但商城 Gateway 也要使用宿主机 `8080`。因此我们把：

```text
Nacos 容器 8080 → 宿主机 8849
```

这样两个程序不会冲突。

---

## 4. 检查 Docker 环境

### 4.1 Windows

建议安装并启动 Docker Desktop，然后在 PowerShell 执行：

```powershell
docker --version
docker compose version
```

### 4.2 macOS / Linux

```bash
docker --version
docker compose version
```

需要确认使用的是带空格的新命令：

```text
docker compose
```

不是旧命令：

```text
docker-compose
```

如果你的环境只有旧命令，也可以自行替换，但本教程统一使用 `docker compose`。

---

## 5. 补充 Gateway 的 LoadBalancer 依赖

打开：

```text
mall-gateway/pom.xml
```

在 `<dependencies>` 中加入：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

建议放在 Nacos Discovery 依赖之后，并写清楚注释：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>

<!-- lb://user-service 路由需要 Spring Cloud LoadBalancer。 -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-loadbalancer</artifactId>
</dependency>
```

### 为什么必须加

Nacos Discovery 负责提供服务实例列表，LoadBalancer 负责从实例列表中选择一个目标实例。

没有这个依赖时，可能出现：

- `lb://user-service` 无法处理；
- Gateway 返回 503；
- 日志提示没有 LoadBalancer 支持。

修改后先执行：

```bash
mvn clean package -DskipTests
```

确保依赖正确。

---

## 6. 创建 Nacos Docker Compose 文件

在项目根目录创建：

```text
deploy/
```

再创建文件：

```text
deploy/docker-compose.nacos.yml
```

内容：

```yaml
services:
  nacos:
    image: nacos/nacos-server:v3.2.3
    container_name: simple-mall-nacos
    restart: unless-stopped

    environment:
      MODE: standalone
      PREFER_HOST_MODE: hostname

      # 本地学习环境关闭鉴权，避免第一次搭建被账号初始化干扰。
      # 生产环境不能这样配置。
      NACOS_AUTH_ENABLE: "false"
      NACOS_AUTH_ADMIN_ENABLE: "false"
      NACOS_AUTH_CONSOLE_ENABLE: "false"

      # Nacos Docker 镜像启动时仍要求提供下面三个安全参数。
      NACOS_AUTH_IDENTITY_KEY: simpleMallServerIdentity
      NACOS_AUTH_IDENTITY_VALUE: simpleMallServerSecurity
      NACOS_AUTH_TOKEN: VGhpc0lzTXlDdXN0b21TZWNyZXRLZXkwMTIzNDU2Nzg=

      JVM_XMS: 256m
      JVM_XMX: 512m
      JVM_XMN: 128m
      TZ: Asia/Shanghai

    ports:
      # Nacos 3 控制台容器端口为 8080；宿主机改为 8849，避开 Gateway。
      - "8849:8080"

      # Nacos Server HTTP 主端口。
      - "8848:8848"

      # Nacos 客户端 gRPC 端口。
      - "9848:9848"

    volumes:
      - nacos_data:/home/nacos/data
      - nacos_logs:/home/nacos/logs

volumes:
  nacos_data:
  nacos_logs:
```

### 6.1 Compose 配置解释

#### `MODE: standalone`

本地学习使用单机模式，不需要三节点集群，也不需要外部 MySQL。

#### `restart: unless-stopped`

Docker 或电脑重启后，容器可以自动恢复，除非你主动停止它。

#### Nacos 鉴权

本教程为了减少第一次搭建的干扰，关闭本地鉴权。

这个设置只适用于：

```text
个人电脑 + 本地学习 + 不对公网开放
```

不能直接用于生产环境。

#### JVM 内存

Nacos 默认内存配置对普通开发电脑可能偏大，所以本地设置为：

```text
Xms = 256 MB
Xmx = 512 MB
```

如果仍然频繁退出，可以适当提高 Docker Desktop 可用内存。

#### Apple Silicon 提示

若 M 系列 Mac 拉取或运行普通镜像时出现架构问题，可以尝试：

```yaml
image: nacos/nacos-server:v3.2.3-slim
```

仅在确实遇到架构问题时修改，不要同时改其他配置。

---

## 7. 创建启动和停止脚本

脚本不是必须的，但它能保证每次都从项目根目录使用同一份 Compose 文件。

创建目录：

```text
scripts/
```

### 7.1 Windows：启动脚本

创建：

```text
scripts/start-nacos.ps1
```

内容：

```powershell
$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $RootDir "deploy/docker-compose.nacos.yml"

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw "未找到 Docker。请先安装并启动 Docker Desktop。"
}

docker compose -f $ComposeFile up -d
docker compose -f $ComposeFile ps

Write-Host ""
Write-Host "Nacos 正在启动。"
Write-Host "控制台：http://127.0.0.1:8849/index.html"
Write-Host "健康检查：http://127.0.0.1:8848/nacos/v1/console/health/readiness"
Write-Host "查看日志：docker logs -f simple-mall-nacos"
```

### 7.2 Windows：停止脚本

创建：

```text
scripts/stop-nacos.ps1
```

内容：

```powershell
$ErrorActionPreference = "Stop"

$RootDir = Split-Path -Parent $PSScriptRoot
$ComposeFile = Join-Path $RootDir "deploy/docker-compose.nacos.yml"

docker compose -f $ComposeFile down

Write-Host "Nacos 已停止。"
```

### 7.3 macOS / Linux：启动脚本

创建：

```text
scripts/start-nacos.sh
```

内容：

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.nacos.yml"

if ! command -v docker >/dev/null 2>&1; then
  echo "未找到 Docker。请先安装并启动 Docker。" >&2
  exit 1
fi

docker compose -f "$COMPOSE_FILE" up -d
docker compose -f "$COMPOSE_FILE" ps

echo
echo "Nacos 正在启动。"
echo "控制台：http://127.0.0.1:8849/index.html"
echo "健康检查：http://127.0.0.1:8848/nacos/v1/console/health/readiness"
echo "查看日志：docker logs -f simple-mall-nacos"
```

### 7.4 macOS / Linux：停止脚本

创建：

```text
scripts/stop-nacos.sh
```

内容：

```bash
#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/deploy/docker-compose.nacos.yml"

docker compose -f "$COMPOSE_FILE" down

echo "Nacos 已停止。"
```

### 7.5 给 Shell 脚本执行权限

macOS / Linux 执行：

```bash
chmod +x scripts/start-nacos.sh
chmod +x scripts/stop-nacos.sh
```

---

## 8. 再次核对两个服务的配置

### 8.1 用户服务配置

检查：

```text
mall-user-service/src/main/resources/application.yml
```

应该是：

```yaml
server:
  port: 8101

spring:
  application:
    name: user-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}

management:
  endpoints:
    web:
      exposure:
        include: health,info
  endpoint:
    health:
      show-details: always
```

重点是：

```yaml
name: user-service
server-addr: 127.0.0.1:8848
```

### 8.2 Gateway 配置

检查：

```text
mall-gateway/src/main/resources/application.yml
```

应该是：

```yaml
server:
  port: 8080

spring:
  application:
    name: gateway-service
  cloud:
    nacos:
      discovery:
        server-addr: ${NACOS_SERVER_ADDR:127.0.0.1:8848}
    gateway:
      server:
        webflux:
          routes:
            - id: user-service-route
              uri: lb://user-service
              predicates:
                - Path=/api/users/**

management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
  endpoint:
    health:
      show-details: always
```

重点检查三处名称是否完全一致：

```text
用户服务名称：user-service
Gateway 路由：lb://user-service
请求路径：/api/users/**
```

任何一个字符不同都可能导致路由失败。

---

## 9. 启动 Nacos

### 9.1 Windows PowerShell

在项目根目录执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-nacos.ps1
```

也可以不使用脚本，直接执行：

```powershell
docker compose -f .\deploy\docker-compose.nacos.yml up -d
```

### 9.2 macOS / Linux

```bash
./scripts/start-nacos.sh
```

或：

```bash
docker compose -f deploy/docker-compose.nacos.yml up -d
```

### 9.3 查看容器状态

```bash
docker ps
```

应该看到：

```text
simple-mall-nacos
```

也可以执行：

```bash
docker compose -f deploy/docker-compose.nacos.yml ps
```

### 9.4 查看启动日志

```bash
docker logs -f simple-mall-nacos
```

`-f` 表示持续跟踪日志。

日志稳定后按：

```text
Ctrl + C
```

这里只会退出日志查看，不会停止容器。

---

## 10. 检查 Nacos 是否就绪

### 10.1 浏览器健康检查

打开：

```text
http://127.0.0.1:8848/nacos/v1/console/health/readiness
```

预期返回：

```text
UP
```

### 10.2 PowerShell 检查

```powershell
Invoke-RestMethod -Uri "http://127.0.0.1:8848/nacos/v1/console/health/readiness"
```

### 10.3 curl 检查

```bash
curl --fail http://127.0.0.1:8848/nacos/v1/console/health/readiness
```

### 10.4 打开 Nacos 控制台

浏览器访问：

```text
http://127.0.0.1:8849/index.html
```

当前本地配置关闭鉴权，因此通常不要求登录。

如果控制台暂时打不开，但容器刚启动，先看日志，不要立刻修改十项配置。

---

## 11. 启动用户服务

### 11.1 推荐方式：IDEA

运行：

```text
mall-user-service
→ com.mall.user.UserServiceApplication
```

观察日志，不要只看最后一行。重点查找：

- Web Server 启动端口为 `8101`；
- 应用名称为 `user-service`；
- 没有持续连接 Nacos 失败；
- Spring Boot 启动完成。

### 11.2 命令行方式

先在根目录安装父工程和公共模块：

```bash
mvn clean install -DskipTests
```

再执行：

```bash
mvn -f mall-user-service/pom.xml spring-boot:run
```

命令行窗口需要保持运行。

### 11.3 验证用户服务直连

浏览器或 Postman 请求：

```http
GET http://127.0.0.1:8101/api/users/ping
```

预期：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "user-service is running"
}
```

再检查 Actuator：

```http
GET http://127.0.0.1:8101/actuator/health
```

预期状态为：

```json
{
  "status": "UP"
}
```

实际响应可能包含更多健康详情。

---

## 12. 在 Nacos 中确认用户服务注册

进入 Nacos 控制台后找到服务列表，应该看到：

```text
user-service
```

检查：

```text
实例数 = 1
健康实例数 = 1
端口 = 8101
```

### 如果接口能访问，但 Nacos 中没有服务

说明用户服务本身启动成功，但服务注册没有成功。

优先检查：

1. 用户服务是否包含 Nacos Discovery 依赖；
2. `spring.application.name` 是否存在；
3. `server-addr` 是否为 `127.0.0.1:8848`；
4. Nacos readiness 是否为 `UP`；
5. 用户服务日志中第一处 Nacos 异常。

---

## 13. 启动 Gateway

必须先保证：

```text
Nacos 已就绪
user-service 已注册且健康
```

然后运行：

```text
mall-gateway
→ com.mall.gateway.GatewayApplication
```

Gateway 启动后检查：

```http
GET http://127.0.0.1:8080/actuator/health
```

预期：

```json
{
  "status": "UP"
}
```

再回到 Nacos 服务列表，应该新增：

```text
gateway-service
```

检查：

```text
实例数 = 1
健康实例数 = 1
端口 = 8080
```

---

## 14. 验证 Gateway 路由转发

请求：

```http
GET http://127.0.0.1:8080/api/users/ping
```

预期得到和用户服务直连相同的响应：

```json
{
  "code": 0,
  "message": "操作成功",
  "data": "user-service is running"
}
```

### 两个请求的区别

用户服务直连：

```text
http://127.0.0.1:8101/api/users/ping
```

请求直接到用户服务。

网关访问：

```text
http://127.0.0.1:8080/api/users/ping
```

请求流程是：

```text
客户端
→ Gateway 的 Path 断言匹配
→ Gateway 识别 lb://user-service
→ 从 Nacos 获取健康实例
→ LoadBalancer 选择实例
→ 转发到 8101
→ 用户服务返回 Result
→ Gateway 把响应返回客户端
```

当这一步成功，你已经真正跑通了最小的微服务注册、发现和路由链路。

---

## 15. 查看 Gateway 路由信息

由于 Gateway 已暴露 `gateway` Actuator Endpoint，可以尝试访问：

```http
GET http://127.0.0.1:8080/actuator/gateway/routes
```

你应该能在返回结果中看到：

```text
user-service-route
lb://user-service
/api/users/**
```

如果 Endpoint 未返回路由详情，先检查：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,gateway
```

Actuator 只用于本地学习和排查。生产环境不能随意把敏感管理接口全部暴露到公网。

---

## 16. 创建一键验证脚本

在手动验证全部成功后，再创建脚本。不要用脚本代替理解过程。

### 16.1 Windows 验证脚本

创建：

```text
scripts/verify-step-02.ps1
```

内容：

```powershell
$ErrorActionPreference = "Stop"

function Test-Endpoint {
    param(
        [string]$Name,
        [string]$Url
    )

    Write-Host "[$Name] GET $Url"
    $response = Invoke-RestMethod -Uri $Url -Method Get
    $response | ConvertTo-Json -Depth 10
    Write-Host ""
}

Test-Endpoint `
    -Name "Nacos readiness" `
    -Url "http://127.0.0.1:8848/nacos/v1/console/health/readiness"

Test-Endpoint `
    -Name "User service direct" `
    -Url "http://127.0.0.1:8101/api/users/ping"

Test-Endpoint `
    -Name "Gateway route" `
    -Url "http://127.0.0.1:8080/api/users/ping"

Write-Host "Step 02 验证通过。"
```

执行：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\verify-step-02.ps1
```

### 16.2 macOS / Linux 验证脚本

创建：

```text
scripts/verify-step-02.sh
```

内容：

```bash
#!/usr/bin/env bash
set -euo pipefail

check_url() {
  local name="$1"
  local url="$2"

  echo "[$name] GET $url"
  curl --fail --silent --show-error "$url"
  echo
  echo
}

check_url \
  "Nacos readiness" \
  "http://127.0.0.1:8848/nacos/v1/console/health/readiness"

check_url \
  "User service direct" \
  "http://127.0.0.1:8101/api/users/ping"

check_url \
  "Gateway route" \
  "http://127.0.0.1:8080/api/users/ping"

echo "Step 02 验证通过。"
```

赋予权限：

```bash
chmod +x scripts/verify-step-02.sh
```

执行：

```bash
./scripts/verify-step-02.sh
```

---

## 17. 正确的日常启动顺序

以后启动当前项目时按以下顺序：

```text
1. 启动 Docker Desktop / Docker Engine
2. 启动 Nacos
3. 确认 Nacos readiness = UP
4. 启动 UserServiceApplication
5. 确认 user-service 已注册并健康
6. 启动 GatewayApplication
7. 确认 gateway-service 已注册并健康
8. 验证用户服务直连
9. 验证 Gateway 路由
```

不要在基础设施没准备好时，同时启动全部程序再混在一起排错。

---

## 18. 常见问题和排查顺序

### 18.1 Docker 命令不存在

现象：

```text
docker: command not found
```

处理：

- 安装 Docker Desktop 或 Docker Engine；
- Windows 确认 Docker Desktop 已启动；
- 重新打开终端；
- 再执行 `docker --version`。

### 18.2 无法连接 Docker daemon

现象可能是：

```text
Cannot connect to the Docker daemon
```

说明 Docker 命令存在，但 Docker 服务没有运行。

先启动 Docker Desktop，再重试。

### 18.3 Nacos 镜像拉取失败

先单独执行：

```bash
docker pull nacos/nacos-server:v3.2.3
```

判断是：

- 网络问题；
- 镜像仓库问题；
- Docker 代理问题；
- 架构问题。

不要因为拉取失败就修改 Java 项目。

### 18.4 Nacos 容器反复退出

查看：

```bash
docker ps -a
docker logs simple-mall-nacos
```

重点找第一处：

```text
ERROR
Exception
Caused by
```

常见原因：

- 安全参数缺失；
- Docker 内存不足；
- 数据卷中有旧版本不兼容数据；
- 端口占用。

若确认不需要旧数据，可以清理数据卷后重建：

```bash
docker compose -f deploy/docker-compose.nacos.yml down -v
docker compose -f deploy/docker-compose.nacos.yml up -d
```

`-v` 会删除 Nacos 本地数据，只适用于当前学习环境。

### 18.5 端口被占用

Windows：

```powershell
netstat -ano | findstr :8848
netstat -ano | findstr :8849
netstat -ano | findstr :9848
netstat -ano | findstr :8080
netstat -ano | findstr :8101
```

macOS / Linux：

```bash
lsof -i :8848
lsof -i :8849
lsof -i :9848
lsof -i :8080
lsof -i :8101
```

本教程端口已经规划好，不要一出现问题就随意改端口。先确认究竟是哪个进程占用。

### 18.6 用户服务启动失败：无法连接 Nacos

按顺序检查：

1. `docker ps` 是否能看到 Nacos；
2. readiness 是否返回 `UP`；
3. `application.yml` 是否使用 `127.0.0.1:8848`；
4. YAML 缩进是否正确；
5. Nacos gRPC 端口 `9848` 是否映射；
6. 是否存在防火墙或代理干扰；
7. 查看第一处 Nacos 客户端异常。

### 18.7 用户服务直连正常，但 Nacos 没有实例

优先检查：

```xml
spring-cloud-starter-alibaba-nacos-discovery
```

以及：

```yaml
spring:
  application:
    name: user-service
```

如果应用名称为空，服务注册名称会有问题。

### 18.8 Gateway 启动成功，但请求返回 503

503 通常表示 Gateway 没找到可用服务实例。

检查：

1. Nacos 中是否存在 `user-service`；
2. 健康实例数是否为 1；
3. Gateway 是否有 LoadBalancer 依赖；
4. 路由是否写成 `lb://user-service`；
5. 大小写和连字符是否完全一致；
6. 用户服务是否在请求过程中退出。

### 18.9 Gateway 返回 404

404 更可能表示路由没有匹配。

检查请求是否为：

```text
/api/users/ping
```

并检查：

```yaml
predicates:
  - Path=/api/users/**
```

还要确认 Gateway 配置使用的是：

```yaml
spring:
  cloud:
    gateway:
      server:
        webflux:
          routes:
```

### 18.10 Gateway 出现 MVC/WebFlux 冲突

检查 `mall-gateway/pom.xml`，不要加入：

```xml
spring-boot-starter-web
```

Gateway 使用：

```xml
spring-cloud-starter-gateway-server-webflux
```

普通业务服务才使用 `spring-boot-starter-web`。

### 18.11 PowerShell 禁止执行脚本

可以仅对当前命令放开：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\start-nacos.ps1
```

不需要永久降低整个系统的执行策略。

### 18.12 `localhost` 与 `127.0.0.1`

本教程统一使用：

```text
127.0.0.1
```

这是为了减少本机 DNS、IPv6 或 hosts 配置带来的差异。

---

## 19. 停止项目

### 19.1 停止 Java 服务

在 IDEA 中依次停止：

```text
GatewayApplication
UserServiceApplication
```

### 19.2 停止 Nacos

Windows：

```powershell
powershell -ExecutionPolicy Bypass -File .\scripts\stop-nacos.ps1
```

macOS / Linux：

```bash
./scripts/stop-nacos.sh
```

或直接执行：

```bash
docker compose -f deploy/docker-compose.nacos.yml down
```

### 19.3 保留还是删除数据卷

普通停止：

```bash
docker compose -f deploy/docker-compose.nacos.yml down
```

会保留数据卷。

彻底重置：

```bash
docker compose -f deploy/docker-compose.nacos.yml down -v
```

会删除 Nacos 数据和日志卷。只在你明确需要重置时使用。

---

## 20. 提交 Git

确认三项验证都成功后：

```text
Nacos readiness = UP
用户服务直连成功
Gateway 路由成功
```

执行：

```bash
git add .
git commit -m "feat: add nacos discovery and gateway routing"
```

---

## 21. Step 02 验收清单

- [ ] Docker 命令可用；
- [ ] Nacos 容器正常运行；
- [ ] Nacos readiness 返回 `UP`；
- [ ] Nacos 控制台可以打开；
- [ ] Gateway 已添加 LoadBalancer 依赖；
- [ ] 用户服务直连接口成功；
- [ ] Nacos 中存在健康的 `user-service`；
- [ ] Nacos 中存在健康的 `gateway-service`；
- [ ] Gateway `/api/users/ping` 转发成功；
- [ ] 能解释 `lb://user-service` 的含义；
- [ ] 能区分 404 和 503 的常见原因；
- [ ] 已停止并重新启动一次完整环境；
- [ ] 已提交 Git。

---

## 22. 下一步预告

Step 03 开始真正的用户业务，但仍然只做一个服务内部的闭环：

```text
安装 MySQL
→ 创建 user_db
→ 设计 user_account 表
→ 引入 MyBatis-Plus
→ 建立 Entity、Mapper、Service
→ 实现用户注册
→ 参数校验
→ 全局异常处理
→ Postman 测试
```

这时才开始建第一张业务表。

---

## 23. Step 02 完成后的项目目录

```text
simple-mall-microservices/
├── .gitignore
├── README.md
├── pom.xml
├── deploy/
│   └── docker-compose.nacos.yml
├── docs/
│   ├── step-01-project-scaffold.md
│   └── step-02-nacos-and-gateway.md
├── scripts/
│   ├── start-nacos.ps1
│   ├── start-nacos.sh
│   ├── stop-nacos.ps1
│   ├── stop-nacos.sh
│   ├── verify-step-02.ps1
│   └── verify-step-02.sh
├── mall-common/
│   ├── pom.xml
│   └── src/
│       └── main/
│           └── java/
│               └── com/
│                   └── mall/
│                       └── common/
│                           ├── api/
│                           │   ├── CommonErrorCode.java
│                           │   ├── ErrorCode.java
│                           │   └── Result.java
│                           └── exception/
│                               └── BusinessException.java
├── mall-gateway/
│   ├── pom.xml
│   └── src/
│       └── main/
│           ├── java/
│           │   └── com/
│           │       └── mall/
│           │           └── gateway/
│           │               └── GatewayApplication.java
│           └── resources/
│               └── application.yml
└── mall-user-service/
    ├── pom.xml
    └── src/
        └── main/
            ├── java/
            │   └── com/
            │       └── mall/
            │           └── user/
            │               ├── UserServiceApplication.java
            │               └── controller/
            │                   └── UserHealthController.java
            └── resources/
                └── application.yml
```

## 24. Step 02 文件总览

| 文件 | 本步状态 | 作用 |
|---|---|---|
| `deploy/docker-compose.nacos.yml` | 新增 | 定义本地单机 Nacos 容器、端口、内存和数据卷 |
| `docs/step-02-nacos-and-gateway.md` | 新增 | Step 02 完整搭建与排错教程 |
| `scripts/start-nacos.ps1` | 新增 | Windows 启动 Nacos |
| `scripts/start-nacos.sh` | 新增 | macOS/Linux 启动 Nacos |
| `scripts/stop-nacos.ps1` | 新增 | Windows 停止 Nacos |
| `scripts/stop-nacos.sh` | 新增 | macOS/Linux 停止 Nacos |
| `scripts/verify-step-02.ps1` | 新增 | Windows 依次验证 Nacos、用户服务和 Gateway |
| `scripts/verify-step-02.sh` | 新增 | macOS/Linux 依次验证三个 Endpoint |
| `mall-gateway/pom.xml` | 修改 | 新增 Spring Cloud LoadBalancer 依赖 |
| `mall-gateway/application.yml` | 核对 | 配置 Nacos 和 `lb://user-service` 路由 |
| `mall-user-service/application.yml` | 核对 | 配置 `user-service` 名称和 Nacos 地址 |
| `GatewayApplication.java` | 使用 | 启动并注册 Gateway 服务 |
| `UserServiceApplication.java` | 使用 | 启动并注册用户服务 |
| `UserHealthController.java` | 使用 | 提供直连与路由验证接口 |
