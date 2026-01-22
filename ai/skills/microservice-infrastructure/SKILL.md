---
name: microservice-infrastructure
description: 微服务基础设施指南，涵盖条件配置、事件驱动架构、服务间通信、国际化与日志等微服务架构的核心基础设施。当用户实现服务间调用、配置多环境、实现异步通信、处理国际化或规范日志输出时使用。
core_files:
  - "src/backend/ci/core/common/common-event/"
  - "src/backend/ci/core/common/common-client/src/main/kotlin/com/tencent/devops/common/client/Client.kt"
  - "support-files/i18n/"
related_skills:
  - backend-microservice-development
  - common-technical-practices
token_estimate: 6000
---

# 微服务基础设施指南

## Skill 概述

本 Skill 涵盖了 BK-CI 微服务架构中的 **4 大核心基础设施**，这些是构建分布式系统的基石，与 Spring Cloud/Spring Boot 框架深度集成。

### 核心主题

| 主题 | 说明 | 文档 |
|------|------|------|
| **条件配置** | Profile 配置、特性开关、环境隔离 | [1-conditional-config.md] |
| **事件驱动** | MQ 消息队列、发布订阅、异步处理 | [2-event-driven.md] |
| **服务间通信** | Feign 客户端、服务发现、熔断降级 | [3-service-communication.md] |
| **国际化与日志** | i18n 多语言、日志规范、敏感信息脱敏 | [4-i18n-logging.md] |

---

## 微服务基础设施架构

### 架构视图

```
┌─────────────────────────────────────────────────────────────────┐
│                    BK-CI 微服务集群                              │
│  Process / Project / Store / Auth / Repository / Dispatch...   │
└─────────────────────────────────────────────────────────────────┘
                             ↓
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │ Feign   │         │  MQ     │         │ Config  │
   │ 服务调用 │         │ 事件驱动 │         │ 配置中心 │
   └─────────┘         └─────────┘         └─────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                    微服务基础设施层                              │
├─────────────────────────────────────────────────────────────────┤
│  • 条件配置（多环境隔离）                                        │
│  • 事件驱动（异步解耦）                                          │
│  • 服务间通信（负载均衡、熔断）                                   │
│  • 国际化与日志（可观测性）                                      │
└─────────────────────────────────────────────────────────────────┘
```

---

## 使用指南

### 场景 1：配置多环境（开发/测试/生产）

**需求**: 不同环境使用不同配置、特性开关

**步骤**:
1. 查阅 [reference/1-conditional-config.md](./reference/1-conditional-config.md)
2. 使用 `@Profile` 注解或 `@Conditional`
3. 配置 `application-{profile}.yml`
4. 实现特性开关逻辑

**典型问题**:
- 如何动态切换环境配置？
- 特性开关如何实现？
- 配置优先级如何确定？

---

### 场景 2：实现事件驱动架构

**需求**: 异步处理、模块解耦、事件溯源

**步骤**:
1. 查阅 [reference/2-event-driven.md](./reference/2-event-driven.md)
2. 定义事件类（实现 `IEvent` 接口）
3. 发布事件到 MQ
4. 订阅并处理事件

**典型问题**:
- 如何发布事件？
- 如何订阅消息队列？
- 事件丢失如何处理？

---

### 场景 3：服务间调用

**需求**: 跨服务通信、负载均衡、熔断降级

**步骤**:
1. 查阅 [reference/3-service-communication.md](./reference/3-service-communication.md)
2. 定义 Feign 客户端接口
3. 配置服务发现（Consul）
4. 实现熔断降级逻辑

**典型问题**:
- Feign 客户端如何定义？
- 超时时间如何配置？
- 服务降级如何实现？

---

### 场景 4：实现国际化与日志规范

**需求**: 多语言支持、统一日志格式、敏感信息脱敏

**步骤**:
1. 查阅 [reference/4-i18n-logging.md](./reference/4-i18n-logging.md)
2. 配置 i18n 消息文件（`messages_zh_CN.properties`）
3. 规范日志输出（使用 SLF4J）
4. 实现敏感信息脱敏

**典型问题**:
- 如何添加新语言？
- 日志级别如何设置？
- 密码等敏感信息如何脱敏？

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

### 1. 条件配置规范

- ✅ 环境相关配置使用 `@Profile` 注解
- ✅ 特性开关使用 `@ConditionalOnProperty`
- ✅ 敏感配置（密码、密钥）加密存储
- ✅ 配置文件按环境分离（`application-dev.yml`）

### 2. 事件驱动规范

- ✅ 事件类实现 `IEvent` 接口
- ✅ 事件命名：`{Module}{Action}Event`（如 `PipelineStartEvent`）
- ✅ 事件发布使用 `EventDispatcher`
- ✅ 事件监听器处理要幂等
- ✅ 避免同步等待事件处理结果

