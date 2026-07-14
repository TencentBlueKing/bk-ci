# Model JSON 解析手册

这个 reference 说明：当用户直接贴出 BK-CI 流水线或创作流的 `Model` JSON 时，AI 应该如何解释它的业务含义。

## 总原则

- 创作流与流水线使用相同的 `Model -> Stage -> Container -> Element` 四层结构
- 默认只解释**配置态**，忽略 `status`、`startEpoch`、`elapsed`、`executeCount`、`timeCost` 等运行态字段
- 不做修改建议，只解释“这个编排在定义什么”
- 对未知 `atomCode` 保守表述，不要臆测

## 默认分析顺序

1. 看 `Model` 顶层元信息
2. 找第一个 `Stage` 中的 `TriggerContainer.params`
3. 逐个 `Stage` 看流程边界和控制项
4. 逐个 `Container` 看 Job 类型、执行环境和控制项
5. 逐个 `Element` 看任务类型、输入、输出和插件级控制项
6. 最后再综合说明整体流程、关键输入和可能产出

## 1. 顶层 Model 先看什么

优先关注：

- `name`：流水线/创作流名称
- `desc`：描述
- `stages`：核心编排主体
- `instanceFromTemplate`：是否模板实例化
- `templateId` / `srcTemplateId`：模板来源
- `pipelineCreator`：创建人
- `latestVersion`：最新版本号

解释时可先回答：

- 这条编排整体是做什么的
- 是否来自模板
- 主要流程有几个阶段

## 2. TriggerContainer 只重点看启动参数

`TriggerContainer` 通常位于第一个 `Stage` 的第一个 `Container`。

解释重点：

- `params`：启动参数列表
- `templateParams`：模板参数
- `buildNo`：构建版本号规则

默认**不把触发器插件**（如 `manualTrigger`、`remoteTrigger`、`timerTrigger`、Webhook 触发器）当成主业务步骤解释，除非用户明确问“怎么触发”。

### 参数解释规则

每个 `BuildFormProperty` 重点看：

- `id`：参数变量名
- `name`：显示名称
- `type`：参数类型
- `required`：是否必填
- `defaultValue`：默认值
- `desc`：描述
- `options`：枚举或多选选项
- `sensitive`：是否敏感
- `constant`：是否常量

处理要求：

- `sensitive=true` 或 `type=PASSWORD` 时，只说“敏感参数”，不回显默认值
- 启动参数过多时，优先保留必填项、敏感项、关键业务参数

## 3. Stage 怎么解释

每个 `Stage` 重点看：

- `id` / `name`
- `finally`
- `stageControlOption`
- `checkIn` / `checkOut`
- `fastKill`
- `containers`

解释要点：

- `finally=true`：这是 Finally Stage，无论前面成功或失败，通常都会执行
- `stageControlOption.enable=false`：该阶段已禁用
- `checkIn` / `checkOut`：表示阶段前后存在审核或门禁
- `fastKill=true`：某些失败场景会快速终止该阶段下剩余任务

## 4. Container 怎么解释

先按 `@type` 判断容器类型，再看环境和控制项。

### `trigger`

- 触发容器
- 主要意义是承载启动参数和触发入口
- 解释主流程时通常弱化

### `vmBuild`

- 有编译环境的 Job，运行在构建机上
- 重点看：
  - `baseOS`
  - `dispatchType`
  - `thirdPartyAgentId` / `thirdPartyAgentEnvId`
  - `matrixControlOption`
  - `jobControlOption`
  - `mutexGroup`

### `normal`

- 无编译环境的 Job，通常运行在蓝盾后台微服务侧
- 重点看：
  - `jobControlOption`
  - `mutexGroup`
  - `matrixControlOption`

### Job 通用解释点

- `jobControlOption.enable=false`：Job 已禁用
- `matrixControlOption`：表示这是矩阵 Job，会按维度拆成多个并行子 Job
- `mutexGroup`：表示该 Job 受互斥组约束
- `containerHashId`、`jobId`、`id/containerId` 是不同维度的定位键，不要混说成同一个“Job ID”

## 5. Element 怎么解释

解释单个插件时，至少同时检查这些字段：

- `@type`
- `name`
- `id`
- `stepId`
- `version`
- `additionalOptions`
- 对商店插件额外看：`atomCode`、`data.input`、`data.output`

### 插件解释顺序

1. 它是什么类型的插件
2. 它在当前 Job 里承担什么作用
3. 它依赖哪些关键输入
4. 它可能产生哪些输出
5. 它是否禁用、限时、可重试、失败继续

### `additionalOptions` 解释规则

重点看：

- `enable`
- `continueWhenFailed`
- `retryWhenFailed`
- `retryCount`
- `timeout`
- `runCondition`

