---
name: pipeline-model-architecture
description: BK-CI 流水线核心模型（Model）架构详解，涵盖 Pipeline/Stage/Container/Task 四层结构、模型序列化、版本管理、模型校验。当用户理解流水线数据结构、开发流水线功能、处理模型转换或进行模型扩展时使用。
---

# BK-CI 流水线核心模型 (Model) 架构

## 概述

Model 是 BK-CI 流水线系统的核心数据模型，采用**四层嵌套**树状结构：

```
Model (流水线)
  └── Stage[] (阶段)
        └── Container[] (容器/Job)
              └── Element[] (插件/任务)
```

所有流水线操作（创建、编辑、执行、调度）围绕此模型展开。Model 以 JSON 存储在数据库中，每次修改生成新版本，每次构建保存快照。

## 核心文件位置

```
src/backend/ci/core/common/common-pipeline/src/main/kotlin/com/tencent/devops/common/pipeline/
├── Model.kt                    # Model 顶层定义
├── container/
│   ├── Stage.kt                # Stage 定义
│   ├── Container.kt            # Container 接口
│   ├── TriggerContainer.kt     # 触发容器 (@type = "trigger")
│   ├── VMBuildContainer.kt     # 构建容器 (@type = "vmBuild")
│   └── NormalContainer.kt      # 无编译容器 (@type = "normal")
├── pojo/element/
│   ├── Element.kt              # Element 抽象类
│   ├── ElementAdditionalOptions.kt
│   ├── trigger/                # 触发器: ManualTrigger, TimerTrigger, WebHook 等
│   ├── agent/                  # 有编译环境: LinuxScript, CodeGit 等
│   └── market/                 # 市场插件: MarketBuildAtomElement 等
└── option/
    ├── JobControlOption.kt
    └── StageControlOption.kt
```

---

## 四层结构快速参考

### Model

- `stages: List<Stage>` — 核心字段
- `getTriggerContainer()` — 返回 `stages[0].containers[0] as TriggerContainer`
- `Model.defaultModel(name, userId)` — 创建最小化模型（仅手动触发器）

### Stage

- `containers: List<Container>` — 核心字段
- `checkIn / checkOut: StagePauseCheck?` — 准入准出人工审核
- `finally: Boolean` — FinallyStage 标识（最多 1 个，必须在末尾，始终执行）
- `stageControlOption: StageControlOption?` — 流程控制
- ID 格式: `stage-{seq}`（系统生成）

### Container（三种实现）

| 类型 | @type | 用途 | 关键字段 |
|------|-------|------|---------|
| TriggerContainer | `trigger` | 触发器 + 全局参数 | `params`, `buildNo` |
| VMBuildContainer | `vmBuild` | 有编译环境 Job | `baseOS`, `dispatchType`, `jobControlOption`, `mutexGroup`, `matrixControlOption` |
| NormalContainer | `normal` | 无编译环境 Job | `jobControlOption`, `mutexGroup` |

- TriggerContainer 固定在 `stages[0].containers[0]`，不可删除
- Container 使用 `@JsonTypeInfo` + `@JsonSubTypes` 实现多态序列化

### Element

- 通过 `@type` 字段区分子类型（`manualTrigger`, `linuxScript`, `marketBuild` 等）
- `additionalOptions: ElementAdditionalOptions?` — 超时、重试、运行条件、Post 任务
- `stepId` — 用户自定义 ID，用于上下文引用 `steps.myStep.status`

---

## ID 规则

| 字段 | 格式 | 说明 |
|------|------|------|
| Stage.id | `stage-{seq}` | 系统生成，不可编辑 |
| Stage.stageIdForUser | 自定义 | 用户可编辑 |
| Container.id / containerId | 数字字符串 `"1"` | 序列 ID |
| Container.containerHashId | 全局唯一 | 跨构建追踪 |
| Container.jobId | 自定义 | Job 依赖配置 |
| Element.id | `{Stage序号}-{Container序号}-{Element序号}` | 如 `"2-1-3"` |
| Element.stepId | 自定义 | 上下文变量引用 |

---

## 关键业务流程

### 流水线创建

1. 用户提交 Model
2. **验证**: `modelCheckPlugin.checkModelIntegrity(model)` 校验完整性
3. 生成 pipelineId，序列化 Model 为 JSON
4. 存入 `T_PIPELINE_RESOURCE_VERSION` 表

### 流水线启动

1. 从 `T_PIPELINE_RESOURCE_VERSION` 获取 Model
2. 初始化运行时状态（`resetBuildOption`, `initStatus`）
3. 保存构建快照到 `T_PIPELINE_BUILD_RECORD_MODEL`
4. 逐 Stage 调度 → Container 调度 → Element 执行
5. 状态流转: `QUEUE → PREPARE_ENV → RUNNING → SUCCEED/FAILED`

### 版本兼容性

每层调用 `transformCompatibility()` 处理历史数据（如 `timeout` → `timeoutVar` 迁移）。

---

## Model 操作工作流

### 创建或修改 Model

```
1. 构造 Model 对象（或使用 Model.defaultModel()）
2. 确保 stages[0].containers[0] 为 TriggerContainer
3. 为每个 Stage 添加 Container 和 Element
   ✓ 验证: TriggerContainer 在首位
   ✓ 验证: FinallyStage（如有）在末尾且唯一
   ✓ 验证: ID 不重复
4. 提交 → modelCheckPlugin.checkModelIntegrity() 校验
5. 持久化到 T_PIPELINE_RESOURCE_VERSION
```

### 遍历和修改 Element

```
1. model.stages.forEach → stage.containers.forEach → container.elements.forEach
2. 修改 Element 属性
   ✓ 验证: 编排阶段不设置运行时字段（status, executeCount）
   ✓ 验证: 优先使用 timeoutVar 而非 timeout
3. 持久化更新
```

### 构建矩阵操作

```
1. 在 VMBuildContainer/NormalContainer 上设置 matrixControlOption
2. 配置 strategyStr（JSON/YAML 格式参数矩阵）
3. 引擎自动展开: 笛卡尔积 + include - exclude
4. 生成 groupContainers（最多 256 个）
   ✓ 验证: maxConcurrency 控制并发
   ✓ 验证: 父容器 matrixGroupFlag = true
   ✓ 验证: 子容器设置 matrixGroupId 和 matrixContext
```

---

## 检查清单

操作 Model 时确认:

- [ ] 至少包含一个 Stage，首 Stage 首 Container 为 TriggerContainer
- [ ] 同层级 ID 不重复
- [ ] FinallyStage 最多一个且在最后
- [ ] 编排阶段不设置 `status`、`executeCount` 等运行时字段
- [ ] 优先使用 `timeoutVar`（支持变量）而非 `timeout`
- [ ] `JobControlOption.dependOnId` 引用的 JobID 存在
- [ ] 矩阵父容器不放实际任务，任务在子容器中
- [ ] TriggerContainer `params` 校验 `required`、类型约束
- [ ] Model JSON 大小不超过 4MB
- [ ] Stage 数 ≤ 20，每 Stage Job 数 ≤ 20，每 Job Element 数 ≤ 50

---

## 详细参考

- [模型完整定义与辅助类型](reference/model-reference.md) — 所有数据结构、枚举值、校验机制
- [数据库 Schema 与持久化](reference/database-schema.md) — 表结构、DAO、Service 层、执行引擎
- [JSON 示例与代码示例](reference/examples.md) — 完整 JSON、Kotlin 操作、扩展指南、调试方法
