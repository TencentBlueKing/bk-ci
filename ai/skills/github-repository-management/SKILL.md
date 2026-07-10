---
name: github-repository-management
description: 通过 GitHub MCP 管理仓库协作时使用，例如检索仓库、提 Issue、起草或创建 PR、查看评论、回复评审和做合并前检查。当用户要操作 GitHub 仓库协作流程而不是修改本地代码实现时优先使用。
---

# GitHub 仓库协作管理

## 适用场景

- 基于 GitHub MCP 查询 `TencentBlueKing/bk-ci` 或其他仓库信息
- 创建、更新或补充 Issue
- 基于当前本地改动起草或创建 PR
- 查看 PR 评论、review、check runs，并回复评审意见
- 做合并前检查，确认是否满足合并条件

## 不适用场景

- 只修改本地代码实现，不涉及 GitHub 协作动作
- 只讨论 commit message、提交边界或本地 Git 历史整理
- 需要深入做正确性审查，此时应切到代码 review 流程
- 未经用户确认直接执行有副作用的 GitHub 写操作

## 快速指导

1. 这个 skill 关注的是“本地仓库上下文 + GitHub MCP 协作动作”的闭环，不是 GitHub 功能总览。
2. 在 BK-CI 仓库里，默认仓库上下文是 `TencentBlueKing/bk-ci`，默认基线分支通常是 `master`；若用户另有指定，以用户指定为准。
3. 代码事实优先来自当前本地仓库；Issue、PR、评论、check runs 等协作事实优先通过 GitHub MCP 获取。
4. 所有写操作都遵循“先读后写”：
   - 先确认 `owner/repo`
   - 先搜索重复 Issue / 现有 PR / 相关评论
   - 先向用户展示拟提交内容
   - 获得确认后再真正创建或更新
5. PR 主链路要同时补齐本地信息：分支名、目标分支、变更摘要、验证结果、关联 Issue。
6. 不确定是否能直接合并时，优先创建 draft PR，而不是假设已可合并。
7. 具体工具选择、模板和流程细节放在 `reference/workflows.md`。

## 高信号规则

- GitHub MCP 负责远端协作状态，本地仓库负责代码与测试事实，二者不要混用
- 创建 Issue 前必须先查重；创建 PR 前必须先看本地 diff、目标分支和现有 PR 状态
- Issue 评论与 PR review comment 是两类动作，回复时不要选错工具
- 合并前至少确认 review 状态、check runs、冲突风险和用户授权
- 标签、assignee、milestone、issue type 只有在仓库中已知可用时才填写，不要臆造

## 关键陷阱

- 只看本地代码就直接提 Issue / PR，导致远端上下文缺失
- 把普通 PR 评论当成代码 review comment，或反过来
- 没有向用户展示最终标题和正文就直接执行写操作
- 在 CI 未确认、评审未收敛时默认执行合并

## 延伸阅读

- 核心工作流与模板：`reference/workflows.md`
- 如需整理提交表达：看 `git-commit-specification`
- 如需做 PR 正确性审查：走 GitHub PR review 流程，而不是把审查细则塞进本 skill
