---
name: github-worktree
description: >-
  须用户 @ 本 skill 或明确说要按 GitHub Issue worktree 流程执行时再读取。
  在 master 上：确定关联的 GitHub Issue（默认关联已有，明确要建时才在上游建单）、生成分支名、git fetch、脚本同时创建新分支与 worktree、cursor --new-window 打开新窗口；细节与分支命名见正文。
  不因泛泛提到 GitHub、issue、worktree、建分支等词而自动套用。
disable-model-invocation: true
---

# GitHub Issue Worktree 工作流

当用户在 **master 分支** 上提出需要改动代码的需求时，执行以下工作流。本流程适配蓝盾（bk-ci）GitHub Issue 驱动的开发模式。

> **硬性约束：一旦用户选择创建分支，Agent 在本次对话轮次中禁止对项目文件做任何内容修改（包括写入、编辑、创建文件）。工作流结束后，提示用户在新打开的 worktree 窗口中继续操作。仅当用户明确选择"直接在 master 上改"时，才跳过工作流、正常执行需求。**

## 固定参数

- 仓库 owner = `TencentBlueKing`，repo = `bk-ci`
- 远端 = `origin`，新分支基线 = `origin/master`
- 当前 GitHub 用户：通过 `get_me` 获取 `login`（用于 issue assignee）
- GitHub MCP server = `user-github`

## 前置检查

### 0. 检查运行环境

本 skill 仅在 **Cursor IDE** 中生效。如果当前环境不是 Cursor IDE（例如在 CLI、终端或其他编辑器中），跳过本工作流，直接执行用户需求。

判断方式：用户信息中存在 `Workspace Path` 且 Shell 可执行 `cursor --version` 命令。

### 1. 检查当前分支

```bash
git branch --show-current
```

只有当前分支为 `master` 时才触发本工作流。如果不在 master，直接执行用户需求即可。

### 2. 检查 GitHub MCP 可用性

确认 `user-github` MCP 服务器可用。如果不可用，提醒用户：

> 当前未检测到 GitHub MCP 服务。请先在 Cursor Settings → MCP 中添加 GitHub 服务器后重试；或手动提供分支名，跳过 issue 关联步骤直接创建 worktree。

如果用户无 GitHub MCP 但仍想创建 worktree，跳过 issue 步骤，直接让用户手动提供分支名后执行 worktree 创建。

## 工作流

### Step 1: 询问是否创建分支

使用 AskQuestion 工具向用户确认：

- 提示：`当前在 master 分支，需要先创建工作分支再改动代码。如何关联 GitHub Issue？`
- 选项：
  - `关联已有 Issue 建分支`（需用户提供 issue 号）
  - `新建 Issue 后建分支`（在上游公开仓库建单，需二次确认）
  - `不关联 Issue，直接建分支`（需用户提供分支名）
  - `不建分支，直接在 master 上改`（给出风险提示后继续）

### Step 2: 确定关联的 Issue

#### A. 关联已有 Issue

用户提供 issue 号后，读取标题用于生成分支关键词：

```
CallMcpTool:
  server: user-github
  toolName: issue_read
  arguments:
    method: get
    owner: TencentBlueKing
    repo: bk-ci
    issue_number: <issue号>
```

从返回中取 `title` 与（如有）`labels`、`issue_type`，用于推断 type 与提炼关键词（见 Step 3「从 Issue 推断 type」）。

#### B. 新建 Issue（上游公开仓库，需二次确认）

> **在公开上游 `TencentBlueKing/bk-ci` 创建 Issue 前，必须用 AskQuestion 让用户确认标题/正文，确认后才调用创建接口。**

**Issue 标题规范（与分支 type、commit 标记对齐）：**

- 格式：`{type}: {简短描述}`（英文冒号后保留一个空格）
- **type** 与 Step 3 分支前缀一致：

| type | 适用场景 |
| ---- | -------- |
| `feat` | 新功能、能力增强 |
| `bug` | 缺陷修复 |
| `pref` | 性能、参数、配置优化 |
| `refactor` | 重构（不改外部行为） |
| `issue` | 类型不明确、杂项跟踪 |

