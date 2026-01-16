---
name: 10-distributed-lock
description: 分布式锁使用指南，涵盖 Redis 分布式锁、锁超时处理、可重入锁、锁粒度设计、死锁预防。当用户需要并发控制、实现分布式锁、处理资源竞争或保证数据一致性时使用。
# 结构化元数据（支持智能压缩）
core_files:
  - src/backend/ci/core/common/common-redis/src/main/kotlin/com/tencent/devops/common/redis/RedisLock.kt
  - src/backend/ci/core/common/common-redis/src/main/kotlin/com/tencent/devops/common/redis/RedisOperation.kt
related_skills:
  - 01-backend-microservice-development
  - 13-retry-mechanism
token_estimate: 3500
---

# 分布式锁

<!-- ═══════════════════════════════════════════════════════════════════════════
     🚀 快速参考区（放在最前面 - 解决 Lost-in-Middle 问题）
     ═══════════════════════════════════════════════════════════════════════════ -->

## Quick Reference

| 项目 | 值 |
|------|-----|
| **核心类** | `RedisLock` |
| **路径** | `common-redis/.../RedisLock.kt` |
| **依赖** | `RedisOperation` (注入) |

**最简用法**（一行代码）:
```kotlin
RedisLock(redisOperation, "pipeline:build:$id", 60).lockAround { doBuild() }
```

**锁命名格式**: `业务:操作:资源ID`
```kotlin
"pipeline:build:$pipelineId"    // 流水线构建锁
"project:update:$projectId"     // 项目更新锁
"task:process:$taskId"          // 任务处理锁
```

---

## When to Use ✅

- 多实例部署环境下需要互斥访问共享资源
- 防止同一流水线/任务被并发执行
- 分布式环境下的数据一致性保证
- 需要跨服务的资源锁定

## When NOT to Use ❌

- 单机环境（使用 `synchronized` 或 `ReentrantLock`）
- 锁持有时间超过 5 分钟（考虑其他方案如数据库乐观锁）
- 高频短时操作（Redis 网络开销可能成为瓶颈）
- 需要公平锁语义（RedisLock 不保证公平性）

---

## 详细说明

### RedisLock 实现原理

```kotlin
class RedisLock(
    private val redisOperation: RedisOperation,
    private val lockKey: String,
    private val expiredTimeInSeconds: Long,
    private val sleepTime: Long = 100L,
    private var lockValue: String = UUID.randomUUID().toString()
) : AutoCloseable {

    // 阻塞获取锁
    fun lock() {
        synchronized(getLocalLock()) {
            while (true) {
                if (tryLockRemote()) break
                Thread.sleep(sleepTime)
            }
        }
    }

    // 尝试获取锁（非阻塞）
    fun tryLock(): Boolean = tryLockRemote()

    // 释放锁
    fun unlock(): Boolean = unLockRemote()

    // 自动释放（try-with-resources）
    override fun close() = unlock()

    // 锁包装执行
    fun <T> lockAround(action: () -> T): T {
        try {
            this.lock()
            return action()
        } finally {
            this.unlock()
        }
    }
}
```

### 使用方式

#### 1. 推荐：lockAround 包装（最简洁）

```kotlin
fun processTask(taskId: String): Result<String> {
    val lock = RedisLock(redisOperation, "task:$taskId", 60)
    return lock.lockAround {
        doProcess(taskId)
    }
}
```

#### 2. try-with-resources（Kotlin use）

```kotlin
fun updatePipeline(pipelineId: String) {
    RedisLock(redisOperation, "pipeline:update:$pipelineId", 30).use { lock ->
        lock.lock()
        doUpdate(pipelineId)
    }
}
```

#### 3. 传统 try-finally

```kotlin
@Service
class PipelineService(private val redisOperation: RedisOperation) {
    fun startBuild(pipelineId: String) {
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "pipeline:build:$pipelineId",
            expiredTimeInSeconds = 60
        )
        
        try {
            lock.lock()
            doBuild(pipelineId)
        } finally {
            lock.unlock()
        }
    }
}
```

#### 4. 非阻塞尝试（适合可跳过的任务）

```kotlin
fun tryProcess(resourceId: String): Boolean {
    val lock = RedisLock(redisOperation, "resource:$resourceId", 30)
    
    if (!lock.tryLock()) {
        logger.info("资源 $resourceId 正在被其他进程处理")
        return false
    }
    
    try {
        doProcess(resourceId)
        return true
    } finally {
        lock.unlock()
    }
}
```

### Lua 解锁脚本

```lua
-- 防止误删其他客户端的锁
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

---

## 相关文件

| 文件 | 说明 |
|------|------|
| `common-redis/.../RedisLock.kt` | 分布式锁核心实现 |
| `common-redis/.../RedisOperation.kt` | Redis 操作封装 |

---

<!-- ═══════════════════════════════════════════════════════════════════════════
     🎯 决策清单（放在最后 - 强化记忆）
     ═══════════════════════════════════════════════════════════════════════════ -->

## Checklist

使用分布式锁前，请确认：

- [ ] **超时时间合理**：通常 30-120 秒，必须大于业务执行时间
- [ ] **锁释放保证**：使用 `try-finally` / `use` / `lockAround`
- [ ] **命名规范**：遵循 `业务:操作:资源ID` 格式
- [ ] **粒度适当**：锁范围尽可能小，避免大范围锁定
- [ ] **异常处理**：业务异常不影响锁释放
- [ ] **监控告警**：关键锁添加获取超时告警
