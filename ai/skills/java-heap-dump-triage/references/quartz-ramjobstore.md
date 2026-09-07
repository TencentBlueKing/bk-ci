# Quartz RAMJobStore Heap Dump 模式

当 MAT 显示 `org.quartz.simpl.RAMJobStore`、`QuartzSchedulerResources`、`CronTriggerImpl`、`CronExpression`、`TriggerWrapper`、`TreeMap$Entry` 或 `JobKey` 占据主要 retained heap 时，使用本参考。

## 典型现象

常见证据：

- `org.quartz.simpl.RAMJobStore` 是最大 retained heap 持有者。
- `org.quartz.core.QuartzSchedulerResources` 出现在到 GC roots 的最短路径上。
- `CronTriggerImpl`、`CronExpression`、`TriggerWrapper` 数量异常高。
- `TreeMap$Entry`、`TreeMap`、`TreeSet`、`Integer` 数量高，通常是 Quartz 内部保存和排序 trigger fire time 产生的结构。
- 线程栈可能只显示 `QuartzSchedulerThread.run()` 在等待。这个线程通常是保留路径，不一定是泄漏源头。

## 代码检查点

重点阅读 scheduler 封装类和所有调用点：

1. `scheduleJob`、`addJob`、`rescheduleJob`、`deleteJob`、`unscheduleJob`、`checkExists`。
2. Job 身份：job name、job group、trigger name、trigger group。
3. Key 组成：project id、pipeline id、task id、cron md5/hash、tenant/shard/channel。
4. Reload 逻辑：启动或周期性 reload 必须幂等。
5. 删除/刷新事件：终止动作必须删除 add 时创建的同一个身份。
6. 异常处理：add 失败要清理部分创建的 job，delete 失败要记录足够上下文。

## 常见根因

- `deleteJob(JobKey)` 使用了 trigger group，而不是 job group。
- 使用 `unscheduleJob(TriggerKey)`，但代码期望 durable job 也一起消失。
- add 路径使用新 key 格式，delete 路径仍使用旧 key 格式。
- 周期性 reload 不断 schedule job，但没有检查或删除已存在 job。
- 数据库中的无效/过期 timer record 没有从 Quartz 中移除。
- 多个服务实例都使用 `RAMJobStore`，并各自加载全量定时任务。

## 修复建议

优先做最小生命周期对称修复：

- 让 add/check/delete 使用同一个 `JobKey`。
- 让 trigger 删除使用同一个 `TriggerKey`。
- 删除失败时记录 delete result、key、group。
- 增加一个小测试：使用内存 Quartz scheduler，add 后 check 为 true，delete 后 check 为 false。

如果 trigger 数量本身确实很大：

- 增加 job count 和 trigger count 指标。
- 评估 JDBC JobStore，注意数据库压力、集群锁、misfire 策略。
- 从业务层减少展开后的 cron trigger 数量。

## 生产验证

修复上线后：

- 对使用 `RAMJobStore` 的服务做重启；历史残留 job 存在于进程内存里。
- 对比 Quartz trigger 数量和数据库有效 timer 记录数乘以每条记录的 cron 表达式数。
- 运行一段时间后重新导出 heap dump，确认 `RAMJobStore` 不再占据主要 retained heap。
- 观察 delete miss、reload add count、expired timer cleanup 等日志。