示例（参考 [#13058](https://github.com/TencentBlueKing/bk-ci/issues/13058) 一类需求单）：

- `feat: 蓝盾智能助手支持项目级知识库、插件级知识库`
- `bug: 流水线构建完成后状态未更新`
- `pref: 构建详情页接口减少冗余查询`

标题中**不要**再写 issue 号；编号由 GitHub 分配。正文仍用背景 / 需求 / 验收等结构，与 `github-repository-management` 的 Issue 模板一致。

1. `get_me` 获取当前用户 `login`，作为 assignee。
2. 按上表从用户需求选定 `type`，起草 `title`（须含 `{type}: ` 前缀）与 `body`。
3. （可选）`list_issue_types`（owner=`TencentBlueKing`）确认该组织是否启用 issue type；若启用且与 `type` 对应，可传 `type`（如 Feature / Bug）；未启用则省略。
4. 调用创建：

```
CallMcpTool:
  server: user-github
  toolName: issue_write
  arguments:
    method: create
    owner: TencentBlueKing
    repo: bk-ci
    title: "<type>: <简短描述>"
    body: <需求/缺陷描述，含背景、用例、实现要点>
    assignees: ["<当前用户 login>"]
```

从返回中取新 issue 的 `number`。

#### C. 不关联 Issue

让用户直接提供分支名，或根据需求描述生成分支名（不含 issue 号后缀），跳到 Step 4。

### Step 3: 生成分支名

分支命名规则：`{type}-{issue号}` 或 `{type}-{keywords}-{issue号}`

#### 从 Issue 推断 type（按优先级）

1. **标题前缀**：若 `title` 以 `feat:`、`bug:`、`pref:`、`refactor:`、`issue:` 开头（不区分大小写），取冒号前一段作为 type（统一小写）。
2. **GitHub `issue_type`**：如 `Feature` → `feat`，`Bug` → `bug`（与仓库配置一致时）。
3. **labels**：如 `kind/bug` → `bug`，`kind/enhancement` → `feat`。
4. 仍无法判断 → `issue`。

#### 从标题提炼 keywords（可选）

- 去掉 `{type}: ` 前缀后，从剩余标题提取 1～3 个英文关键词（全小写、`-` 连接）；中文标题可据语义翻译或缩写（如「知识库」→ `knowledge`）。
- 例：标题 `feat: 蓝盾智能助手支持项目级知识库、插件级知识库`，issue `#13058` → `feat-ai-knowledge-13058` 或 `feat-knowledge-13058`。

- **issue号**：GitHub Issue 编号（纯数字），**必须**为分支名末段。

示例：
- `bug: …` + `#12994` → `bug-12994`
- `feat: …` + `#11524` → `feat-11524` 或 `feat-tenant-3-11524`
- 无类型前缀、仅 `issue_type` 为 Feature 的 `#13058` → `feat-knowledge-13058`（按标题补 keywords）

> 可在 type 与 issue 号之间补充 1～3 个英文关键词增强可读性（与现网 `feat-tenant-3-11524`、`feat-ai-knowledge-13058` 风格一致）。是否加关键词由你按标题判断，但**末段必须是 issue 号**，便于后续 commit/close skill 解析。

如果用户选择不关联 Issue，则让用户提供分支名或根据需求描述生成（末段可不含 issue 号）。

### Step 4: 创建 Worktree 并同时创建分支

先拉取最新代码，确保基于最新 master 创建分支：

```bash
git remote set-url origin https://github.com/TencentBlueKing/bk-ci.git
git fetch origin master
```

在执行脚本前，先确认目标分支名在本地尚不存在；本步骤要求**创建 worktree 时同时新建分支**，不复用已有本地分支。如果本地已存在同名分支，应提示用户改名，或先手动清理旧分支/旧 worktree 后再重试。

根据操作系统选择脚本，**基线指定 `origin/master`**：

**Windows（PowerShell）：**

```powershell
powershell -File .cursor/skills/github-worktree/scripts/git-worktree.ps1 -Action Add -Branch "<分支名>" -Base origin/master
```

**macOS / Linux（Bash）：**

```bash
bash .cursor/skills/github-worktree/scripts/git-worktree.sh add -b "<分支名>" -B origin/master
```

脚本会显式执行“基于 `origin/master` 新建分支 + 创建 worktree”这两个动作；成功后会自动从主仓库同步 `.idea` 和 `.cursor` 到新 worktree，保证
IDEA 项目配置、Gradle 导入配置、本地运行配置以及当前工程下的 Cursor rules/skills/config
尽量与主仓库窗口一致。

脚本随后会进入新 worktree 的 `src/backend/ci`，对所有使用 `task-gen-jooq` 的模型模块执行
jOOQ 生成任务。如果数据库不可访问或 jOOQ 生成失败，脚本会失败退出，需先处理数据库连接或生成错误。

#### 操作系统判断

- 用户信息中的 `OS Version` 包含 `win32` → 使用 PowerShell 脚本
- 包含 `darwin` → 使用 Bash 脚本
- 包含 `linux` → 使用 Bash 脚本

### Step 5: 在新窗口打开 Worktree

从 Step 4 脚本输出中提取 `Created worktree: <path>` 的实际路径（不要自己拼路径）。

使用以下命令在 Cursor 新窗口打开：

```bash
cursor --new-window "<从脚本输出中提取的 worktree 路径>"
```

告知用户新窗口已打开，可以在新窗口中继续开发。**本次对话到此结束，不要再对任何文件做修改。**

## 脚本说明

| 脚本 | 系统 | 路径 |
|------|------|------|
| git-worktree.ps1 | Windows | `.cursor/skills/github-worktree/scripts/git-worktree.ps1` |
| git-worktree.sh | macOS/Linux | `.cursor/skills/github-worktree/scripts/git-worktree.sh` |

两个脚本功能一致，支持三个操作：

- **add**：基于 `-B/-Base` 指定的 base 同时新建分支与 worktree；若本地已存在同名分支则直接失败，避免误复用旧分支
- **list**：列出所有 worktree
- **remove**：移除 worktree（支持按分支名或路径）
