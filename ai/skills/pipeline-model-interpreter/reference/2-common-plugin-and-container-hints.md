# 容器、插件与参数速查

这个 reference 提供解释 Model JSON 时最常用的速查信息。

## Container 类型

| `@type` | 容器类型 | 解释重点 |
|---|---|---|
| `trigger` | `TriggerContainer` | 启动参数 `params`、模板参数 `templateParams`、构建号 `buildNo` |
| `vmBuild` | `VMBuildContainer` | 有编译环境；关注 `baseOS`、`dispatchType`、第三方构建机、矩阵、Job 控制 |
| `normal` | `NormalContainer` | 无编译环境；关注 `jobControlOption`、`mutexGroup`、矩阵 |

## Stage 级常见控制

| 字段 | 含义 |
|---|---|
| `finally` | Finally Stage，通常用于收尾、通知、清理 |
| `stageControlOption.enable` | 阶段是否启用 |
| `checkIn` | 阶段准入审核/门禁 |
| `checkOut` | 阶段准出审核/门禁 |
| `fastKill` | 快速终止策略 |

## Job 级常见控制

| 字段 | 含义 |
|---|---|
| `jobControlOption.enable` | Job 是否启用 |
| `jobControlOption.timeout` | Job 超时控制 |
| `jobControlOption.runCondition` | Job 运行条件 |
| `mutexGroup` | 互斥组 |
| `matrixControlOption` | 矩阵配置 |

## Element 级常见控制

| 字段 | 含义 |
|---|---|
| `additionalOptions.enable` | 插件是否启用 |
| `continueWhenFailed` | 失败是否继续 |
| `retryWhenFailed` | 失败是否自动重试 |
| `retryCount` | 重试次数 |
| `timeout` | 插件超时 |
| `runCondition` | 插件运行条件 |

## 常见内置插件

### 构建环境类插件

| `@type` | 常见作用 |
|---|---|
| `linuxScript` | 执行 Shell/Bash 脚本 |
| `windowsScript` | 执行 BAT/PowerShell 脚本 |
| `CODE_GIT` / `CODE_GITLAB` / `CODE_SVN` / `GITHUB` | 从代码仓库拉取代码（classType 为大写） |
| `manualReviewUserTask` | 人工审核，暂停等待指定人员确认 |

### 商店插件

| `@type` | 常见作用 |
|---|---|
| `marketBuild` | 有编译环境的研发商店插件，重点看 `atomCode`、`data.input`、`data.output` |
| `marketBuildLess` | 无编译环境的研发商店插件，重点看 `atomCode`、`data.input`、`data.output` |

### 其他常见插件

| `@type` | 常见作用 |
|---|---|
| `subPipelineCall` | 调用子流水线/子创作流 |
| `qualityGateInTask` / `qualityGateOutTask` | 质量红线准入/准出 |
| `marketCheckImage` | 镜像检查 |
| `stepTemplate` | 步骤模板引用 |

## 商店插件解释要点

解释 `marketBuild` / `marketBuildLess` 时优先看：

1. `atomCode`：插件标识
2. `version`：插件版本
3. `data.input`：输入配置，决定插件做什么
4. `data.output`：输出定义，决定插件产出什么
5. `data.namespace`：输出变量的命名空间

### 未知 `atomCode` 的推荐说法

- `这是一个自定义或未知商店插件，功能需结合插件文档确认。`
- 再补充：
  - `atomCode`
  - 关键 `data.input`
  - 关键 `data.output`

## BuildFormPropertyType 速查

| 类型 | 说明 |
|---|---|
| `STRING` | 字符串 |
| `TEXTAREA` | 多行文本 |
| `ENUM` | 单选枚举 |
| `MULTIPLE` | 多选 |
| `BOOLEAN` | 布尔值 |
| `DATE` | 日期 |
| `LONG` | 长整型数字 |
| `GIT_REF` | Git 分支/Tag |
| `SVN_TAG` | SVN Tag |
| `REPO_REF` | 仓库引用 |
| `CODE_LIB` | 代码库 |
| `CONTAINER_TYPE` | 构建机类型 |
| `ARTIFACTORY` | 制品仓库文件 |
| `SUB_PIPELINE` | 子流水线 |
| `CUSTOM_FILE` | 自定义仓库文件 |
| `PASSWORD` | 密码/敏感字段 |

