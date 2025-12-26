# OpenSpec 安装使用指南

## 结论

在已有项目中，通过 **CodeBuddy + Claude 4.5 + OpenSpec**，可以在显著降低成本的前提下，获得高质量的 AI SDD（规范驱动开发）体验。

## 路线

1. 在项目中初始化 OpenSpec，补全 `openspec/project.md`，让 AI 理解项目的技术栈、架构和约定
2. 每个需求都按照 `/openspec:proposal` → 审查/修订文档 → `/openspec:apply` → `/openspec:archive` 的闭环流程推进

## 环境准备

### 1. 升级 Node.js 环境

OpenSpec 需要 Node 20+，若已经安装可直接跳过此步：

```bash
nvm install 20
nvm use 20
```

### 2. 安装 CodeBuddy-Code 和 OpenSpec

```bash
npm install -g @tencent-ai/codebuddy-code
npm install -g @fission-ai/openspec@latest
```

### 3. 初始化项目

```bash
cd your-project
openspec init            # 初始化 OpenSpec 目录结构
```

生成目录结构：
```
openspec/
├── project.md      # 项目上下文（技术栈、架构、规范）
├── specs/          # 主规范库（已实现功能的规范）
├── changes/        # 变更提案目录
│   └── archive/    # 已归档变更
└── AGENTS.md       # AI 助手指令
```

进入 CodeBuddy 完成登录、模型配置，并让 AI 学习工程背景：

```bash
codebuddy                # 进入命令行界面，首次需登录
/model                   # 切换模型（推荐 Claude-4.5-Opus）
> Please read openspec/project.md and help me fill it out with details about my project, tech stack, and conventions，后续工作请用中文
```

AI 会扫描项目结构、识别代码模式，将分析结果写入 `openspec/project.md`。

## AI SDD 开发流程

```
起草提案 → 审查对齐 → 实施任务 → 归档更新
```

### Stage 1: 起草变更提案

```bash
/openspec:proposal [需求描述]
```

**作用**：AI 理解需求意图，生成提案文档（proposal.md）、技术方案（design.md）、任务清单（tasks.md）、规范增量（specs/）。

如果需求描述不够清晰，AI 会主动追问补充信息。

### Stage 2: 审查文档与双向对齐

阅读生成的文档，对问题进行修订，然后要求 AI 刷新：

```bash
/openspec:proposal [对修改的描述]
```

循环修订直到提案和设计都审查通过。

### Stage 3: 实施任务

```bash
/openspec:apply [change-id]
```

**作用**：AI 读取变更上下文，按任务清单开发代码，同步更新文档。

审查代码后如发现问题，**先修改文档，再让 AI 更新代码**：

```bash
/openspec:proposal [对修改的描述]
```

### Stage 4: 归档变更

```bash
/openspec:archive [change-id]
```

**作用**：合并规范增量到 `specs/` 主规范库，生成归档摘要，更新变更日志。

## 常用命令速查

| 命令 | 作用 |
|------|------|
| `/openspec:proposal [描述]` | 起草/修订变更提案 |
| `/openspec:apply [change-id]` | 实施任务，生成代码 |
| `/openspec:archive [change-id]` | 归档变更，更新规范 |
| `openspec list` | 查看进行中的变更 |
| `openspec list --specs` | 查看所有规范 |
| `openspec validate <id> --strict` | 验证格式 |

## 目录含义

| 目录 | 状态 |
|------|------|
| `specs/` | 已构建、已部署（单一事实来源） |
| `changes/` | 提案中、进行中 |
| `archive/` | 已完成、可追溯 |

## 使用案例：流水线取消通知功能

以 BK-CI 项目中的「流水线取消通知」功能为例，展示完整的 AI SDD 开发流程。

### Stage 1: 起草提案

```bash
/openspec:proposal 流水线通知增加构建取消发送时机，当构建被取消时（手动取消、超时取消、级联取消）能通知相关人员
```

AI 生成的 `proposal.md`：

```markdown
# Change: 流水线通知增加「构建取消」发送时机

## Why
当前流水线通知仅支持「构建成功」和「构建失败」两种发送时机，
无法在构建被取消时通知相关人员。

## What Changes
1. 新增取消通知订阅配置 - 在 PipelineSetting 中增加 cancelSubscriptionList 字段
2. 新增 YAML 通知条件 - 支持 `if: CANCELED` 条件
3. 新增取消相关变量 - 提供 ci.cancel_user 和 ci.cancel_reason 变量

## Impact
- Affected specs: pipeline-notification
- Affected code: PipelineSetting.kt, BluekingNotifySendCmd.kt 等
- Breaking changes: 无
```

AI 生成的 `tasks.md`：

```markdown
## 1. 数据模型层
- [ ] 1.1 在 PipelineSetting 中增加 cancelSubscriptionList 字段
- [ ] 1.2 更新 fixSubscriptions() 方法处理取消订阅

## 2. 数据库层
- [ ] 2.1 编写 DDL 脚本增加 CANCEL_SUBSCRIPTION 字段
- [ ] 2.2 更新 PipelineSettingDao 读写取消订阅字段

## 3. 通知发送层
- [ ] 3.1 更新 BluekingNotifySendCmd 增加取消状态分支
- [ ] 3.2 增加 ci.cancel_user 和 ci.cancel_reason 变量

## 4. 国际化
- [ ] 4.1 增加中英文通知文案
```

### Stage 2: 审查对齐

审查后发现需要补充「级联取消」场景的取消原因说明：

```bash
/openspec:proposal 补充级联取消场景：当父流水线被取消时，子流水线的 cancel_reason 应显示"由 xxx 取消父流水线的操作级联取消"
```

AI 更新文档，补充场景说明。

### Stage 3: 实施任务

```bash
/openspec:apply add-pipeline-cancel-notification
```

AI 按任务清单依次实现：数据模型 → 数据库脚本 → 业务逻辑 → 国际化文案。

### Stage 4: 归档

代码开发完成、测试通过后：

```bash
/openspec:archive add-pipeline-cancel-notification
```

规范增量合并到 `specs/pipeline-notification/`，变更历史保留在 `changes/archive/`。

---

**核心理念**：先让 AI 理解你要做什么，再让 AI 帮你做。Specs 是真相源，Changes 是提案。
