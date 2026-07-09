---
name: common-technical-practices
description: 处理 BK-CI 后端中的横切技术实践时使用，例如 AOP、分布式锁、重试、参数校验、性能监控、定时任务和审计日志。当用户要实现这类框架级实践而不是具体业务功能时优先使用。
---

# 通用技术实践

## 适用场景

- 添加 AOP 切面
- 使用分布式锁控制并发
- 配置重试机制与退避策略
- 做接口参数校验
- 添加性能监控埋点
- 编写定时任务
- 记录审计日志

## 不适用场景

- 只是使用某个具体工具类，例如 JWT、表达式解析、线程池工具
- 只是微服务之间的调用、事件驱动或多环境配置
- 只是某个模块自己的业务规则

## 快速指导

1. 这个 skill 关注的是“框架级横切实践”，不是具体业务模块。
2. 如果问题更像“给业务逻辑增加一层通用能力”，通常落在这里。
3. 先按主题进入对应参考文档：
   - AOP：`reference/1-aop-aspect.md`
   - 分布式锁：`reference/2-distributed-lock.md`
   - 重试：`reference/3-retry-mechanism.md`
   - 参数校验：`reference/4-parameter-validation.md`
   - 性能监控：`reference/5-performance-monitoring.md`
   - 定时任务：`reference/6-scheduled-tasks.md`
   - 审计日志：`reference/7-audit-logging.md`
4. 如果你要的是“某个现成组件怎么用”，切到 `utility-components`；如果你要的是“微服务基础能力怎么搭”，切到 `microservice-infrastructure`。

## 高信号规则

- AOP、锁、重试、监控、审计这类能力通常跨多个模块复用
- 这类实践往往和 Spring / 框架集成方式强相关
- 落地时要优先考虑幂等性、可观测性和并发安全
- 不要把横切能力写死在单个业务类里

## 关键陷阱

- 把工具类使用问题误放到这里
- 只实现功能，不处理幂等、超时、并发和可观测性
- 重试、锁、定时任务直接上，而不先判断是否真的需要
- 把审计日志、监控埋点散落在业务代码里，后续难以维护

## 延伸阅读

- AOP：`reference/1-aop-aspect.md`
- 分布式锁：`reference/2-distributed-lock.md`
- 重试：`reference/3-retry-mechanism.md`
- 参数校验：`reference/4-parameter-validation.md`
- 性能监控：`reference/5-performance-monitoring.md`
- 定时任务：`reference/6-scheduled-tasks.md`
- 审计日志：`reference/7-audit-logging.md`
- 如果你需要的是工具组件：再看 `utility-components`
- 如果你需要的是微服务底座：再看 `microservice-infrastructure`
