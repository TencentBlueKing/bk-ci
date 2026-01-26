---
name: common-technical-practices
description: 通用技术实践指南，涵盖 AOP 切面、分布式锁、重试机制、参数校验、性能监控、定时任务、审计日志等后端开发中的常见技术实践。当用户需要实现横切关注点、处理并发控制、配置重试策略、添加性能监控或实现审计功能时使用。
core_files:
  - "src/backend/ci/core/common/common-web/src/main/kotlin/com/tencent/devops/common/web/aop/"
  - "src/backend/ci/core/common/common-redis/src/main/kotlin/com/tencent/devops/common/redis/"
  - "src/backend/ci/core/common/common-service/src/main/kotlin/com/tencent/devops/common/service/utils/RetryUtils.kt"
  - "src/backend/ci/core/common/common-audit/"
related_skills:
  - backend-microservice-development
  - design-patterns
token_estimate: 10000
---

# 通用技术实践指南

## Skill 概述

本 Skill 涵盖了 BK-CI 后端开发中常用的 **7 大通用技术实践**，这些技术是横跨多个模块的 **横切关注点**（Cross-Cutting Concerns），与 Spring Boot 框架紧密集成。

### 核心主题

| 主题 | 说明 | 文档 |
|------|------|------|
| **AOP 切面编程** | 拦截器、日志切面、权限切面 | [1-aop-aspect.md] |
| **分布式锁** | Redis 锁、并发控制、死锁预防 | [2-distributed-lock.md] |
| **重试机制** | 重试策略、退避算法、幂等性 | [3-retry-mechanism.md] |
| **参数校验** | JSR-303 注解、自定义校验器 | [4-parameter-validation.md] |
| **性能监控** | Micrometer 指标、Prometheus | [5-performance-monitoring.md] |
| **定时任务** | Spring Scheduled、分布式调度 | [6-scheduled-tasks.md] |
| **审计日志** | 操作审计、行为追踪、合规性 | [7-audit-logging.md] |

---

## ⚠️ 与 `utility-components` 的区别

### 定位对比

| Skill | 定位 | 关注点 | 典型场景 |
|-------|------|--------|----------|
| **common-technical-practices** (本 Skill) | **框架级实践** | 如何在 Spring Boot 中使用这些技术 | AOP 切面、分布式锁、重试机制、参数校验、性能监控、定时任务、审计日志 |
| **utility-components** | **工具级组件** | 如何使用特定的工具类和组件 | JWT 认证、表达式解析、线程池使用、责任链实现 |

### 使用选择

```
需要实现横切关注点（AOP、锁、重试、监控）
    → 使用 common-technical-practices (本 Skill)

需要使用特定工具类（JWT、表达式、线程池、责任链）
    → 使用 utility-components
```

**示例对比**:
- 需要 **添加性能监控切面** → `common-technical-practices` (reference/5-performance-monitoring.md)
- 需要 **使用线程池批量处理** → `utility-components` (reference/3-thread-pool-loop-util.md)
- 需要 **实现分布式锁** → `common-technical-practices` (reference/2-distributed-lock.md)
- 需要 **实现 JWT 认证** → `utility-components` (reference/1-jwt-security.md)

---

## 技术实践架构

### 横切关注点视图

```
┌─────────────────────────────────────────────────────────────────┐
│                      BK-CI 业务逻辑层                            │
│  (Process/Project/Store/Auth/Repository/Dispatch...)           │
└─────────────────────────────────────────────────────────────────┘
                             ↓
        ┌────────────────────┼────────────────────┐
        │                    │                    │
   ┌────▼────┐         ┌────▼────┐         ┌────▼────┐
   │  AOP    │         │  参数   │         │  审计   │
   │  切面   │         │  校验   │         │  日志   │
   └─────────┘         └─────────┘         └─────────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│                     通用技术实践层                               │
├─────────────────────────────────────────────────────────────────┤
│  • 分布式锁（并发控制）                                          │
│  • 重试机制（容错处理）                                          │
│  • 性能监控（可观测性）                                          │
│  • 定时任务（调度管理）                                          │
└─────────────────────────────────────────────────────────────────┘
```

---

## 使用指南

### 场景 1：实现 AOP 切面

**需求**: 添加日志切面、权限切面、性能监控切面

**步骤**:
1. 查阅 [reference/1-aop-aspect.md](./reference/1-aop-aspect.md)
2. 了解切点表达式语法
3. 选择通知类型（Before/After/Around）
4. 实现切面逻辑

**典型问题**:
- 如何定义切点表达式？
- Around 通知如何获取方法参数？
- 切面执行顺序如何控制？

---

### 场景 2：使用分布式锁

**需求**: 并发控制、资源竞争、数据一致性保证

