---
name: 16-scheduled-tasks
description: 定时任务开发指南
---

# 定时任务开发

定时任务开发指南.

## 触发条件

当用户需要实现定时任务、周期性任务、Cron 调度时，使用此 Skill。

## @Scheduled 注解

```kotlin
@Component
class AuthCronManager(
    private val redisOperation: RedisOperation
) {
    companion object {
        private val logger = LoggerFactory.getLogger(AuthCronManager::class.java)
    }
    
    // 每天凌晨 2 点执行
    @Scheduled(cron = "0 0 2 * * ?")
    fun cleanExpiredTokens() {
        val lock = RedisLock(redisOperation, "auth:clean:token", 3600)
        if (!lock.tryLock()) {
            logger.info("其他节点正在执行清理任务")
            return
        }
        try {
            doClean()
        } finally {
            lock.unlock()
        }
    }
    
    // 每 5 分钟执行
    @Scheduled(fixedRate = 300000)
    fun syncPermissions() {
        // 同步权限
    }
    
    // 上次执行完成后 1 分钟再执行
    @Scheduled(fixedDelay = 60000)
    fun processQueue() {
        // 处理队列
    }
}
```

## Cron 表达式

```
秒 分 时 日 月 周
0  0  2  *  *  ?   # 每天凌晨 2 点
0  */5 * * * ?     # 每 5 分钟
0  0  0  1  *  ?   # 每月 1 号零点
0  0  8-18 * * ?   # 每天 8-18 点整点
```

## 分布式锁保护

```kotlin
@Scheduled(cron = "0 0 3 * * ?")
fun dailyTask() {
    val lock = RedisLock(
        redisOperation = redisOperation,
        lockKey = "cron:daily:task",
        expiredTimeInSeconds = 7200  // 2 小时
    )
    
    if (!lock.tryLock()) {
        return  // 其他节点已在执行
    }
    
    try {
        logger.info("开始执行每日任务")
        doDailyTask()
        logger.info("每日任务执行完成")
    } catch (e: Exception) {
        logger.error("每日任务执行失败", e)
    } finally {
        lock.unlock()
    }
}
```

## 配置启用

```kotlin
@Configuration
@EnableScheduling
class ScheduleConfiguration
```

## 最佳实践

1. **分布式锁**：多节点部署时使用 Redis 锁
2. **异常处理**：捕获异常避免影响后续执行
3. **日志记录**：记录任务开始和结束
4. **合理间隔**：避免任务重叠执行

## 相关文件

- `auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/cron/`
