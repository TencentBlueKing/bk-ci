---
name: 10-distributed-lock
description: 分布式锁使用指南
---

# 分布式锁

分布式锁使用指南.

## 触发条件

当用户需要实现分布式环境下的资源互斥访问、防止并发冲突时，使用此 Skill。

## RedisLock 实现

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

## 使用方式

### 1. 基本用法

```kotlin
@Service
class PipelineService(
    private val redisOperation: RedisOperation
) {
    fun startBuild(pipelineId: String) {
        val lock = RedisLock(
            redisOperation = redisOperation,
            lockKey = "pipeline:build:$pipelineId",
            expiredTimeInSeconds = 60
        )
        
        try {
            lock.lock()
            // 执行构建逻辑
            doBuild(pipelineId)
        } finally {
            lock.unlock()
        }
    }
}
```

### 2. try-with-resources

```kotlin
fun updatePipeline(pipelineId: String) {
    RedisLock(redisOperation, "pipeline:update:$pipelineId", 30).use { lock ->
        lock.lock()
        // 更新逻辑
        doUpdate(pipelineId)
    }
}
```

### 3. lockAround 包装

```kotlin
fun processTask(taskId: String): Result<String> {
    val lock = RedisLock(redisOperation, "task:$taskId", 60)
    return lock.lockAround {
        // 处理任务
        doProcess(taskId)
    }
}
```

### 4. 非阻塞尝试

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

## Lua 解锁脚本

```lua
-- 防止误删其他客户端的锁
if redis.call("get", KEYS[1]) == ARGV[1] then
    return redis.call("del", KEYS[1])
else
    return 0
end
```

## 锁命名规范

```kotlin
// 格式：业务:操作:资源ID
"pipeline:build:$pipelineId"
"project:update:$projectId"
"task:process:$taskId"
"user:login:$userId"
```

## 最佳实践

1. **合理设置过期时间**：根据业务执行时间设置，避免死锁
2. **使用 UUID 作为锁值**：确保只释放自己持有的锁
3. **优先使用 try-finally**：确保锁一定被释放
4. **本地锁优化**：先获取本地锁减少 Redis 请求

## 相关文件

- `common-redis/src/main/kotlin/com/tencent/devops/common/redis/RedisLock.kt`
- `common-redis/src/main/kotlin/com/tencent/devops/common/redis/RedisOperation.kt`
