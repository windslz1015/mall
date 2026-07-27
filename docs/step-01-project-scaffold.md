# Step 01：Project Scaffold（Maven 微服务项目骨架）

> 文件名：`step-01-project-scaffold.md`  
> 项目：简易多商家电商微服务练习项目  
> 本步原则：**不下载成品项目，所有目录、POM 和 Java 文件都由你自己创建。**

---

## 1. 本步要完成什么

本步只搭建一个最小的 Maven 多模块微服务骨架，最终包含：

- 一个 Maven 父工程；
- 一个公共模块 `mall-common`；
- 一个统一入口 `mall-gateway`；
- 一个用户服务空壳 `mall-user-service`；
- 统一的接口返回结构；
- 一个用于后续验证的 `/api/users/ping` 接口；
- Spring Boot、Spring Cloud、Spring Cloud Alibaba 的统一版本管理。

本步结束后，你应该能在项目根目录执行：

```bash
mvn clean package -DskipTests
```

并看到：

```text
BUILD SUCCESS
```

### 本步暂时不做

- 不连接 MySQL；
- 不连接 Redis；
- 不引入 RabbitMQ；
- 不写用户注册和登录；
- 不创建数据库表；
- 不启动 Nacos；
- 不要求启动 Gateway 和用户服务。

先把项目骨架搭正确，再逐步增加业务和基础设施。

---

## 2. 本教程固定使用的版本

为了和后续步骤保持一致，本教程固定采用以下版本，不代表你必须永远使用这些版本：

| 技术 | 固定版本 |
|---|---:|
| JDK | 17 |
| Spring Boot | 3.5.13 |
| Spring Cloud | 2025.0.3 |
| Spring Cloud Alibaba | 2025.0.0.0 |
| Maven | 3.9.x |

### 为什么使用 JDK 17

你的简历主要是 Spring Boot、MyBatis、Spring Cloud Alibaba 技术路线。JDK 17 是当前 Java 后端项目中较常见的长期支持版本，也适合练习 Spring Boot 3.x。

### 版本使用规则

后续新增 Spring Cloud 组件时：

- 不要给每一个 Spring Cloud 依赖单独写版本；
- 统一由 Spring Cloud BOM 管理；
- Spring Cloud Alibaba 组件统一由 Spring Cloud Alibaba BOM 管理；
- 普通第三方组件需要时再单独管理版本。

---

## 3. 开始前检查本机环境

### 3.1 检查 JDK

在终端执行：

```bash
java -version
```

你需要看到 Java 17，例如：

```text
java version "17..."
```

再检查编译器：

```bash
javac -version
```

### 3.2 检查 Maven

```bash
mvn -version
```

重点确认：

- Maven 版本为 3.9.x；
- Maven 使用的 Java 版本为 17；
- Maven 没有使用另一套旧 JDK。

### 3.3 IDEA 设置

打开 IDEA 后检查：

```text
File
→ Project Structure
→ Project
→ SDK = 17
→ Language level = 17
```

再检查 Maven 使用的 JDK：

```text
Settings
→ Build, Execution, Deployment
→ Build Tools
→ Maven
→ Runner
→ JRE = Project JDK 17
```

### 3.4 检查 Git

```bash
git --version
```

Git 不是运行项目的必要条件，但建议每完成一步就提交一次，方便回退。

---

## 4. 创建项目根目录

你可以在任意工作目录创建项目：

```bash
mkdir simple-mall-microservices
cd simple-mall-microservices
```

Windows PowerShell 也可以执行相同命令。

先创建基础目录：

```bash
mkdir docs
mkdir mall-common
mkdir mall-gateway
mkdir mall-user-service
```

此时目录应为：

```text
simple-mall-microservices/
├── docs/
├── mall-common/
├── mall-gateway/
└── mall-user-service/
```

把当前这份文档保存到：

```text
docs/step-01-project-scaffold.md
```

---

## 5. 创建根项目 `pom.xml`

在项目根目录创建：

```text
pom.xml
```

