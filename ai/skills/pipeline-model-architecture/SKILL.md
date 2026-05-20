---
name: pipeline-model-architecture
description: 理解或修改 BK-CI 流水线 Model、Stage、Container、Element 结构时使用。当用户提到流水线模型、Stage/Job/Task 数据结构、模型扩展、模型校验、版本兼容、YAML 与 Model 转换时优先使用。
---

# BK-CI 流水线模型架构

## 适用场景

- 理解 `Model -> Stage -> Container -> Element` 的层级关系
- 修改流水线模型字段、结构或校验逻辑
- 新增容器类型、插件类型或模型辅助结构
- 排查模型序列化、版本兼容、YAML 转换问题
- 判断一个需求应该落在模型定义、运行时状态还是持久化层

## 不适用场景

- 只是调度资源分配、构建机执行或插件运行问题
- 只是普通后端接口开发，不涉及流水线模型本身
- 只是数据库脚本设计，不涉及模型结构和模型存储

## 快速指导

1. 先把问题归到下面 5 类之一，再继续读对应参考文档：
   - 结构定义：看 `reference/1-core-structure.md`
   - 持久化与版本：看 `reference/2-persistence-versioning.md`
   - 运行时、YAML、校验、扩展：看 `reference/3-runtime-conversion-extension.md`
2. `Model` 是流水线配置的核心载体，但不是所有运行时状态的权威来源。运行中的状态、耗时、重试等字段要区分“配置态”和“运行态”。
3. 触发容器是模型入口，默认位于第一个 `Stage` 的第一个 `Container`。很多参数、触发器和启动逻辑都从这里进入。
4. `Container` 和 `Element` 都是多态结构。新增类型时，不能只改一个类定义，通常还要一起检查：
   - Jackson 多态序列化配置
   - 前端编排模型
   - 后端转换与校验
   - 运行引擎或 Worker 识别逻辑
5. 版本兼容和 YAML 转换要一起考虑。模型字段一旦进入存量数据，就不能只从“当前新建流程”角度看。
6. 如果你修改的是流程控制、执行条件、Finally、Matrix 之类能力，优先确认它影响的是 Stage、Job 还是 Element 层。

## 高信号规则

- `Model` 关心的是“流水线如何定义”，`Process` 更关心“流水线如何被编排执行”
- `Stage` 表达阶段边界和流程控制，`Container` 更接近 Job，`Element` 更接近具体任务或插件
- `id`、`stageIdForUser`、插件 code、运行时状态字段，不是同一层次的概念，不能混用
- 只要涉及模型新增字段，就要同时检查序列化、兼容、默认值和历史版本读取

## 关键陷阱

- 把运行时字段直接当成持久化配置字段来修改，导致旧构建详情或历史快照异常
- 只改 `Element`/`Container` 类定义，不补多态声明、校验或前端侧映射
- 忽略 Finally、Matrix、运行条件这些特殊结构，按普通 Stage/Job 处理
- 只验证 UI 新建流程，没验证历史 Model 回放、重试、版本切换和 YAML 转换
- 看到问题出现在模型字段，就直接改模型，实际上问题可能在 `Process` 的执行链或转换层

## 延伸阅读

- 核心结构：`reference/1-core-structure.md`
- 持久化与版本：`reference/2-persistence-versioning.md`
- 运行时、转换与扩展：`reference/3-runtime-conversion-extension.md`
- 涉及执行链路归属时：先看 `process-module-architecture`
- 涉及 YAML/PAC 时：再看 `yaml-pipeline-transfer`
