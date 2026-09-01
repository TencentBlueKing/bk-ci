---
name: utility-components
description: 使用 BK-CI 中的具体工具组件时使用，例如 JWT、安全认证、表达式解析、线程池循环工具和责任链实现。当用户要直接复用这些组件而不是设计框架级实践时优先使用。
---

# 工具组件

## 适用场景

- 使用 JWT 或认证相关工具
- 解析变量表达式、条件表达式或自定义函数
- 使用线程池、批量处理、循环重试类工具
- 实现或复用责任链模式组件

## 不适用场景

- 只是框架级横切实践，例如 AOP、分布式锁、监控、审计
- 只是服务间通信、事件驱动或多环境配置
- 只是某个业务模块自己的数据结构或流程

## 快速指导

1. 这个 skill 关注的是“具体工具怎么用”，不是“框架实践怎么落地”。
2. 如果问题更像“有没有现成组件可以直接复用”，通常落在这里。
3. 先按主题进入对应参考文档：
   - JWT：`reference/1-jwt-security.md`
   - 表达式解析：`reference/2-expression-parser.md`
   - 线程池与循环工具：`reference/3-thread-pool-loop-util.md`
   - 责任链：`reference/4-chain-responsibility.md`
4. 如果你想解决的是横切架构问题，切到 `common-technical-practices`；如果你想解决的是微服务底座问题，切到 `microservice-infrastructure`。

## 高信号规则

- 这里更偏“组件级复用”，不是“平台级框架方案”
- 工具组件应该优先复用现有实现，而不是重复造轮子
- 责任链在这里更偏具体组件和实现，而不是模式百科
- 用组件前先确认它解决的是业务问题，还是只是让代码看起来更复杂

## 关键陷阱

- 把组件问题和框架实践问题混在一起
- 明明已有现成工具，却重新造一套
- 只会调用工具类，不理解输入输出和边界条件
- 把责任链、线程池、表达式解析滥用到不需要的地方

## 延伸阅读

- JWT：`reference/1-jwt-security.md`
- 表达式解析：`reference/2-expression-parser.md`
- 线程池与循环工具：`reference/3-thread-pool-loop-util.md`
- 责任链：`reference/4-chain-responsibility.md`
- 如果你需要横切实践：再看 `common-technical-practices`
- 如果你需要微服务底座：再看 `microservice-infrastructure`