内容如下。建议你手动输入，并理解每一部分的作用。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.13</version>
        <relativePath/>
    </parent>

    <groupId>com.mall</groupId>
    <artifactId>simple-mall-microservices</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <name>simple-mall-microservices</name>
    <description>简易多商家电商微服务练习项目</description>

    <modules>
        <module>mall-common</module>
        <module>mall-gateway</module>
        <module>mall-user-service</module>
    </modules>

    <properties>
        <java.version>17</java.version>
        <spring-cloud.version>2025.0.3</spring-cloud.version>
        <spring-cloud-alibaba.version>2025.0.0.0</spring-cloud-alibaba.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <project.reporting.outputEncoding>UTF-8</project.reporting.outputEncoding>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>

            <dependency>
                <groupId>com.alibaba.cloud</groupId>
                <artifactId>spring-cloud-alibaba-dependencies</artifactId>
                <version>${spring-cloud-alibaba.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <build>
        <pluginManagement>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                </plugin>

                <plugin>
                    <groupId>org.apache.maven.plugins</groupId>
                    <artifactId>maven-compiler-plugin</artifactId>
                    <configuration>
                        <release>${java.version}</release>
                        <parameters>true</parameters>
                    </configuration>
                </plugin>
            </plugins>
        </pluginManagement>
    </build>
</project>
```

### 5.1 这份父 POM 在做什么

#### `packaging=pom`

父工程本身不生成业务 Jar，它只负责：

- 聚合子模块；
- 统一依赖版本；
- 统一构建插件；
- 统一项目坐标。

#### `<modules>`

声明当前 Maven Reactor 中的三个模块。新增服务时，需要继续在这里加入模块名。

#### `<dependencyManagement>`

它只负责管理版本，不会自动把依赖加进子模块。

例如，父工程导入 Nacos 的版本管理后，用户服务仍然需要在自己的 POM 中显式声明：

```xml
<dependency>
    <groupId>com.alibaba.cloud</groupId>
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
</dependency>
```

#### `<pluginManagement>`

统一插件配置。子服务需要真正使用 Spring Boot Maven 插件时，仍要在自己的 `<plugins>` 中声明。

---

## 6. 创建 `.gitignore`

在项目根目录创建：

```text
.gitignore
```

内容：

```gitignore
# Maven
**/target/

# IntelliJ IDEA
.idea/
*.iml
*.ipr
*.iws

# VS Code
.vscode/

# Eclipse
.classpath
.project
.settings/

# Logs
*.log
logs/

# Operating systems
.DS_Store
Thumbs.db

# Local environment files
.env
*.local
```

这可以避免把编译结果、IDE 配置和本机私有配置提交到 Git。

---

## 7. 创建根目录 `README.md`

在根目录创建：

```text
README.md
```

先写最简单的内容：

```markdown
# Simple Mall Microservices

简易多商家电商微服务练习项目。

## 当前模块

- mall-common：公共基础模块
- mall-gateway：统一网关
- mall-user-service：用户服务

## 当前进度

- [x] Step 01：Maven 多模块项目骨架
- [ ] Step 02：Nacos 注册发现与 Gateway 路由
- [ ] Step 03：用户表、MySQL 与 MyBatis-Plus
```

README 只记录项目整体状态，详细操作放在 `docs` 目录。

---

## 8. 创建公共模块 `mall-common`

公共模块只存放真正跨服务通用、并且稳定的基础代码。

当前只放：

- 统一响应结构；
- 通用错误码；
- 业务异常。

不要把用户 Entity、商品 Entity、订单 Entity 放进公共模块。否则服务之间会重新产生强耦合。

### 8.1 创建目录

```text
mall-common/
└── src/
    └── main/
        └── java/
            └── com/
                └── mall/
                    └── common/
                        ├── api/
                        └── exception/
```

### 8.2 创建 `mall-common/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.mall</groupId>
        <artifactId>simple-mall-microservices</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>mall-common</artifactId>
    <packaging>jar</packaging>

    <name>mall-common</name>
    <description>公共返回结构和基础异常</description>
</project>
```

当前公共模块不需要任何第三方依赖。

### 8.3 创建错误码接口 `ErrorCode.java`

文件位置：

```text
mall-common/src/main/java/com/mall/common/api/ErrorCode.java
```

内容：

```java
package com.mall.common.api;

/**
 * 业务错误码接口。
 */
public interface ErrorCode {

    int getCode();

    String getMessage();
}
```

后面每个服务可以定义自己的错误码枚举，并实现这个接口。

### 8.4 创建通用错误码 `CommonErrorCode.java`

文件位置：

```text
mall-common/src/main/java/com/mall/common/api/CommonErrorCode.java
```

内容：

```java
package com.mall.common.api;

/**
 * 通用错误码。
 */
public enum CommonErrorCode implements ErrorCode {

    SUCCESS(0, "操作成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未登录或登录已过期"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "请求资源不存在"),
    INTERNAL_ERROR(500, "系统内部错误");

    private final int code;
    private final String message;

    CommonErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    @Override
    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

当前错误码数字和 HTTP 状态码相似，是为了便于理解。后面可以再演进为模块化业务错误码，例如：

```text
100001 用户不存在
100002 用户名已存在
200001 商品不存在
500001 库存不足
```

### 8.5 创建统一返回结构 `Result.java`

文件位置：

```text
mall-common/src/main/java/com/mall/common/api/Result.java
```

内容：

```java
package com.mall.common.api;

/**
 * 统一接口返回结构。
 *
 * @param <T> 返回数据类型
 */
public final class Result<T> {

    private final int code;
    private final String message;
    private final T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(
                CommonErrorCode.SUCCESS.getCode(),
                CommonErrorCode.SUCCESS.getMessage(),
                data
        );
    }

    public static Result<Void> success() {
        return success(null);
    }

    public static <T> Result<T> failure(ErrorCode errorCode) {
        return new Result<>(
                errorCode.getCode(),
                errorCode.getMessage(),
                null
        );
    }

    public static <T> Result<T> failure(int code, String message) {
        return new Result<>(code, message, null);
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }
}
```

这里暂时不引入 Lombok。你可以直接看到构造方法、泛型、静态工厂方法和 Getter 是怎么工作的。

### 8.6 创建业务异常 `BusinessException.java`

文件位置：

```text
mall-common/src/main/java/com/mall/common/exception/BusinessException.java
```

内容：

```java
package com.mall.common.exception;

import com.mall.common.api.ErrorCode;

/**
 * 可预期的业务异常。
 */
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
```

后面会通过全局异常处理器，把业务异常转换成统一的 `Result`。

---

## 9. 创建用户服务 `mall-user-service`

当前用户服务只完成三件事：

- 作为独立 Spring Boot 服务启动；
- 准备注册到 Nacos；
- 提供一个 Ping 接口。

### 9.1 创建目录

```text
mall-user-service/
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── mall/
        │           └── user/
        │               ├── controller/
        │               └── UserServiceApplication.java
        └── resources/
            └── application.yml
```

### 9.2 创建 `mall-user-service/pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.mall</groupId>
        <artifactId>simple-mall-microservices</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>mall-user-service</artifactId>
    <packaging>jar</packaging>

    <name>mall-user-service</name>
    <description>用户微服务</description>

    <dependencies>
        <dependency>
            <groupId>com.mall</groupId>
            <artifactId>mall-common</artifactId>
            <version>${project.version}</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 9.3 理解用户服务依赖

| 依赖 | 作用 |
|---|---|
| `mall-common` | 使用统一返回结构和公共异常 |
| `spring-boot-starter-web` | Spring MVC、REST 接口、内嵌 Tomcat |
| `spring-boot-starter-validation` | 请求参数校验 |
| `spring-boot-starter-actuator` | 健康检查和运行状态 |
| `nacos-discovery` | 注册服务并发现其他服务 |
| `spring-boot-starter-test` | 单元测试与 Spring 测试 |

### 9.4 创建启动类

文件位置：

```text
mall-user-service/src/main/java/com/mall/user/UserServiceApplication.java
```

内容：

```java
package com.mall.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
```

`@EnableDiscoveryClient` 用于明确表达这是一个服务发现客户端。现代 Spring Cloud 在依赖存在时通常也能自动启用服务发现，但练习阶段保留这个注解更直观。

### 9.5 创建 Ping 接口

文件位置：

```text
mall-user-service/src/main/java/com/mall/user/controller/UserHealthController.java
```

内容：

```java
package com.mall.user.controller;

import com.mall.common.api.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserHealthController {

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("user-service is running");
    }
}
```

这个接口只是为了验证：

- Spring Boot 是否启动；
- `mall-common` 是否能被引用；
- JSON 是否能正常返回；
- 下一步 Gateway 是否能转发请求。

它不是最终业务接口，后续可以保留，也可以改成 Actuator 健康检查。

### 9.6 创建配置文件

文件位置：

```text
mall-user-service/src/main/resources/application.yml
```

内容：

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

#### 配置解释

```yaml
server.port: 8101
```

用户服务直连端口。

```yaml
spring.application.name: user-service
```

这是注册到 Nacos 的服务名，也是 Gateway 使用 `lb://user-service` 找到它的关键。

```yaml
${NACOS_SERVER_ADDR:127.0.0.1:8848}
```

表示：

- 优先读取环境变量 `NACOS_SERVER_ADDR`；
- 没有环境变量时，使用 `127.0.0.1:8848`。

开发环境可以用默认值，未来部署时可以通过环境变量修改。

---

## 10. 创建网关服务 `mall-gateway`

Gateway 是系统统一入口。当前只配置一条路由：

```text
/api/users/** → user-service
```

### 10.1 创建目录

```text
mall-gateway/
└── src/
    └── main/
        ├── java/
        │   └── com/
        │       └── mall/
        │           └── gateway/
        │               └── GatewayApplication.java
        └── resources/
            └── application.yml
```

### 10.2 创建 `mall-gateway/pom.xml`

Step 01 先创建基础版本。Step 02 会补充 LoadBalancer 依赖。

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.mall</groupId>
        <artifactId>simple-mall-microservices</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>mall-gateway</artifactId>
    <packaging>jar</packaging>

    <name>mall-gateway</name>
    <description>微服务统一网关</description>

    <dependencies>
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-gateway-server-webflux</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>

        <dependency>
            <groupId>com.alibaba.cloud</groupId>
            <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### 10.3 为什么 Gateway 不使用 `spring-boot-starter-web`

当前使用的是 Gateway Server WebFlux：

```text
spring-cloud-starter-gateway-server-webflux
```

它基于响应式 Web 栈。不要再给 Gateway 添加 `spring-boot-starter-web`，否则可能产生 WebMVC 和 WebFlux 的应用类型冲突。

业务服务仍然可以继续使用普通的 Spring MVC。

### 10.4 创建启动类

文件位置：

```text
mall-gateway/src/main/java/com/mall/gateway/GatewayApplication.java
```

内容：

```java
package com.mall.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
```

### 10.5 创建 Gateway 配置

文件位置：

```text
mall-gateway/src/main/resources/application.yml
```

内容：

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

### 10.6 理解路由配置

```yaml
id: user-service-route
```

路由的唯一标识，方便查看和排查。

```yaml
uri: lb://user-service
```

含义：

- `lb` 表示通过负载均衡选择服务实例；
- `user-service` 必须与用户服务的 `spring.application.name` 完全一致；
- Gateway 会通过服务发现组件获得用户服务实例地址。

```yaml
Path=/api/users/**
```

所有 `/api/users/` 开头的请求都会命中这条路由。

例如：

```text
GET http://127.0.0.1:8080/api/users/ping
```

后续将转发为类似：

```text
GET http://某个-user-service-实例/api/users/ping
```

当前配置没有使用 `StripPrefix`，所以路径保持不变。

---

## 11. 用 IDEA 导入工程

推荐直接打开项目根目录，而不是分别打开三个模块。

操作：

```text
IDEA
→ Open
→ 选择 simple-mall-microservices 根目录
→ Trust Project
→ 等待 Maven 同步
```

在 IDEA 的 Maven 面板中应该看到：

```text
simple-mall-microservices
├── mall-common
├── mall-gateway
└── mall-user-service
```

如果子模块显示成普通文件夹：

1. 右键根目录 `pom.xml`；
2. 选择 `Add as Maven Project`；
3. 点击 Maven 面板的 Reload。

---

## 12. 编译整个工程

在根目录执行：

```bash
mvn clean package -DskipTests
```

Maven 会按依赖顺序构建：

```text
simple-mall-microservices
mall-common
mall-gateway
mall-user-service
```

成功时应看到类似：

```text
Reactor Summary

simple-mall-microservices ........ SUCCESS
mall-common ...................... SUCCESS
mall-gateway ..................... SUCCESS
mall-user-service ................ SUCCESS

BUILD SUCCESS
```

### 12.1 为什么现在只编译，不启动

两个服务已经配置连接：

```text
127.0.0.1:8848
```

但 Nacos 要在 Step 02 才启动。为了让每一步的目标单一，本步只验证 Maven 工程、依赖和 Java 编译。

---

## 13. 常见问题

### 13.1 `Non-resolvable parent POM`

先确认：

- 能访问 Maven Central；
- Maven `settings.xml` 没有配置失效镜像；
- IDEA 和命令行使用的是同一个 Maven 配置。

可以执行：

```bash
mvn -U clean package -DskipTests
```

`-U` 会要求 Maven 检查更新。

### 13.2 `invalid target release: 17`

说明 Maven 实际使用的 JDK 低于 17。

执行：

```bash
mvn -version
```

检查输出中的 Java version，而不只是 `java -version`。

### 13.3 `mall-common` 找不到

检查：

- 根 POM 的 `<modules>` 是否包含 `mall-common`；
- 用户服务依赖的版本是否为 `${project.version}`；
- 父子项目的 `groupId`、`artifactId`、`version` 是否完全一致；
- 是否在根目录执行 Maven 命令。

### 13.4 YAML 报错

YAML 使用空格缩进，不能使用 Tab。

推荐每一层缩进两个空格。

### 13.5 Gateway 依赖名写错

本教程使用新名称：

```text
spring-cloud-starter-gateway-server-webflux
```

不要随意换成旧的 Starter 名称。

---

## 14. 初始化 Git

确认项目能够构建后执行：

```bash
git init
git add .
git commit -m "chore: initialize microservice project scaffold"
```

以后每完成一个 Step 提交一次。

建议提交内容保持单一，例如：

```text
Step 01：只提交工程骨架
Step 02：只提交 Nacos 和 Gateway 路由
Step 03：只提交用户数据层和注册接口
```

---

## 15. Step 01 验收清单

逐项确认：

- [ ] 本机 JDK 是 17；
- [ ] Maven 使用的也是 JDK 17；
- [ ] 根项目 packaging 是 `pom`；
- [ ] 根 POM 聚合了三个模块；
- [ ] `mall-common` 可以独立编译；
- [ ] 用户服务能引用 `Result`；
- [ ] Gateway 使用 WebFlux Starter；
- [ ] 两个服务的 `spring.application.name` 已配置；
- [ ] `mvn clean package -DskipTests` 成功；
- [ ] 已完成一次 Git 提交。

满足以上条件后，再进入 Step 02。

---

## 16. 下一步预告

Step 02 只完成基础微服务通信：

```text
启动 Nacos
→ 用户服务注册
→ Gateway 注册
→ Gateway 从 Nacos 发现用户服务
→ 访问 Gateway
→ 请求被转发到用户服务
```

仍然不会创建数据库表。

---

## 17. Step 01 完成后的项目目录

```text
simple-mall-microservices/
├── .gitignore
├── README.md
├── pom.xml
├── docs/
│   └── step-01-project-scaffold.md
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

## 18. Step 01 文件总览

| 文件 | 作用 |
|---|---|
| `.gitignore` | 排除编译产物、IDE 配置、日志和本机环境文件 |
| `README.md` | 记录项目介绍、模块和整体进度 |
| `pom.xml` | Maven 父工程、模块聚合、版本统一管理 |
| `docs/step-01-project-scaffold.md` | Step 01 搭建教程 |
| `mall-common/pom.xml` | 公共模块 Maven 配置 |
| `ErrorCode.java` | 所有业务错误码的统一接口 |
| `CommonErrorCode.java` | 通用成功和失败错误码 |
| `Result.java` | REST 接口统一响应结构 |
| `BusinessException.java` | 可预期业务异常 |
| `mall-gateway/pom.xml` | Gateway 依赖和构建配置 |
| `GatewayApplication.java` | Gateway 启动入口 |
| `mall-gateway/application.yml` | 网关端口、Nacos 地址和用户路由 |
| `mall-user-service/pom.xml` | 用户服务依赖和构建配置 |
| `UserServiceApplication.java` | 用户服务启动入口 |
| `UserHealthController.java` | 用户服务 Ping 验证接口 |
| `mall-user-service/application.yml` | 用户服务端口、服务名和 Nacos 地址 |
