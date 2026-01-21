---
name: utility-components
description: 工具组件指南，涵盖 JWT 安全认证、表达式解析器、线程池循环工具、责任链模式等特定功能的工具类使用。当用户需要实现 JWT 认证、解析表达式、使用线程池或实现责任链时使用。
core_files:
  - "src/backend/ci/core/common/common-security/src/main/kotlin/com/tencent/devops/common/security/jwt/"
  - "src/backend/ci/core/common/common-expression/"
  - "src/backend/ci/core/common/common-util/src/main/kotlin/com/tencent/devops/common/util/"
related_skills:
  - common-technical-practices
  - design-patterns
token_estimate: 6000
---

# 工具组件指南

## Skill 概述

本 Skill 涵盖了 BK-CI 中常用的 **4 类工具组件**，这些是特定功能的工具类和组件实现。

### 核心主题

| 主题 | 说明 | 文档 |
|------|------|------|
| **JWT 安全认证** | JWT 生成验证、Token 刷新、OAuth2 | [1-jwt-security.md](./reference/1-jwt-security.md) |
| **表达式解析器** | 变量表达式、条件求值、自定义函数 | [2-expression-parser.md](./reference/2-expression-parser.md) |
| **线程池循环工具** | 线程池配置、批量处理、循环工具类 | [3-thread-pool-loop-util.md](./reference/3-thread-pool-loop-util.md) |
| **责任链模式** | 责任链设计、拦截器链、请求处理链 | [4-chain-responsibility.md](./reference/4-chain-responsibility.md) |

---

## ⚠️ 与 `common-technical-practices` 的区别

### 定位对比

| Skill | 定位 | 关注点 | 典型场景 |
|-------|------|--------|----------|
| **common-technical-practices** | **框架级实践** | 如何在 Spring Boot 中使用技术 | AOP 切面、分布式锁、重试机制、参数校验、性能监控、定时任务、审计日志 |
| **utility-components** (本 Skill) | **工具级组件** | 如何使用特定的工具类和组件 | JWT 认证、表达式解析、线程池使用、责任链实现 |

### 使用选择

```
需要实现横切关注点（AOP、锁、重试、监控）
    → 使用 common-technical-practices

需要使用特定工具类（JWT、表达式、线程池、责任链）
    → 使用 utility-components (本 Skill)
```

**示例对比**:
- 需要 **添加性能监控切面** → `common-technical-practices` (reference/5-performance-monitoring.md)
- 需要 **使用线程池批量处理** → `utility-components` (reference/3-thread-pool-loop-util.md)
- 需要 **实现分布式锁** → `common-technical-practices` (reference/2-distributed-lock.md)
- 需要 **实现 JWT 认证** → `utility-components` (reference/1-jwt-security.md)

---

## 工具组件架构

### 组件分层视图

```
┌─────────────────────────────────────────────────────────────┐
│                    BK-CI 业务逻辑层                          │
│      (Process/Project/Store/Auth/Repository...)            │
└─────────────────────────────────────────────────────────────┘
                           ↓
        ┌──────────────────┼──────────────────┐
        │                  │                  │
   ┌────▼────┐       ┌────▼────┐       ┌────▼────┐
   │  JWT    │       │  表达式  │       │  线程池  │
   │  认证   │       │  解析   │       │  工具   │
   └─────────┘       └─────────┘       └─────────┘
        │                  │                  │
        └──────────────────┼──────────────────┘
                           ↓
                  ┌────────────────┐
                  │  责任链模式    │
                  │  (拦截器链)    │
                  └────────────────┘
```

---

## 一、JWT 安全认证

详见 [reference/1-jwt-security.md](./reference/1-jwt-security.md)

### 核心功能

- JWT Token 生成与验证
- Token 刷新机制
- 权限校验拦截器
- OAuth2 集成

### 快速开始

```kotlin
// 生成 JWT Token
val token = JwtManager.generateToken(userId, expireTime)

// 验证 Token
val claims = JwtManager.verifyToken(token)
```

---

## 二、表达式解析器

详见 [reference/2-expression-parser.md](./reference/2-expression-parser.md)

### 核心功能

- 变量表达式解析 (`${variable}`)
- 条件表达式求值
- 自定义函数扩展
- 表达式缓存优化

### 快速开始

```kotlin
// 解析变量表达式
val context = mapOf("buildId" to "b-123", "status" to "success")
val result = ExpressionParser.parse("${buildId}_${status}", context)
// 结果: "b-123_success"
```

---

## 三、线程池与循环工具

详见 [reference/3-thread-pool-loop-util.md](./reference/3-thread-pool-loop-util.md)

### 核心功能

- 线程池配置与管理
- 批量任务并发处理
- 循环工具类 (`LoopUtil`)
- 并发控制与优化

### 快速开始

```kotlin
// 批量并发处理
val results = ThreadPoolUtil.executeBatch(taskList) { task ->
    processTask(task)
}

// 循环重试
LoopUtil.loopWithRetry(maxRetries = 3) {
    callExternalApi()
}
```

---

## 四、责任链模式

详见 [reference/4-chain-responsibility.md](./reference/4-chain-responsibility.md)

### 核心功能

- 责任链设计与实现
- 拦截器链模式
- 流水线插件链
- 请求处理链

### 快速开始

```kotlin
// 定义拦截器链
val chain = InterceptorChain()
    .addInterceptor(AuthInterceptor())
    .addInterceptor(ValidationInterceptor())
    .addInterceptor(LoggingInterceptor())

// 执行链
chain.proceed(request)
```

---

## 使用场景决策树

```
用户需求
    ↓
是横切关注点（AOP/锁/重试/监控）？
    ├─ 是 → 使用 common-technical-practices
    └─ 否 → 是否需要特定工具类？
              ├─ JWT 认证 → utility-components (reference/1)
              ├─ 表达式解析 → utility-components (reference/2)
              ├─ 线程池处理 → utility-components (reference/3)
              ├─ 责任链模式 → utility-components (reference/4)
              └─ 其他 → 查找对应模块 Skill
```

---

## 相关 Skill

- [common-technical-practices](../common-technical-practices/SKILL.md) - 通用技术实践（框架级）
- [design-patterns](../design-patterns/SKILL.md) - 设计模式指南
- [backend-microservice-development](../backend-microservice-development/SKILL.md) - 后端微服务开发

---

## Quick Reference

| 需求 | 使用 Skill | 参考章节 |
|------|-----------|----------|
| 实现 JWT 认证 | utility-components | reference/1-jwt-security.md |
| 解析流水线变量 | utility-components | reference/2-expression-parser.md |
| 批量并发处理 | utility-components | reference/3-thread-pool-loop-util.md |
| 实现拦截器链 | utility-components | reference/4-chain-responsibility.md |
| 添加 AOP 切面 | common-technical-practices | reference/1-aop-aspect.md |
| 实现分布式锁 | common-technical-practices | reference/2-distributed-lock.md |
| 配置重试机制 | common-technical-practices | reference/3-retry-mechanism.md |
