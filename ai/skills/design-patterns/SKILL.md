---
name: design-patterns
description: BK-CI 项目设计模式实践指南，涵盖工厂模式创建任务和SCM实例、策略模式重构权限检查和数据迁移、观察者模式事件驱动、责任链处理请求流程、装饰器增强异常和权限服务。当用户学习设计模式、重构代码、设计可扩展架构、理解 BK-CI 项目设计或需要选择合适的 design pattern 时使用。
---

# BK-CI 项目设计模式实践指南

## 模式选择决策

按场景快速定位适用模式：

- **根据类型创建对象？** → [工厂模式](reference/factory-pattern.md) — `TaskFactory`, `ScmFactory`
- **多种算法/策略可切换？** → [策略模式](reference/strategy-pattern.md) — 权限检查、数据迁移
- **请求需经多步处理？** → [责任链模式](reference/chain-of-responsibility.md) — `StoreCreateHandlerChain`
- **状态变更需通知多方？** → [观察者模式](reference/observer-pattern.md) — Spring Event 事件监听
- **动态增强对象功能？** → [装饰器模式](reference/decorator-pattern.md) — 异常装饰、权限装饰
- **定义算法框架、子类实现步骤？** → [模板方法模式](reference/template-method.md) — 版本后置处理器
- **简化复杂子系统接口？** → [facade-pattern](reference/facade-pattern.md) — `ParamFacadeService`
- **统一不同SCM接口？** → [适配器模式](reference/adapter-pattern.md) — 代码库服务适配

## 快速示例：工厂模式

```kotlin
// TaskFactory — 根据 classType 创建对应 Task 实例
val task = TaskFactory.create(buildTask.classType)
task.execute(buildTask, buildVariables, workspace)
```

```kotlin
// 注册新策略到 Spring 容器
@Component
class MyCheckStrategy : PermissionCheckStrategy {
    override fun check(request: PermissionRequest): Boolean { /* ... */ }
}
// 工厂自动通过 List<PermissionCheckStrategy> 注入获取
```

## BK-CI Kotlin 惯例

- 单例用 `object`，不用双重检查锁
- 复杂对象用 `data class` + 命名参数，不用传统 Builder
- 策略实现注册到 Spring 容器，通过 `@Component` + 工厂获取
- 责任链处理器通过 `List<Handler>` 自动注入编排

## 相关文件索引

### 工厂模式
- `worker-common/task/TaskFactory.kt`
- `common-scm/ScmFactory.kt`
- `worker-common/api/ApiFactory.kt`
- `common-api/factory/DigestFactory.kt`

### 策略模式
- `process/biz-process/strategy/bus/` — 权限检查策略
- `misc/biz-misc/strategy/impl/` — 数据迁移策略（20+ 个）
- `log/biz-log/strategy/factory/` — 日志权限检查策略

### 责任链模式
- `store/biz-store/common/handler/` — 研发商店处理链
- `process/biz-base/engine/interceptor/` — 流水线拦截器链
- `common-webhook/service/code/filter/` — Webhook 过滤器链

### 观察者模式
- `process/biz-process/engine/listener/` — 流水线事件监听器
- `quality/biz-quality/listener/` — 质量检查监听器
- `project/biz-project/listener/` — 项目事件监听器

### 模板方法模式
- `process/biz-process/service/pipeline/version/processor/` — 流水线版本后置处理器

### 装饰器模式
- `worker-common/exception/TaskExecuteExceptionDecorator.kt`
- `auth/biz-auth/provider/rbac/service/DelegatingPermissionServiceDecorator.kt`
