---
name: 16-scheduled-tasks
description: 定时任务开发指南，涵盖 Spring Scheduled、Cron 表达式、分布式任务调度、任务锁、任务监控。当用户创建定时任务、配置 Cron 表达式、实现分布式调度或处理任务并发时使用。
core_files:
  - "src/backend/ci/core/auth/biz-auth/src/main/kotlin/com/tencent/devops/auth/cron/"
related_skills:
  - 10-distributed-lock
token_estimate: 1200
---

# 定时任务开发

## Quick Reference

```
Cron 格式：秒 分 时 日 月 周
示例：0 0 2 * * ?（每天凌晨2点）
多节点部署：必须使用 RedisLock 防止重复执行
```

### 最简示例

```kotlin
@Component
class MyCronManager(private val redisOperation: RedisOperation) {
    
    @Scheduled(cron = "0 0 2 * * ?")  // 每天凌晨 2 点
    fun dailyClean() {
        val lock = RedisLock(redisOperation, "cron:daily:clean", 3600)
        if (!lock.tryLock()) {
            return  // 其他节点正在执行
        }
        try {
            doClean()
        } finally {
            lock.unlock()
        }
    }
    
    @Scheduled(fixedRate = 300000)  // 每 5 分钟
    fun syncData() { }
    
    @Scheduled(fixedDelay = 60000)  // 上次完成后 1 分钟再执行
    fun processQueue() { }
}
```

## When to Use

- 周期性任务
- 定时清理
- 数据同步

---

## Cron 表达式

```
秒 分 时 日 月 周
0  0  2  *  *  ?   # 每天凌晨 2 点
0  */5 * * * ?     # 每 5 分钟
0  0  0  1  *  ?   # 每月 1 号零点
```

## 配置启用

```kotlin
@Configuration
@EnableScheduling
class ScheduleConfiguration
```

---

## Checklist

- [ ] 多节点部署使用 Redis 锁
- [ ] 捕获异常避免影响后续执行
- [ ] 记录任务开始和结束日志
- [ ] 设置合理的锁超时时间
