---
name: common-technical-practices
description: 通用技术实践指南，涵盖 AOP 切面编程、分布式锁、重试机制、参数校验、性能监控、定时任务、审计日志等后端开发中的常见技术实践。当用户需要实现横切关注点、处理并发控制、配置重试策略、添加性能监控或实现审计功能时使用。也适用于用户提到切面编程、锁、重试、监控、拦截器、并发、死锁、退避算法、埋点、Cron、审计追踪等场景。
---

# 通用技术实践指南

本 Skill 涵盖 BK-CI 后端开发中常用的通用技术实践，这些技术是横跨多个模块的横切关注点（Cross-Cutting Concerns），与 Spring Boot 框架紧密集成。

## 使用指南

### 场景 1：实现 AOP 切面

**适用于**: 添加日志切面、权限切面、性能监控切面

**步骤**:

1. 在 `*.aop` 包下创建切面类，使用 `@Aspect` + `@Component` 注解
2. 定义切点表达式，选择通知类型（Before/After/Around）
3. 实现切面逻辑，例如日志切面：

```kotlin
@Aspect
@Component
@Order(1)
class LogAspect {
    @Around("@annotation(com.tencent.devops.common.web.aop.BkTimed)")
    fun logExecutionTime(joinPoint: ProceedingJoinPoint): Any? {
        val start = System.currentTimeMillis()
        val result = joinPoint.proceed()
        val duration = System.currentTimeMillis() - start
        logger.info("${joinPoint.signature.name} executed in ${duration}ms")
        return result
    }
}
```

4. 验证：确认目标方法是 Spring Bean 的 public 方法，切面类已被扫描

> 参考: [reference/1-aop-aspect.md](./reference/1-aop-aspect.md)

---

### 场景 2：使用分布式锁

**适用于**: 并发控制、资源竞争、数据一致性保证

**步骤**:

1. 引入 `RedisLock`，选择合适的锁粒度（细粒度优先）
2. 获取锁并在 try-finally 中执行业务逻辑：

```kotlin
val redisLock = RedisLock(redisOperation, "lock:pipeline:$pipelineId", 30)
try {
    val locked = redisLock.tryLock()
    if (!locked) {
        throw OperationException("获取锁失败，请稍后重试")
    }
    // --- 验证点：确认锁已获取 ---
    logger.info("Lock acquired for pipeline: $pipelineId")

    // 执行业务逻辑
    doPipelineUpdate(pipelineId)

    // --- 验证点：确认业务逻辑完成 ---
    logger.info("Pipeline update completed: $pipelineId")
} finally {
    redisLock.unlock()
    // --- 验证点：确认锁已释放 ---
    logger.info("Lock released for pipeline: $pipelineId")
}
```

3. **验证检查清单**:
   - [ ] 锁超时时间已设置（建议 10-30 秒）
   - [ ] finally 块中释放了锁
   - [ ] 锁内无耗时操作（如远程调用）
   - [ ] 锁 key 粒度足够细（如包含资源 ID）
   - [ ] 获取锁失败有明确错误提示

> 参考: [reference/2-distributed-lock.md](./reference/2-distributed-lock.md)

---

### 场景 3：配置重试机制

**适用于**: 处理临时性故障、网络抖动、服务降级

**步骤**:

1. 使用 `RetryUtils` 配置重试策略：

```kotlin
// 指数退避重试，适用于网络抖动场景
val result = RetryUtils.retryWhenException(
    retryTime = 3,
    retryPeriodMills = 1000  // 基础延迟 1 秒，后续指数递增
) {
    // 确保此操作是幂等的
    remoteService.fetchData(resourceId)
}
```

2. 仅对临时性故障重试（网络超时、连接失败），不要对业务错误重试（参数错误、权限不足）
3. 使用指数退避避免雪崩，设置最大重试次数（建议 3-5 次）

> 参考: [reference/3-retry-mechanism.md](./reference/3-retry-mechanism.md)

---

### 场景 4：添加参数校验

**适用于**: 接口参数校验、数据完整性检查

**步骤**:

1. 在 Controller 层使用 JSR-303 注解校验入参：

```kotlin
data class CreatePipelineRequest(
    @field:NotBlank(message = "流水线名称不能为空")
    @field:Size(max = 128, message = "流水线名称不能超过128个字符")
    val name: String,

    @field:BkField(required = true, patternStyle = BkStyleEnum.ID_STYLE)
    val projectId: String
)

@PostMapping("/pipelines")
fun create(@Valid @RequestBody request: CreatePipelineRequest): Result<String> {
    return pipelineService.create(request)
}
```

2. 复杂校验使用自定义校验器，确保校验失败返回 400（非 500）并包含明确错误信息

> 参考: [reference/4-parameter-validation.md](./reference/4-parameter-validation.md)

---

### 场景 5：实现性能监控

**适用于**: 添加性能埋点、监控慢查询、分析瓶颈

**步骤**:

1. 使用 `@BkTimed` 注解或 `Watcher` 工具类添加埋点：

```kotlin
// 方式一：注解方式
@BkTimed(value = "pipeline_build_time", description = "流水线构建耗时")
fun buildPipeline(pipelineId: String): BuildResult { ... }

// 方式二：Watcher 手动埋点，适用于细粒度监控
val watcher = Watcher("buildPipeline")
try {
    watcher.start("prepare")
    prepareBuild()
    watcher.start("execute")
    executeBuild()
} finally {
    watcher.stop()
    logger.info("Performance: ${watcher.toString()}")
}
```

