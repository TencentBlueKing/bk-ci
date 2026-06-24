## 1. API 与入参建模

- [x] 1.1 在 `api-project-op` 增加比例放量接口定义（含 dry-run/execute 模式）。
- [x] 1.2 新增放量请求 DTO：目标比例、sourceTag、targetTag、执行模式。
- [x] 1.2.1 保持 `ProjectPercentageRoutingRequest.channelCode` 为字符串类型，默认值为 `BS`。
- [x] 1.3 新增放量响应 DTO：统计结果含总目标数、已达成数、实际切换数、白名单命中数、黑名单排除数与失败明细。
- [x] 1.4 在 `api-project-op` 新增白名单/黑名单管理接口（增/删/查，按 targetTag 维度区分）。
- [x] 1.5 保留并回归验证现有 `setTagByProject` 接口行为不变。

## 2. Redis 名单管理

- [x] 2.1 定义 Redis key 规范：白名单 `project:routing:whitelist:{targetTag}`，黑名单 `project:routing:blacklist:{targetTag}`，类型为 Set，不设过期时间。
- [x] 2.2 实现白名单增删查 Service 方法，操作 Redis Set。
- [x] 2.3 实现黑名单增删查 Service 方法，操作 Redis Set。
- [x] 2.4 实现对应 ResourceImpl，接入 1.4 定义的管理接口。

## 3. 核心路由选择与执行

- [x] 3.1 在 `biz-project` 实现基于 `projectId` 的确定性哈希分桶算法（`hash(projectId) % 10000`，与项目总数无关）。
- [x] 3.2 实现比例集合计算逻辑，保证比例提升时集合单调扩展（如 3% -> 5%）。
- [x] 3.3 放量执行前从 Redis 读取当前白名单与黑名单，应用优先级：黑名单 > 白名单 > 哈希分桶。
- [x] 3.4 实现 dry-run 逻辑，仅返回目标集合统计（含白名单命中数与黑名单排除数），不落库。
- [x] 3.5 实现 execute 逻辑，按批更新项目 tag，并输出完整统计（目标数/已达成数/实际切换数/白名单数/黑名单数/失败数）。
- [x] 3.6 增加幂等处理：重复执行同参数时跳过已在目标 tag 的项目（白名单项目同样适用）。

## 4. 回滚与可观测能力

- [x] 4.1 支持 source/target 反向执行，满足按比例快速回滚。
- [x] 4.2 增加执行过程日志与审计字段，确保每次放量可追踪。
- [x] 4.3 输出阶段执行摘要，便于发布平台串联 1%/3%/5%/10%/.../100% 流程。

