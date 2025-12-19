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

### 2. 安装 CodeBuddy

```bash
npm install -g @anthropic-ai/codebuddy-cli
codebuddy --version
# 命令行执行codebuddy进入命令行界面，首次需要登录，登录后使用 `/model` 命令可以切换具体的模型。
codebuddy
# 确认没问题后，执行exit，退出codebuddy
exit
```



### 3. 安装 OpenSpec

```bash
npm install -g @fission-ai/openspec@latest
openspec --version
```

### 4. 初始化项目

```bash
cd your-project
openspec init
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

## AI SDD 开发流程

```
起草提案 → 审查对齐 → 实施任务 → 归档更新
```

### 前置准备：让 AI 学习工程背景

首次使用时，让 AI 理解项目背景：

```
codebuddy
> Please read openspec/project.md and help me fill it out with details about my project, tech stack, and conventions
```

AI 会扫描项目结构、识别代码模式，将分析结果写入 `openspec/project.md`。

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

---

**核心理念**：先让 AI 理解你要做什么，再让 AI 帮你做。Specs 是真相源，Changes 是提案。
