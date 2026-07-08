---
name: cleanup-github-worktree
description: >-
  清理长时间未使用的 git worktree（GitHub 版）。仅在用户明确要求清理 worktree 时使用，不自动触发。
  关键词：清理 worktree、删除 worktree、移除旧分支、worktree 回收。
disable-model-invocation: true
---

# 清理过期 Worktree（GitHub 版）

手动触发：当用户明确要求清理、删除或回收长时间未使用的 worktree 时执行。

## 前置约束

**必须在 master 上执行**：执行前 `git branch --show-current`；若不是 `master`，中止并提示用户切换。

## 工作流

### Step 1: 选择清理时间范围

使用 AskQuestion 让用户选择阈值：

- 提示：`选择清理阈值：超过多久未使用的 worktree 将被清理？`
- 选项：
  - `1 周`（对应 7 天）
  - `2 周`（对应 14 天）
  - `1 个月`（对应 30 天）

将用户选择映射为天数：

| 选项 | 天数 |
|------|------|
| 1 周 | 7 |
| 2 周 | 14 |
| 1 个月 | 30 |

### Step 2: 预览过期 Worktree

运行清理脚本，输出候选分支名列表（每行一个分支名），不执行删除。

根据操作系统选择脚本（通过用户信息中的 `OS Version` 判断）：

**Windows（PowerShell）：**

```powershell
powershell -File .cursor/skills/cleanup-github-worktree/scripts/cleanup-worktree.ps1 -Days <天数>
```

**macOS / Linux（Bash）：**

```bash
bash .cursor/skills/cleanup-github-worktree/scripts/cleanup-worktree.sh -d <天数>
```

将脚本输出的分支名列表展示给用户。如果没有找到过期 worktree，告知用户并结束。

### Step 3: 用户确认

使用 AskQuestion 让用户确认是否继续：

- 提示：`以上 <N> 个 worktree 将被清理（关闭 GitHub Issue、移除 worktree、删除本地分支），是否继续？`
- 选项：
  - `确认清理`
  - `取消`

用户选择取消时，结束工作流。

### Step 4: 批量执行 close-github-worktree-done

对 Step 2 识别出的**每一个候选分支**，以该分支名作为目标分支，执行 `close-github-worktree-done` skill 的 **Step 1 ～ Step 5** 完整流程（跳过其 Step 0 的交互选择）。

- 批量场景下，Step 3 关闭 Issue 默认可直接对 `open` 状态执行关闭（`state=closed, state_reason=completed`），无需逐个再次 AskQuestion；已 `closed` 的跳过。
- 有未提交变更的分支：按 `close-github-worktree-done` Step 2 规则跳过，在最终报告中标记为"跳过（有未提交变更）"。
- 单个分支处理失败不中断整批，继续处理下一个。

### Step 5: 汇总报告

向用户展示清理结果摘要，包含每个分支的处理状态：

| 分支名 | GitHub Issue | worktree | 本地分支 | 备注 |
|--------|--------------|----------|----------|------|
| bug-12994 | ✅ closed | ✅ 已移除 | ✅ 已删除 | |
| feat-11524 | ⏭ 跳过 | ✅ 已移除 | ✅ 已删除 | 分支名无 issue 号 |
| pref-12948 | — | — | — | 跳过（有未提交变更） |

## 脚本说明

| 脚本 | 系统 | 路径 |
|------|------|------|
| cleanup-worktree.ps1 | Windows | `.cursor/skills/cleanup-github-worktree/scripts/cleanup-worktree.ps1` |
| cleanup-worktree.sh | macOS/Linux | `.cursor/skills/cleanup-github-worktree/scripts/cleanup-worktree.sh` |

脚本职责：

1. 通过 `git worktree list --porcelain` 获取所有 worktree
2. 跳过主 worktree（第一个）和 bare 条目
3. 通过 `git log -1 --format=%ct <branch>` 获取分支最后一次 commit 时间；detached HEAD 时回退到目录修改时间
4. 筛选超出阈值的条目，**仅输出候选分支名列表**（每行一个），供本 skill Step 2 解析使用