2. 指标命名遵循 Prometheus 规范，避免高基数标签（如 userId）
3. 慢查询（>1s）必须监控

> 参考: [reference/5-performance-monitoring.md](./reference/5-performance-monitoring.md)

---

### 场景 6：创建定时任务

**适用于**: 定期清理、数据同步、统计报表

**步骤**:

1. 使用 `@Scheduled` 注解配置 Cron 表达式，并结合分布式锁防止并发执行：

```kotlin
@Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点执行
fun cleanExpiredData() {
    val redisLock = RedisLock(redisOperation, "lock:clean:expired", 600)
    try {
        if (!redisLock.tryLock()) {
            logger.info("其他实例正在执行清理任务，跳过")
            return
        }
        doClean()
    } finally {
        redisLock.unlock()
    }
}
```

2. 任务执行时间避开业务高峰，长时间任务添加进度监控和告警机制

> 参考: [reference/6-scheduled-tasks.md](./reference/6-scheduled-tasks.md)

---

### 场景 7：记录审计日志

**适用于**: 操作审计、行为追踪、合规性要求

**步骤**:

1. 使用审计日志工具类记录关键操作：

```kotlin
auditService.createAudit(
    AuditEvent(
        userId = userId,
        action = ActionEnum.DELETE,
        resourceType = ResourceTypeEnum.PIPELINE,
        resourceId = pipelineId,
        timestamp = LocalDateTime.now(),
        detail = "删除流水线: $pipelineName"
    )
)
```

2. 记录谁在什么时间做了什么操作，敏感信息需脱敏
3. 审计日志不可修改，保留足够长的存储周期（至少 1 年）

> 参考: [reference/7-audit-logging.md](./reference/7-audit-logging.md)

---

## 核心类与文件速查

| 类/文件 | 路径 | 说明 |
|---------|------|------|
| `BkAspect` | `common-web/aop/BkAspect.kt` | 基础切面类 |
| `LogAspect` | `common-web/aop/LogAspect.kt` | 日志切面 |
| `RedisLock` | `common-redis/RedisLock.kt` | Redis 分布式锁 |
| `RedisOperation` | `common-redis/RedisOperation.kt` | Redis 操作工具 |
| `RetryUtils` | `common-service/utils/RetryUtils.kt` | 重试工具类 |
| `BkField` | `common-web/annotation/BkField.kt` | 自定义校验注解 |
| `BkTimed` | `common-service/prometheus/BkTimed.kt` | 性能监控注解 |
| `Watcher` | `common-api/util/Watcher.kt` | 性能监控工具 |
| `common-audit/` | `common/common-audit/` | 审计日志模块 |

---

## 开发规范

1. **AOP 切面**: 切面类放在 `*.aop` 包下，切点表达式尽量精确，使用 `@Order` 控制执行顺序
2. **分布式锁**: 锁粒度要细，设置合理超时（10-30 秒），try-finally 确保释放，锁内避免耗时操作
3. **重试机制**: 仅对临时性故障重试，确保操作幂等，使用指数退避避免雪崩
4. **参数校验**: Controller 层必须校验入参，使用标准 JSR-303 注解，校验失败返回明确错误信息
5. **性能监控**: 关键流程必须埋点，慢查询（>1s）必须监控，指标命名遵循 Prometheus 规范
6. **定时任务**: 使用分布式锁避免并发执行，避开业务高峰，长任务需进度监控
7. **审计日志**: 记录操作人、时间、操作内容，敏感信息脱敏，日志不可修改

---

## 与其他 Skill 的关系

**前置知识**:
- `backend-microservice-development` - Spring Boot 基础
- `design-patterns` - 常见设计模式

**相关 Skill**:
- `utility-components` - 工具级组件（JWT、表达式解析、线程池、责任链）
- `microservice-infrastructure` - 微服务基础设施（事件驱动、服务通信）
- `database-design` - 数据库设计（与审计日志存储相关）

## 详细文档导航

| 文档 | 内容 | 典型问题 |
|------|------|----------|
| [1-aop-aspect.md](./reference/1-aop-aspect.md) | AOP 切面编程 | 如何定义切点？Around 通知如何使用？ |
| [2-distributed-lock.md](./reference/2-distributed-lock.md) | 分布式锁 | 如何避免死锁？锁超时如何处理？ |
| [3-retry-mechanism.md](./reference/3-retry-mechanism.md) | 重试机制 | 如何实现指数退避？幂等性如何保证？ |
| [4-parameter-validation.md](./reference/4-parameter-validation.md) | 参数校验 | 如何自定义校验注解？嵌套对象如何校验？ |
| [5-performance-monitoring.md](./reference/5-performance-monitoring.md) | 性能监控 | 如何添加自定义指标？慢查询如何监控？ |
| [6-scheduled-tasks.md](./reference/6-scheduled-tasks.md) | 定时任务 | Cron 表达式如何写？分布式调度如何做？ |
| [7-audit-logging.md](./reference/7-audit-logging.md) | 审计日志 | 哪些操作需要审计？审计日志如何存储？ |