**步骤**:
1. 查阅 [reference/2-distributed-lock.md](./reference/2-distributed-lock.md)
2. 选择合适的锁粒度
3. 使用 `RedisLock.kt` 工具类
4. 处理锁超时和死锁

**典型问题**:
- 如何避免死锁？
- 锁超时时间如何设置？
- 可重入锁如何实现？

---

### 场景 3：配置重试机制

**需求**: 处理临时性故障、网络抖动、服务降级

**步骤**:
1. 查阅 [reference/3-retry-mechanism.md](./reference/3-retry-mechanism.md)
2. 选择重试策略（固定延迟/指数退避）
3. 配置重试次数和超时
4. 确保操作幂等性

**典型问题**:
- 什么情况下应该重试？
- 如何实现指数退避？
- 重试次数上限如何确定？

---

### 场景 4：添加参数校验

**需求**: 接口参数校验、数据完整性检查

**步骤**:
1. 查阅 [reference/4-parameter-validation.md](./reference/4-parameter-validation.md)
2. 使用 JSR-303 注解（@NotNull/@Size/@Valid）
3. 编写自定义校验器（如需）
4. 配置校验分组

**典型问题**:
- 如何自定义校验注解？
- 嵌套对象如何校验？
- 校验错误消息如何国际化？

---

### 场景 5：实现性能监控

**需求**: 添加性能埋点、监控慢查询、分析瓶颈

**步骤**:
1. 查阅 [reference/5-performance-monitoring.md](./reference/5-performance-monitoring.md)
2. 使用 `@BkTimed` 注解或 `Watcher` 工具类
3. 配置 Prometheus 指标采集
4. 分析性能数据

**典型问题**:
- 如何添加自定义指标？
- Micrometer 如何使用？
- 如何监控数据库慢查询？

---

### 场景 6：创建定时任务

**需求**: 定期清理、数据同步、统计报表

**步骤**:
1. 查阅 [reference/6-scheduled-tasks.md](./reference/6-scheduled-tasks.md)
2. 使用 `@Scheduled` 注解配置 Cron 表达式
3. 添加分布式锁防止并发执行
4. 实现任务监控

**典型问题**:
- Cron 表达式如何编写？
- 如何避免任务重复执行？
- 分布式环境下如何调度？

---

### 场景 7：记录审计日志

**需求**: 操作审计、行为追踪、合规性要求

**步骤**:
1. 查阅 [reference/7-audit-logging.md](./reference/7-audit-logging.md)
2. 使用审计日志工具类
3. 记录关键操作（创建/修改/删除）
4. 存储审计数据

**典型问题**:
- 哪些操作需要审计？
- 审计日志如何存储？
- 如何追溯历史操作？

---

## 核心类与文件速查

### AOP 切面

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `BkAspect` | `common-web/aop/BkAspect.kt` | 基础切面类 |
| `LogAspect` | `common-web/aop/LogAspect.kt` | 日志切面 |

### 分布式锁

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `RedisLock` | `common-redis/RedisLock.kt` | Redis 分布式锁 |
| `RedisOperation` | `common-redis/RedisOperation.kt` | Redis 操作工具 |

### 重试机制

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `RetryUtils` | `common-service/utils/RetryUtils.kt` | 重试工具类 |

### 参数校验

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `BkField` | `common-web/annotation/BkField.kt` | 自定义校验注解 |

### 性能监控

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `BkTimed` | `common-service/prometheus/BkTimed.kt` | 性能监控注解 |
| `Watcher` | `common-api/util/Watcher.kt` | 性能监控工具 |

### 审计日志

| 目录 | 路径 | 说明 |
|------|------|------|
| `common-audit/` | `common/common-audit/` | 审计日志模块 |

---

## 开发规范

### 1. AOP 切面开发

- ✅ 切面类放在 `*.aop` 包下
- ✅ 使用 `@Aspect` + `@Component` 注解
- ✅ 切点表达式尽量精确，避免过度拦截
- ✅ 注意切面执行顺序（使用 `@Order`）

### 2. 分布式锁使用

- ✅ 锁粒度要细，避免大锁
- ✅ 设置合理的超时时间（建议 10-30 秒）
- ✅ 使用 try-finally 确保锁释放
- ✅ 避免在锁内执行耗时操作

### 3. 重试机制配置

- ✅ 仅对 **临时性故障** 重试（网络抖动、超时）
- ✅ 不要对 **业务错误** 重试（参数错误、权限不足）
- ✅ 确保操作幂等性
- ✅ 使用指数退避避免雪崩

### 4. 参数校验规范

- ✅ Controller 层必须校验入参
- ✅ 使用标准 JSR-303 注解
- ✅ 复杂校验使用自定义校验器
- ✅ 校验失败返回明确错误信息

### 5. 性能监控埋点

