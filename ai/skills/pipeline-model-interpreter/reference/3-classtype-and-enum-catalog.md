# classType 与枚举速查清单

这个 reference 收录解释 Model JSON 时高频、易错的**真实字面量**，用于快速识别节点类型与运行资源。字面量以源码为准，注意大小写。

## Element `@type`（classType）全量速查

### 脚本类

| `@type` | 说明 |
|---|---|
| `linuxScript` | Linux/macOS Shell 脚本，脚本在 `script` 字段 |
| `windowsScript` | Windows BAT/PowerShell 脚本，脚本在 `script` 字段 |

### 代码拉取类（注意全大写）

| `@type` | 说明 |
|---|---|
| `CODE_GIT` | 拉取工蜂/Git 代码库 |
| `CODE_GITLAB` | 拉取 GitLab 代码库 |
| `CODE_SVN` | 拉取 SVN 代码库 |
| `GITHUB` | 拉取 GitHub 代码库 |

### 触发器类（解释主流程时默认忽略，仅用于识别）

`manualTrigger` / `remoteTrigger` / `timerTrigger` / `codeGitWebHookTrigger` /
`codeGitlabWebHookTrigger` / `codeSVNWebHookTrigger` / `codeGithubWebHookTrigger` /
`codeTGitWebHookTrigger` / `codeP4WebHookTrigger` 等。

> 触发器只放在 `TriggerContainer` 中，代表“怎么被触发”，不是主业务步骤。除非用户明确问触发方式，否则不展开。

### 商店插件类

| `@type` | 说明 |
|---|---|
| `marketBuild` | 有编译环境的研发商店插件 |
| `marketBuildLess` | 无编译环境的研发商店插件 |
| `marketCheckImage` | 镜像检查插件 |
| `marketEvent` | 事件类商店插件 |

### 质量红线类

| `@type` | 说明 |
|---|---|
| `qualityGateInTask` | 质量红线准入卡点 |
| `qualityGateOutTask` | 质量红线准出卡点 |

### 审核 / 子流水线 / 矩阵 / 模板

| `@type` | 说明 |
|---|---|
| `manualReviewUserTask` | 人工审核，暂停等待指定人确认 |
| `subPipelineCall` | 调用子流水线/子创作流 |
| `matrixStatus` | 矩阵拆分后的子任务占位状态节点（运行态产物，非用户配置） |
| `stepTemplate` | 步骤模板引用 |

### 归档 / 通知等 SPI 插件

| `@type` | 说明 |
|---|---|
| `buildArchiveGet` | 拉取构建产物 |
| `singleArchive` | 单文件归档 |
| `reportArchive` | 报告归档 |
| `customizeArchiveGet` | 自定义仓库产物拉取 |
| `buildPushDockerImage` | 推送 Docker 镜像 |
| `sendRTXNotify` | 企业微信/RTX 通知 |
| `sendEmailNotify` | 邮件通知 |
| `sendSmsNotify` | 短信通知 |
| `sendWechatNotify` | 微信通知 |

### 兜底类型

| `@type` | 说明 |
|---|---|
| `unknownType` | `EmptyElement`，无法识别或未注册的插件类型 |

> 遇到 `unknownType` 或不在本清单中的 classType：保守表述“该节点为未识别/自定义插件类型，功能需结合插件文档确认”，只描述能确认的 `name`、`atomCode`、关键输入输出，不臆测用途。

## Container `@type`

| `@type` | 容器类 | 说明 |
|---|---|---|
| `trigger` | `TriggerContainer` | 触发容器，承载启动参数与触发器，只出现在第一个 Stage |
| `vmBuild` | `VMBuildContainer` | 有编译环境的 Job，运行在构建机上 |
| `normal` | `NormalContainer` | 无编译环境的 Job，运行在蓝盾后台微服务侧 |
| `jobTemplate` | `JobTemplateContainer` | Job 模板引用容器 |

## dispatchType `buildType`（Job 跑在什么构建资源上）

`VMBuildContainer.dispatchType.buildType` 决定 Job 的构建资源来源：

| `buildType` | 构建资源 | `value` 常见含义 |
|---|---|---|
| `DOCKER` | 蓝盾公共 Docker 构建机 | 镜像版本/镜像名 |
| `KUBERNETES` | Kubernetes 构建集群 | 镜像信息 |
| `THIRD_PARTY_AGENT_ID` | 指定的第三方单机构建机 | Agent 显示名/ID |
| `THIRD_PARTY_AGENT_ENV` | 第三方构建集群（环境） | envName/环境名 |
| `THIRD_PARTY_DEVCLOUD` | 第三方 DevCloud 资源 | 资源标识 |
| `CREATE_AGENT_ENV` | 创作流集群 | 环境标识 |

> `dispatchType.value` 在不同子类里含义不同（镜像版本 / envName / displayName），解释时要结合 `buildType` 判断，不要一律说成“镜像”。

## VMBaseOS（构建机操作系统）

`VMBuildContainer.baseOS` 取值：`LINUX` / `MACOS` / `WINDOWS` / `ALL`。
