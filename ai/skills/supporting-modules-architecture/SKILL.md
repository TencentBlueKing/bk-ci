---
name: supporting-modules-architecture
description: 处理 BK-CI 支撑服务模块时使用，例如 Ticket、Environment、Notify、Log、Quality、OpenAPI 等能力的定位、边界和入口选择。当用户要判断该去哪个支撑模块而不是修改核心流水线链路时优先使用。
---

# 支撑模块架构

## 适用场景

- 判断某个需求属于 Ticket、Environment、Notify、Log、Quality 还是 OpenAPI
- 理解支撑模块与核心业务模块的边界
- 为支撑性需求选择正确模块入口
- 排查核心模块调用支撑服务时的责任归属

## 不适用场景

- 直接修改 Process 主链路
- 直接修改 Auth、Dispatch、Worker、Agent 等核心平台模块
- 只是处理某个支撑模块的细节实现，但模块归属已经明确

## 快速指导

1. 这个 skill 主要是“模块路由器”，帮助判断支撑性能力该落在哪个模块。
2. 支撑模块通常提供凭证、环境、通知、日志、质量和开放接口能力，但不负责流水线主编排。
3. 先按能力进入已有参考文档：
   - Ticket：`reference/1-ticket-module.md`
   - Environment：`reference/2-environment-module.md`
   - Notify：`reference/3-notify-module.md`
   - Log：`reference/4-log-module.md`
   - Quality：`reference/5-quality-module.md`
   - OpenAPI：`reference/6-openapi-module.md`
4. 如果问题已经明显属于流水线主链路，切到 `process-module-architecture`。
5. 如果问题涉及权限、调度、执行或制品等核心平台模块，也应优先切对应模块 skill。

## 高信号规则

- 支撑模块更像能力底座，被核心模块调用而不是主导执行链路
- 先判断需求是“业务主链的一部分”，还是“被多个模块复用的支撑能力”
- 这类模块常见误区不是实现难，而是归属判断错
- 边界比细节更重要，先选对模块再下钻

## 关键陷阱

- 把核心流程问题误归到支撑模块
- 因为多个模块都会调用，就误以为它属于 Process 或 Auth
- 在模块边界未明确前，就直接深入某个子模块实现

## 延伸阅读

- Ticket：`reference/1-ticket-module.md`
- Environment：`reference/2-environment-module.md`
- Notify：`reference/3-notify-module.md`
- Log：`reference/4-log-module.md`
- Quality：`reference/5-quality-module.md`
- OpenAPI：`reference/6-openapi-module.md`
- 如果你需要看核心流水线链路：再看 `process-module-architecture`
- 如果你需要看全局模块协作：再看 `00-bkci-global-architecture`