### 3. 服务间通信规范

- ✅ Feign 接口定义在 `api-*` 模块
- ✅ 设置合理的超时时间（连接超时 5s，读超时 30s）
- ✅ 实现服务降级（返回默认值或缓存数据）
- ✅ 避免服务间循环调用
- ✅ 关键调用添加链路追踪

### 4. 国际化与日志规范

- ✅ 用户可见文案必须国际化
- ✅ 至少支持中文和英文
- ✅ 日志使用 SLF4J（不使用 `println`）
- ✅ 日志级别：ERROR（错误）、WARN（警告）、INFO（关键流程）、DEBUG（调试）
- ✅ 敏感信息（密码、Token）必须脱敏

---

## 与其他 Skill 的关系

```
microservice-infrastructure (本 Skill)
    ↓ 依赖
backend-microservice-development     # 微服务开发基础
common-technical-practices           # 通用技术实践
    ↓ 被依赖
process-module-architecture          # Process 模块使用这些基础设施
auth-module-architecture             # Auth 模块使用这些基础设施
...                                  # 其他业务模块
```

**前置知识**:
- `backend-microservice-development` - 了解 Spring Boot/Spring Cloud 基础

**相关 Skill**:
- `common-technical-practices` - 通用技术实践（AOP、锁、重试）
- `process-module-architecture` - Process 模块架构（事件驱动应用）

---

## 详细文档导航

| 文档 | 内容 | 行数 | 典型问题 |
|------|------|------|----------|
| [1-conditional-config.md](./reference/1-conditional-config.md) | 条件配置 | 59 | 如何配置多环境？特性开关如何实现？ |
| [2-event-driven.md](./reference/2-event-driven.md) | 事件驱动架构 | 88 | 如何发布事件？事件丢失如何处理？ |
| [3-service-communication.md](./reference/3-service-communication.md) | 服务间通信 | 104 | Feign 如何配置？超时如何处理？ |
| [4-i18n-logging.md](./reference/4-i18n-logging.md) | 国际化与日志 | 67 | 如何添加新语言？敏感信息如何脱敏？ |

---

## 常见问题 FAQ

### Q1: 如何根据环境切换配置？
**A**: 
1. 创建 `application-{profile}.yml`（如 `application-dev.yml`）
2. 启动时指定：`--spring.profiles.active=dev`
3. 或使用 `@Profile("dev")` 注解

### Q2: 事件发布后如何保证消费？
**A**:
1. 使用 **持久化队列**（RabbitMQ）
2. 消费失败后 **自动重试**
3. 最终失败进入 **死信队列**
4. 监控死信队列并告警

### Q3: Feign 调用超时如何处理？
**A**:
1. 配置合理的超时时间（连接 5s，读 30s）
2. 实现 **服务降级** 返回默认值
3. 添加 **重试机制**（幂等操作）
4. 使用 **熔断器** 快速失败

### Q4: 服务间循环调用如何避免？
**A**:
1. **禁止双向依赖**（A 调 B，B 不能调 A）
2. 使用 **事件驱动** 解耦
3. 引入 **中间服务** 打破循环
4. 代码 Review 时检查依赖关系

### Q5: 如何添加新的语言支持？
**A**:
1. 在 `support-files/i18n/` 下创建 `messages_{locale}.properties`
2. 如添加日文：`messages_ja_JP.properties`
3. 翻译所有 key 的内容
4. 重启服务生效

### Q6: 日志打印过多影响性能？
**A**:
1. 生产环境使用 **INFO** 级别（不用 DEBUG）
2. 避免在循环中打印日志
3. 使用 **异步日志**（Logback AsyncAppender）
4. 定期清理旧日志

### Q7: 敏感信息如何脱敏？
**A**:
```kotlin
// 密码脱敏
logger.info("User login: username={}, password={}", username, "******")

// Token 脱敏（显示前4后4）
logger.info("Token: {}...{}", token.take(4), token.takeLast(4))

// 手机号脱敏（显示前3后4）
logger.info("Phone: {}****{}", phone.take(3), phone.takeLast(4))
```

---

## 总结

本 Skill 涵盖了 BK-CI 微服务架构的 4 大核心基础设施，这些是构建分布式系统的基石。

**学习路径**:
1. 先了解微服务基础（`backend-microservice-development`）
2. 按需深入具体技术（配置/事件/通信/日志）
3. 在实际开发中应用并总结经验

**最佳实践**:
- ✅ 多环境隔离使用条件配置
- ✅ 模块解耦使用事件驱动
- ✅ 服务间调用实现熔断降级
- ✅ 用户文案必须国际化
- ✅ 敏感信息必须脱敏

掌握这些基础设施，让你的微服务架构更加健壮和可维护！🚀
