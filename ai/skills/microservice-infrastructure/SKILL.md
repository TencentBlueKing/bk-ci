---
name: microservice-infrastructure
description: 配置Kafka消费者、发布RabbitMQ事件消息、实现OpenFeign服务调用、配置Spring Cloud Gateway路由、管理Spring Boot多环境Profile、实现i18n国际化。BK-CI微服务基础设施：条件配置（@Profile/@ConditionalOnProperty）、事件驱动（RabbitMQ/Kafka发布订阅）、服务间通信（Feign客户端/Consul服务发现/熔断降级）、SLF4J日志脱敏。
core_files:
  - "src/backend/ci/core/common/common-event/"
  - "src/backend/ci/core/common/common-client/src/main/kotlin/com/tencent/devops/common/client/Client.kt"
  - "support-files/i18n/"
related_skills:
  - backend-microservice-development
  - common-technical-practices
token_estimate: 5000
---

# 微服务基础设施指南

BK-CI 微服务架构的核心基础设施，基于 Spring Cloud、Spring Boot、OpenFeign、RabbitMQ、Consul 构建。涵盖条件配置、事件驱动、服务间通信、国际化与日志脱敏。

## 使用指南

### 场景 1：配置多环境（开发/测试/生产）

**需求**: 不同环境使用不同配置、特性开关

**工作流**:
1. 创建环境配置文件 `application-{profile}.yml`（如 `application-dev.yml`、`application-prod.yml`）
2. 使用 `@Profile("dev")` 注解标注环境特定的 Bean
3. 使用 `@ConditionalOnProperty(prefix="feature", name="newPipeline", havingValue="true")` 实现特性开关
4. 启动时指定环境：`--spring.profiles.active=dev`
5. 验证：检查启动日志确认 active profile，调用 `/actuator/env` 确认配置值

参考：[reference/1-conditional-config.md](./reference/1-conditional-config.md)

---

### 场景 2：实现事件驱动架构（RabbitMQ）

**需求**: 异步处理、模块解耦、事件溯源

**工作流**:
1. 定义事件类，实现 `IEvent` 接口，命名遵循 `{Module}{Action}Event`（如 `PipelineStartEvent`）
2. 通过 `EventDispatcher` 发布事件到 RabbitMQ
3. 实现 `EventListener` 订阅并处理事件，确保处理逻辑幂等
4. 配置死信队列处理消费失败的消息
5. **验证**：检查 RabbitMQ 管理控制台确认队列绑定正确，发送测试事件确认消费者收到消息，检查死信队列无积压

参考：[reference/2-event-driven.md](./reference/2-event-driven.md)

---

### 场景 3：服务间调用（OpenFeign + Consul）

**需求**: 跨服务通信、负载均衡、熔断降级

**工作流**:
1. 在 `api-*` 模块中定义 Feign 客户端接口（继承 `ServiceXXXResource`）
2. 通过 `Client` 基类获取 Feign 客户端实例，Consul 自动服务发现
3. 配置超时：连接超时 5s，读超时 30s
4. 实现服务降级逻辑，返回默认值或缓存数据
5. **验证**：检查 Consul 服务注册状态，调用接口确认负载均衡生效，模拟下游不可用确认熔断降级触发

**示例 — 定义 Feign 客户端接口**:
```kotlin
@FeignClient(name = "project", contextId = "ServiceProjectResource")
interface ServiceProjectResource {
    @GetMapping("/service/projects/{projectId}")
    fun get(@PathVariable("projectId") projectId: String): Result<ProjectVO>
}

// 调用方通过 Client 基类获取实例
val projectInfo = client.get(ServiceProjectResource::class).get(projectId)
```

参考：[reference/3-service-communication.md](./reference/3-service-communication.md)

---

### 场景 4：实现国际化与 SLF4J 日志规范

**需求**: 多语言支持、统一日志格式、敏感信息脱敏

**工作流**:
1. 在 `support-files/i18n/` 下创建 `messages_{locale}.properties`（如 `messages_zh_CN.properties`、`messages_en_US.properties`）
2. 用户可见文案使用 `MessageUtil.getMessageByLocale()` 获取
3. 日志统一使用 SLF4J（禁止 `println`），级别：ERROR/WARN/INFO/DEBUG
4. 敏感信息（密码、Token）打印时脱敏
5. **验证**：切换 locale 确认文案正确，检查日志输出无明文密码

参考：[reference/4-i18n-logging.md](./reference/4-i18n-logging.md)

---

## 核心类与文件速查

### 条件配置

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `@Profile` | Spring Boot 内置 | 环境配置注解 |
| `@Conditional` | Spring Boot 内置 | 条件化 Bean 加载 |
| `application-*.yml` | `src/main/resources/` | 多环境配置文件 |

### 事件驱动

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `IEvent` | `common-event/pojo/IEvent.kt` | 事件接口 |
| `EventDispatcher` | `common-event/dispatcher/` | 事件分发器 |
| `EventListener` | `common-event/listener/` | 事件监听器 |

### 服务间通信

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `Client` | `common-client/Client.kt` | Feign 客户端基类 |
| `ServiceXXXResource` | `api-*/api/` | 服务接口定义 |

### 国际化与日志

| 目录/文件 | 路径 | 说明 |
|----------|------|------|
| `i18n/` | `support-files/i18n/` | 国际化消息文件 |
| `messages_*.properties` | `support-files/i18n/` | 多语言配置 |

---

## 开发规范

- 环境相关配置使用 `@Profile`，特性开关使用 `@ConditionalOnProperty`，敏感配置加密存储
- 事件类实现 `IEvent`，命名 `{Module}{Action}Event`，通过 `EventDispatcher` 发布，监听器处理幂等
- OpenFeign 接口定义在 `api-*` 模块，设置超时（连接 5s/读 30s），实现熔断降级，禁止循环调用
- 用户可见文案国际化（至少中文+英文），日志用 SLF4J，敏感信息必须脱敏

---

## 相关 Skill

- **前置**: `backend-microservice-development` — Spring Boot/Spring Cloud 基础
- **相关**: `common-technical-practices` — 通用技术实践（AOP、锁、重试）
- **应用**: `process-module-architecture` — Process 模块（事件驱动应用示例）

---

## 常见问题

**事件发布后如何保证消费？** 使用 RabbitMQ 持久化队列，消费失败自动重试，最终失败进入死信队列并告警。

**Feign 调用超时如何处理？** 配置超时（连接 5s/读 30s），实现服务降级返回默认值，幂等操作添加重试，使用熔断器快速失败。

**服务间循环调用如何避免？** 禁止双向依赖（A 调 B，B 不能调 A），用事件驱动解耦，或引入中间服务打破循环。

**敏感信息如何脱敏？**
```kotlin
// Token 脱敏（显示前4后4）
logger.info("Token: {}...{}", token.take(4), token.takeLast(4))
// 手机号脱敏（显示前3后4）
logger.info("Phone: {}****{}", phone.take(3), phone.takeLast(4))
```
