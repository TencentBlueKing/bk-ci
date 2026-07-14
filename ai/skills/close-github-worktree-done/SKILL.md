---
name: close-github-worktree-done
description: >-
  在用户显式 @ 本 skill 或明确下达「关闭 worktree 并完成需求」类指令时：将指定分支关联的 GitHub Issue 关闭（可选并确认），移除本地 git worktree 目录与本地分支（不删除远程分支）。
  必须在 master 分支上执行。用户可指定要关闭的分支名；若未指定，列出所有 worktree 供用户选择。
  不得因仅提及 GitHub、issue、worktree、git 等词而自动套用；须以用户主动调用本工作流为准。
disable-model-invocation: true
---

# 关闭 Worktree 并完成需求（GitHub 版）

## 固定参数

- 仓库 owner = `TencentBlueKing`，repo = `bk-ci`
- 远端 = `origin`
- GitHub MCP server = `user-github`

## 硬性约束

1. **必须在 master 上执行**：执行前 `git branch --show-current`；若**不是** `master`，**中止**并说明本 skill 仅在 master 分支上使用，请先切换到 master 分支。

## 工作流

### Step 0: 确定目标分支

用户可在指令中指定要关闭的分支名（如 `bug-12994`）。

- **已指定分支名**：直接使用该分支名作为目标分支，进入 Step 1。
- **未指定分支名**：执行 `git worktree list`（或 `--porcelain`）列出所有 worktree，**排除主工作树**（`--porcelain` 下第一条 `worktree ` 路径对应、与主仓库根目录相同的那条），将剩余条目的**分支名**（及路径，便于辨认）展示给用户，使用 `AskQuestion` 让用户选择要关闭的分支。用户选择后以所选分支名作为目标分支进入 Step 1。若无附属 worktree，说明情况并结束。

以下步骤中「目标分支」均指本步确定的分支名。

### Step 1: 解析 Issue 号

从**目标分支名**解析 GitHub Issue 号，约定 `{type}-{keywords}-{issue号}` 或 `{type}-{issue号}`（例：`bug-12994`、`feat-tenant-3-11524`、`issue-12123`）。用 `-` 分段，**最后一段**为 issue 号（纯数字）。

- 最后一段非纯数字 / 无法解析：询问用户是否**仅移除 worktree、跳过 GitHub Issue 操作**；用户确认后继续（跳过 Step 3）。
- 可解析：记录 issue 号，供 Step 3 使用。

### Step 2: 解析目标 worktree 路径并确认是否可移除

**顺序说明：** 须先确认本地存在可移除的 worktree 且脏数据策略已达成一致，再操作 GitHub Issue（Step 3），避免「无 worktree / 用户中止」时 Issue 已被关闭。

1. **主仓库根目录 `MAIN_ROOT`**：`git rev-parse --show-toplevel`（当前已在 master，应为主工作树）。
2. **目标 worktree 根目录 `TARGET_ROOT`**：根据 `git worktree list`（或 `--porcelain`）查找检出「目标分支」的那一行路径。若找不到（该分支无本地 worktree），说明情况并中止。

若 **`TARGET_ROOT` 与 `MAIN_ROOT` 相同**：**不得**继续移除（主工作树不应由此 skill 关闭）；应中止并说明。

在 **`TARGET_ROOT`** 下执行 `git status --porcelain`（可用 `git -C "<TARGET_ROOT>" status --porcelain`）。

- 有未提交变更：说明风险，**需用户明确同意**后才可在移除时使用 `git worktree remove --force`；否则请先提交或 stash。
- 无变更：移除时无需 `--force`（除非 Git 仍要求，按提示处理）。

**合并状态检查：** 须先让本地 `master` 与远端一致，再判断目标分支是否已合并；均在 **`MAIN_ROOT`**（当前检出 `master`）下执行：

```bash
git fetch origin master
git merge origin/master
```

- `fetch` / `merge` 任一步失败（含本地 `master` 有未提交变更导致无法快进/合并）：向用户说明原因并**中止**本流程。
- 同步成功后执行：

```bash
git branch --merged master
```

若输出中**不包含**目标分支名，说明该分支尚未合并到 master。此时须通过 `AskQuestion` 明确询问用户：

> 分支 `<目标分支名>` 尚未合并到 master，删除后其本地提交将无法通过此分支访问。确定要继续删除吗？

- 用户选择**取消**：中止整个流程。
- 用户选择**确认**：继续执行 Step 3。

本步骤**不删除远程分支**；不执行 `git push --delete` 等操作。

### Step 3: 关闭 GitHub Issue（可选并确认）

（若 Step 1 已选择**跳过 GitHub**，跳过本步。）

> **说明：** bk-ci 的 PR 合并若在描述中带 `fixes #N` / `closes #N`，Issue 通常在合并时**已自动关闭**。因此本步默认不强制关闭，须先确认状态、再决定。

1. 读取 Issue 当前状态：

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

2. 若 `state` 已为 `closed`：跳过关闭，直接进入 Step 4。
3. 若仍为 `open`：用 `AskQuestion` 询问用户是否现在关闭该 Issue。
   - 用户确认关闭：

```
CallMcpTool:
  server: user-github
  toolName: issue_write
  arguments:
    method: update
    owner: TencentBlueKing
    repo: bk-ci
    issue_number: <issue号>
    state: closed
    state_reason: completed
```

   - 用户选择不关闭：保留 Issue 开启，继续 Step 4。

MCP 失败时向用户说明，由用户决定是否仍执行 Step 4 移除 worktree。**本步不删除远程分支。**

### Step 4: 移除本地 worktree

应在 **master / `MAIN_ROOT`** 中操作，避免在待删目录内执行导致失败。

**方式 A（推荐，路径已明确）：** 在 `MAIN_ROOT` 下执行：

```bash
git worktree remove [--force] "<TARGET_ROOT>"
```

**方式 B（按分支名解析，与创建时路径规则一致）：** 在 **MAIN_ROOT** 下执行脚本：

**Windows（PowerShell）：**

```powershell
powershell -File .cursor/skills/github-worktree/scripts/git-worktree.ps1 -Action Remove -Branch "<目标分支名>"
```

**macOS / Linux：**

```bash
bash .cursor/skills/github-worktree/scripts/git-worktree.sh remove -b "<目标分支名>"
```

必要时加 `-Force` / `-f`（仅在与用户确认后）。

### Step 5: 删除本地分支

worktree 移除成功后，删除对应的本地分支：

```bash
git branch -d "<目标分支名>"
```

- 若 Git 提示该分支未完全合并（`-d` 拒绝），**需用户明确同意**后才可使用 `-D` 强制删除；否则跳过本步，提示用户手动处理。
- 本步骤**不删除远程分支**；不执行 `git push --delete` 等操作。

## 注意事项

- 移除 worktree 后，若 Cursor 仍打开 **`TARGET_ROOT`** 目录，该工作区路径将失效，属预期现象。
- 若 GitHub 与移除任一步失败，如实反馈；已执行的步骤不自动回滚。