## 参数解释规则

| 字段 | 解释规则 |
|---|---|
| `required=true` | 必填参数 |
| `constant=true` | 固定值，通常不可改 |
| `sensitive=true` | 敏感参数，不展示默认值 |
| `options` | 枚举或多选选项 |
| `displayCondition` | 参数存在条件显示逻辑 |

## 输出解释边界

### 可以直接确认的输出

- 插件 `data.output` 中显式声明的变量
- 明确的归档/镜像/部署结果字段

### 只能保守推断的输出

- 纯脚本插件的产出
- 仅通过插件名猜测出的制品类型
- 未知商店插件的业务结果

对于这类场景，优先说：

- `根据当前编排可推断可能存在这类输出`
- `具体产出物仍需结合脚本内容或插件文档确认`

## 常见定位键提醒

| 键 | 含义 |
|---|---|
| `stage.id` | 系统阶段 ID |
| `stageIdForUser` | 用户可读阶段 ID |
| `containerHashId` | Job 稳定标识 |
| `jobId` | 用户自定义 Job ID |
| `element.id` | 插件 ID |
| `stepId` | 用户自定义 Step ID |

解释时不要把这些字段统一说成“ID”，最好带上层级说明。

## 审核结构（`checkIn` / `checkOut` = `StagePauseCheck`）

阶段准入 `checkIn` 与准出 `checkOut` 都是 `StagePauseCheck`，用于解释“这个阶段谁审核、审核什么”：

| 字段 | 含义 |
|---|---|
| `manualTrigger` | 是否需要人工审核触发 |
| `reviewGroups` | 审核用户组列表（`StageReviewGroup`） |
| `reviewGroups[].name` | 审核组名称 |
| `reviewGroups[].reviewers` | 审核人列表 |
| `reviewGroups[].groups` | 审核用户组（IAM 组）列表 |
| `reviewGroups[].status` | 该审核组当前状态 |
| `reviewParams` | 审核时可填写的参数（`ManualReviewParam`） |
| `timeout` | 审核超时时间，默认 24 小时 |
| `ruleIds` | 关联的质量红线规则 ID |

## 矩阵结构（`matrixControlOption` = `MatrixControlOption`）

| 字段 | 含义 |
|---|---|
| `strategyStr` | 矩阵维度定义（YAML/JSON 字符串） |
| `includeCaseStr` | 额外追加的组合 |
| `excludeCaseStr` | 需要排除的组合 |
| `maxConcurrency` | 最大并发数，默认 5 |
| `fastKill` | 任一子任务失败是否快速终止其余子任务 |

矩阵 Job 在运行时会按维度**分裂为多个并行子 Job**：

- 原 Job 上带 `matrixGroupFlag=true`
- 子任务用 `matrixStatus` 占位节点表示
- 子任务通过 `matrixGroupId` 归属矩阵组，`matrixContext` 携带当前维度取值

## BuildFormProperty 扩展字段

除基础字段外，解释启动参数时可能遇到：

| 字段 | 含义 |
|---|---|
| `category` | 参数分组/分类 |
| `payload` | 扩展负载数据 |
| `scmType` | 代码库类型（配合 `GIT_REF`/`CODE_LIB` 等类型） |
| `containerType` | 构建机类型信息 |
| `repoHashId` | 关联代码库 hashId |
| `displayCondition` | 条件展示逻辑，满足条件时参数才显示 |
| `valueNotEmpty` | 值是否要求非空 |
| `readOnly` | 是否只读 |

## VMBaseOS

`VMBuildContainer.baseOS` 取值：`LINUX` / `MACOS` / `WINDOWS` / `ALL`。
