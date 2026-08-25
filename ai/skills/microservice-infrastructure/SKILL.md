---
name: microservice-infrastructure
description: 处理 BK-CI 微服务底层基础设施时使用，例如条件配置、事件驱动、服务间通信、国际化与日志规范。当用户要搭多环境配置、异步事件、Feign 调用或统一 i18n/日志时优先使用。
---

# 微服务基础设施

## 适用场景

- 配置多环境、多 profile、特性开关
- 实现事件驱动或 MQ 异步处理
- 做服务间通信、Feign 调用、降级和超时控制
- 处理国际化文案和日志规范

## 不适用场景

- 只是 AOP、分布式锁、重试、参数校验这类横切实践
- 只是使用 JWT、表达式解析、线程池等工具组件
- 只是某个模块自己的业务逻辑

## 快速指导

1. 这个 skill 关注的是“微服务底座能力”，不是单模块业务规则。
2. 如果问题更像“服务怎么互相通信、配置怎么分环境、事件怎么解耦”，通常落在这里。
3. 先按主题进入对应参考文档：
   - 条件配置：`reference/1-conditional-config.md`
   - 事件驱动：`reference/2-event-driven.md`
   - 服务间通信：`reference/3-service-communication.md`
   - 国际化与日志：`reference/4-i18n-logging.md`
4. 如果你要的是框架级横切实践，切到 `common-technical-practices`；如果你要的是具体工具类，切到 `utility-components`。

## 高信号规则

- 多环境配置、事件、Feign、日志/i18n 都属于平台级基础设施
- 这类能力的核心是稳定性、可维护性和跨模块一致性
- 服务间调用要同时考虑超时、降级、循环依赖和可观测性
- 事件驱动要优先考虑幂等和异步一致性

## 关键陷阱

- 把微服务基础设施和横切实践混在一起
- 只写调用逻辑，不处理超时、降级和链路追踪
- 只接入 MQ，不处理幂等、重放和事件顺序
- 日志和 i18n 没有统一规范，导致跨模块行为割裂

## 延伸阅读

- 条件配置：`reference/1-conditional-config.md`
- 事件驱动：`reference/2-event-driven.md`
- 服务间通信：`reference/3-service-communication.md`
- 国际化与日志：`reference/4-i18n-logging.md`
- 如果你需要横切实践：再看 `common-technical-practices`
- 如果你需要工具组件：再看 `utility-components`