- ✅ 关键业务流程必须埋点
- ✅ 慢查询（>1s）必须监控
- ✅ 指标命名遵循 Prometheus 规范
- ✅ 避免高基数标签（如 userId）

### 6. 定时任务开发

- ✅ 使用分布式锁避免并发执行
- ✅ 任务执行时间不要与业务高峰重叠
- ✅ 长时间任务要有进度监控
- ✅ 任务失败要有告警机制

### 7. 审计日志记录

- ✅ 记录 **谁** 在 **什么时间** 做了 **什么操作**
- ✅ 敏感信息要脱敏
- ✅ 审计日志不可修改
- ✅ 保留足够长的存储周期（至少 1 年）

---

## 与其他 Skill 的关系

```
common-technical-practices (本 Skill)
    ↓ 依赖
backend-microservice-development     # 后端微服务开发基础
design-patterns                      # 设计模式
    ↓ 被依赖
process-module-architecture          # Process 模块使用这些技术
auth-module-architecture             # Auth 模块使用这些技术
...                                  # 其他业务模块
```

**前置知识**:
- `backend-microservice-development` - 了解 Spring Boot 基础
- `design-patterns` - 了解常见设计模式

**相关 Skill**:
- `microservice-infrastructure` - 微服务基础设施（事件驱动、服务通信）
- `database-design` - 数据库设计（与审计日志存储相关）

---

## 详细文档导航

| 文档 | 内容 | 行数 | 典型问题 |
|------|------|------|----------|
| [1-aop-aspect.md](./reference/1-aop-aspect.md) | AOP 切面编程 | 74 | 如何定义切点？Around 通知如何使用？ |
| [2-distributed-lock.md](./reference/2-distributed-lock.md) | 分布式锁 | 164 | 如何避免死锁？锁超时如何处理？ |
| [3-retry-mechanism.md](./reference/3-retry-mechanism.md) | 重试机制 | 75 | 如何实现指数退避？幂等性如何保证？ |
| [4-parameter-validation.md](./reference/4-parameter-validation.md) | 参数校验 | 74 | 如何自定义校验注解？嵌套对象如何校验？ |
| [5-performance-monitoring.md](./reference/5-performance-monitoring.md) | 性能监控 | 77 | 如何添加自定义指标？慢查询如何监控？ |
| [6-scheduled-tasks.md](./reference/6-scheduled-tasks.md) | 定时任务 | 65 | Cron 表达式如何写？分布式调度如何做？ |
| [7-audit-logging.md](./reference/7-audit-logging.md) | 审计日志 | 69 | 哪些操作需要审计？审计日志如何存储？ |

---

## 常见问题 FAQ

### Q1: AOP 切面不生效？
**A**: 检查：
1. 切面类是否加了 `@Aspect` 和 `@Component`
2. 切点表达式是否正确
3. 目标方法是否是 Spring Bean 的 public 方法

### Q2: 分布式锁死锁如何排查？
**A**: 
1. 检查锁是否设置了超时时间
2. 确认 finally 块中释放了锁
3. 使用 Redis 命令查看锁状态：`GET lock_key`

### Q3: 重试机制导致雪崩？
**A**:
1. 使用 **指数退避** 而非固定延迟
2. 设置 **最大重试次数**（建议 3-5 次）
3. 添加 **熔断器** 快速失败

### Q4: 参数校验失败返回 500？
**A**:
1. 确认有全局异常处理器捕获 `MethodArgumentNotValidException`
2. 返回 400 而非 500
3. 错误信息格式统一

### Q5: 性能监控指标不准？
**A**:
1. 确认 `@BkTimed` 注解的方法是 Spring Bean
2. 检查 Prometheus 配置
3. 避免在循环中使用 Watcher

### Q6: 定时任务重复执行？
**A**:
1. 添加 **分布式锁**
2. 检查是否有多个实例同时运行
3. 确认 Cron 表达式正确

### Q7: 审计日志丢失？
**A**:
1. 检查审计日志存储是否正常
2. 确认异步写入的队列没有溢出
3. 添加日志持久化机制

---

## 总结

本 Skill 涵盖了 BK-CI 后端开发中 7 大通用技术实践，这些技术是横跨多个模块的基础设施，掌握它们对于开发高质量、高可靠的微服务至关重要。

**学习路径**:
1. 先了解 Spring Boot 基础（`backend-microservice-development`）
2. 按需深入具体技术（AOP/锁/重试/...）
3. 在实际开发中应用并总结经验

**最佳实践**:
- ✅ 横切关注点使用 AOP
- ✅ 并发控制使用分布式锁
- ✅ 临时性故障使用重试
- ✅ 接口入参必须校验
- ✅ 关键流程必须监控
- ✅ 定时任务避免并发
- ✅ 敏感操作必须审计

开始使用这些技术实践，让你的代码更加健壮和可维护！🚀
