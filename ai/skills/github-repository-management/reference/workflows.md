# GitHub 协作工作流

## 仓库上下文

默认先判断当前请求是否已经落在本地仓库中。

- 如果当前工作区就是目标仓库，代码、分支、diff、测试结果优先从本地获取
- 如果用户只给了 GitHub 链接或仓库名，再用 GitHub MCP 补 `owner/repo`
- 在 BK-CI 场景下，常见默认值是：
  - `owner`: `TencentBlueKing`
  - `repo`: `bk-ci`
  - `base`: `master`

只有在用户明确指定了别的仓库、分支或组织时，才覆盖这些默认值。

## GitHub MCP 工具映射

- 仓库搜索：`search_repositories`
- 远端文件/目录读取：`get_file_contents`
- Issue 搜索：`search_issues`
- 创建或更新 Issue：`issue_write`
- PR 详情、文件、评论、review、check runs：`pull_request_read`
- 创建 PR：`create_pull_request`
- 提交 review、请求修改、解决线程：`pull_request_review_write`
- Issue/PR 普通评论：`add_issue_comment`
- 合并 PR：`merge_pull_request`

原则：

1. 先读取 MCP tool schema，再调用工具。
2. 对用户可见的写操作，先给草案，后执行。
3. 不把 MCP 返回的字段名硬背进主 `SKILL.md`，只在这里保留高频映射。

## Issue 工作流

### 1. 创建前检查

- 明确问题类型：缺陷、需求、任务拆分、回归、文档缺口
- 提炼最小上下文：
  - 现象 / 目标
  - 影响范围
  - 期望行为
  - 复现方式或证据
- 用 `search_issues` 查重，至少覆盖：
  - 标题关键词
  - 核心模块词
  - 已关闭但相关的历史 issue

### 2. 起草规则

- **标题前缀（bk-ci 惯例）**：`{type}: {简短描述}`，冒号后保留一个空格；`type` 与分支/commit 对齐：`feat` / `bug` / `pref` / `refactor` / `issue`（含义见 `github-worktree` Step 3）。示例：`feat: 蓝盾智能助手支持项目级知识库、插件级知识库`（[#13058](https://github.com/TencentBlueKing/bk-ci/issues/13058)）。
- 前缀之后写清“问题/目标 + 对象”，避免 `update`、`优化一下` 这类弱描述；标题中不写 issue 号。
- 正文至少包含：
  - 背景
  - 当前问题或目标
  - 验收口径
  - 相关链接或证据
- 只有在确定仓库里已有对应值时，才补 labels、assignees、milestone、type

### 3. 执行规则

- 先把标题和正文草案展示给用户
- 获得确认后，用 `issue_write` 的 `create`
- 如果是补充信息、改状态或标记重复，用 `issue_write` 的 `update`
- 普通跟进回复用 `add_issue_comment`

### Issue 草案模板

```markdown
## 背景
[说明问题出现的场景或为什么要做]

## 当前问题 / 目标
[描述当前行为、缺口或希望达成的结果]

## 验收标准
- [标准 1]
- [标准 2]

## 补充信息
[日志、截图、提交、PR、设计链接等]
```

## PR 工作流

### 1. 创建前检查

PR 不是只靠 GitHub 远端信息就能起草，必须结合本地仓库：

- 当前分支名
- 目标分支
- 本地 diff / 变更摘要
- 已做验证
- 关联 Issue 或背景链接

如果这些信息不完整，先补齐，不要直接创建 PR。

### 2. 起草规则

- 标题写清主意图，不重复 commit message 列表
- 正文优先服务评审者，至少包括：
  - Summary
  - Test plan
  - 风险 / 影响面（必要时）
- 如果需求未收敛、验证未完成、仍有待答问题，优先 draft PR

### 3. 执行规则

- 先检查是否已有同分支或同主题 PR，避免重复提单
- 先向用户展示拟提交的标题、正文、`head`、`base`
- 获得确认后再调用 `create_pull_request`
- 创建后如需补充普通说明，用 `add_issue_comment`
- 如需正式 review 动作，用 `pull_request_review_write`

### PR 正文模板

```markdown
## Summary
- [变更点 1]
- [变更点 2]

## Test plan
- [ ] 已执行的验证 1
- [ ] 已执行的验证 2

## Risk
- [可选：影响模块、兼容性、回滚关注点]
```

## 评论与评审

先区分三类动作：

1. Issue 下的普通讨论
2. PR 页面上的普通评论
3. 代码 review comment / review thread

选择规则：

- 用户只是“回复这个 issue / PR”时，优先 `add_issue_comment`
- 用户明确要“给 review、请求修改、approve、处理 review thread”时，用 `pull_request_review_write`
- 读取代码评审线程前，先用 `pull_request_read` 的 `get_review_comments`
- 读取普通 PR 评论时，用 `pull_request_read` 的 `get_comments`

不要把 review 意见发成普通评论，否则会破坏评审语义。

## 合并前检查

执行合并前至少确认：

- 用户明确要求合并
- PR 已指向正确的 `base`
- 关键 review 结论已收敛
- `pull_request_read` 的 `get_check_runs` 或 `get_status` 没有阻塞项
- 没有明显冲突或待处理线程

只有在以上条件满足后，才调用 `merge_pull_request`。默认不要替用户猜测 `merge` / `squash` / `rebase`，除非仓库约定或用户已指定。

## 业界实践摘要

- 写前先读：避免重复 issue、重复 PR 和错误上下文
- 草案先行：Issue/PR 的标题和正文先让用户看一遍
- 最小必要写入：不确定的 labels、assignee、issue type 宁可留空
- PR 面向评审者：正文重点是变更意图、验证结果和风险，不是重复 diff
- 合并是单独授权动作：创建 PR 不等于允许合并