处理要求：

- `enable=false`：明确标注该插件已禁用
- 运行条件、失败策略、超时、重试应作为解释中的补充语义，而不是主功能本身

## 6. 商店插件如何保守解释

对于 `marketBuild` 和 `marketBuildLess`：

- `atomCode` 用于标识具体插件
- `data.input` 解释它“做什么、怎么做”
- `data.output` 解释它“产出什么变量或结果”
- `namespace` 用于理解输出变量前缀

### 已知插件

如果 `atomCode` 是你明确知道的常见插件，可以直接说明用途。

### 未知插件

如果 `atomCode` 不熟悉：

- 先标注“自定义或未知商店插件，功能需结合插件文档确认”
- 再列出最关键的 `data.input` / `data.output`
- 不要因为名字像某类插件就直接下结论

## 7. 如何推断整体流程

用户问“这个编排整体做什么”时，可按下面顺序组织回答：

1. 启动时需要什么参数
2. 主流程有哪些阶段
3. 每个关键 Job 负责什么
4. 是否有审核、矩阵、Finally、子流水线、质量门禁
5. 最终可能产出什么

### 常见流程模式

- 代码拉取 + 编译/打包 + 归档：偏构建流水线
- 代码拉取 + 测试 + 质量红线：偏测试流水线
- 构建 + 部署 + 审核：偏 CI/CD / 发布流水线
- 定时触发 + 脚本：偏定时任务流水线

这些模式只用于**辅助归纳**，不是硬规则；最终仍以实际插件和参数为准。

## 8. 运行条件与依赖枚举（三层各自独立）

Stage / Job / Element 各有**独立**的运行条件枚举，字面量不通用，解释执行时机时不要混用。

### Element 级：`additionalOptions.runCondition`（`RunCondition`）

| 枚举 | 含义 |
|---|---|
| `PRE_TASK_SUCCESS` | 前置任务成功时才执行（默认） |
| `PRE_TASK_FAILED_BUT_CANCEL` | 前置失败但未取消时执行 |
| `PRE_TASK_FAILED_EVEN_CANCEL` | 前置失败即使取消也执行 |
| `PRE_TASK_FAILED_ONLY` | 仅前置失败时执行 |
| `CUSTOM_VARIABLE_MATCH` | 自定义变量满足条件时执行 |
| `CUSTOM_VARIABLE_MATCH_NOT_RUN` | 自定义变量满足条件时不执行 |
| `CUSTOM_CONDITION_MATCH` | 自定义表达式匹配时执行 |
| `PARENT_TASK_CANCELED_OR_TIMEOUT` | 父任务取消或超时时执行 |
| `PARENT_TASK_FINISH` | 父任务结束时执行 |

### Job 级：`jobControlOption.runCondition`（`JobRunCondition`）

| 枚举 | 含义 |
|---|---|
| `STAGE_RUNNING` | 所在 Stage 运行即执行（默认） |
| `PREVIOUS_STAGE_SUCCESS` | 上一 Stage 成功时执行 |
| `PREVIOUS_STAGE_FAILED` | 上一 Stage 失败时执行 |
| `PREVIOUS_STAGE_CANCEL` | 上一 Stage 取消时执行 |
| `CUSTOM_VARIABLE_MATCH` / `CUSTOM_VARIABLE_MATCH_NOT_RUN` | 自定义变量匹配/不匹配时执行 |
| `CUSTOM_CONDITION_MATCH` | 自定义表达式匹配时执行 |

### Stage 级：`stageControlOption.runCondition`（`StageRunCondition`）

| 枚举 | 含义 |
|---|---|
| `AFTER_LAST_FINISHED` | 上一阶段完成后执行（默认） |
| `CUSTOM_VARIABLE_MATCH` / `CUSTOM_VARIABLE_MATCH_NOT_RUN` | 自定义变量匹配/不匹配时执行 |
| `CUSTOM_CONDITION_MATCH` | 自定义表达式匹配时执行 |

### Job 间依赖：`jobControlOption`

- `dependOnType`：依赖定位方式，`ID`（按 jobId）或 `NAME`（按 Job 名）
- `dependOnId`：按 ID 依赖时的目标 jobId 列表
- `dependOnName`：按 NAME 依赖时的目标 Job 名
- 用于解释同一 Stage 内多个 Job 的先后执行顺序，而不是并行

## 9. 输出边界

默认不要输出：

- 代码修改建议
- 配置优化建议
- 对未知插件的武断判断
- 大段照抄 JSON

默认应该说明：

- 哪部分信息可以明确确认
- 哪部分信息只能保守推断
- 如果用户只问某个节点，就只解释该节点及必要上下文
