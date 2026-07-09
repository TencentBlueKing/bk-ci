---
name: git-commit-and-push
description: 通过 BK-CI 仓库约定安全执行本地提交与远端推送，例如根据当前变更生成一句简短提交内容、先展示给用户确认、确认后再 `git commit`，并默认推送到个人 fork 远端而不是主仓库。 当用户要求“帮我 commit / push / 提交并推送代码 / 发到远端”时优先使用。
---

# Git 提交与推送

## 适用场景

- 用户明确要求执行 `git commit`
- 用户明确要求执行 `git push`
- 用户要求把当前本地改动提交并同步到远端分支
- 需要根据当前 diff 先生成一句简短提交内容给用户确认

## 不适用场景

- 只是编写 commit message，不需要真正提交或推送
- 需要创建 GitHub Issue、PR、回复评论或检查远端协作状态
- 需要改写 Git 历史，如 `rebase`、`reset`、`commit --amend`、`push --force`
- 当前还在 `master` 上准备开始开发，此时应先走 `github-worktree`

## 快速指导

1. 先看分支和工作树状态，再决定能否提交：
   - `git branch --show-current`
   - `git status --short`
   - `git diff` / `git diff --cached`
2. push 前先识别远端角色，不要假设 `origin` 一定是 fork：
   - `git remote -v`
   - `git remote get-url --push <remote>`
   - 区分哪个 remote 是个人 fork，哪个 remote 是上游主仓库
3. 当前在 `master`、`main`、`release-*` 等共享分支时，不要默认直接推送；优先提醒用户改走 `github-worktree` 或明确确认风险。
4. commit 前先整理提交边界：只提交与本次需求直接相关的文件；遇到无关改动、敏感文件或可疑生成物，先停下来和用户确认。
5. 先基于当前变更生成一条**简短预览文案**，默认格式为：
   - `{issue标题或规范化subject} {本次变更一句话摘要}`
   - 摘要只写这次实际新增/修改了什么，控制在一句话内，不展开成长正文
6. 若分支能解析出 issue 号，且已知 issue 标题，可直接复用 issue 标题作为前半句；后半句根据当前 diff 补一句简单说明。
7. 示例：
   - issue 标题：`feat: 新增 GitHub worktree 实践相关 Skill #13062`
   - 当前变更：新增 `worker-skill` 文件
   - 预览展示：`feat: 新增 GitHub worktree 实践相关 Skill #13062 新增了 worker-skill 文件。`
8. **特例：** 如果用户明确指定了某个 issue，并明确说“直接推送”，则把这视为对 `commit + push` 的一次性授权：
   - 直接使用 **当前 issue 标题 + issue 号** 作为完整 commit message
   - 若 issue 标题本身**已包含** `#issue号`，则直接原样使用
   - 若 issue 标题本身**不包含** `#issue号`，则自动归一化为：`<issue标题> #<issue号>`
   - 例如：当前 issue 标题是 `feat: 新增 GitHub worktree 实践相关 Skill`，issue 号是 `13062`，则 commit 内容必须为：`feat: 新增 GitHub worktree 实践相关 Skill #13062`
   - **不要**再拼接当前 diff 的补充摘要
   - **不要**改写措辞、补句号、补正文、补 scope，或追加除 `#issue号` 外的任何其他 message
   - **不要**再向用户展示预览或做二次确认
   - 完成必要安全检查后，直接执行 `git commit` 和 `git push`
9. 默认路径下，把生成的预览内容先展示给用户，等待用户明确回复“没问题”“可以提交”“直接 push”等确认后，再执行 `git commit` 和 `git push`。
10. commit message 的格式、type 选择、Issue 号拼接规则，复用 `git-commit-specification`；本 skill 只补“基于当前 diff 生成一句摘要并先确认”这层工作流，以及“指定 issue + 直接推送”的快捷特例。
11. commit 成功后再判断 push 策略，**默认目标是 fork 远端**：
   - 当前分支已跟踪 fork remote：优先 `git push`
   - 尚未建立 upstream：优先 `git push -u <fork-remote> HEAD`
   - 若当前 tracking 指向主仓库 remote，不要直接沿用；先改为 fork remote 或先向用户确认
   - 远端领先、分叉或被拒绝：停止默认推送，先向用户说明并确认后续同步策略
12. push 完成后，向用户反馈实际结果：当前分支、推送到的 fork remote、是否已建立 upstream、是否还需要走 PR 流程。

## 高信号规则

- `commit` 和 `push` 都是有副作用的写操作；没有用户明确授权时，不要自行执行
- 不要把用户未要求提交的无关改动一并塞进本次提交
- 展示给用户的预览文案要基于**当前实际 diff**，不要只照抄 issue 标题
- 只有在用户明确指定 issue 且明确说“直接推送”时，才允许跳过二次确认并直接使用 issue 标题作为完整 commit message
- 走“直接推送”快捷路径时，commit 内容必须包含当前 `#issue号`；若标题缺少 issue 号，只允许补上 ` #issue号`
- BK-CI 仓库里，分支名末段若是纯数字，通常对应 GitHub Issue 号；可用于 message 对齐，但不要臆造 issue
- push 默认推到个人 fork，不默认推到上游主仓库；只有用户明确要求时才向主仓库 remote 推送
- push 只负责把本地分支同步到远端，不等于创建 PR、合并分支或关闭 Issue
- 被 hook 修改文件、push 被 reject、远端已领先时，应如实反馈当前 Git 状态，不要偷偷改写历史
- 非用户明确要求时，不执行 `push --force`、不推共享主分支、不错用 `--no-verify`

## 关键陷阱

- 只看文件名就直接提交，没有先看 diff
- 生成的预览文案没有体现“本次实际改了什么”，用户看完仍不知道要提交什么
- 用户并未明确指定 issue，却误走了“直接用 issue 标题提交并跳过确认”的快捷路径
- 已命中“直接推送”快捷路径，却漏掉了 `#issue号`，或在补 `#issue号` 之外又追加了摘要
- 把 unrelated 改动、格式化噪音或敏感文件顺手提交
- 没先识别 fork remote，就把分支直接推到了主仓库 remote
- 当前分支无 upstream 却仍使用普通 `git push`，导致失败后误判
- push 失败后直接建议强推，跳过远端分叉原因排查
- 把“提交并推送”误当成“顺手创建 PR 并处理 GitHub 评论”

## 延伸阅读

- commit message 与 issue 规则：`git-commit-specification`
- 从 `master` 创建 issue 分支与 worktree：`github-worktree`
- PR / Issue / review comment 工作流：`github-repository-management`
