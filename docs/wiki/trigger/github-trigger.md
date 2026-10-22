# GitHub 事件触发

当 GitHub 仓库发生**推送、创建分支/Tag、Pull Request、代码评审、Issue、评论**等事件时，自动启动流水线。

- 入口：**流水线编辑 → 触发器 → 新增「GitHub 事件触发」**
- 可视化配置与 PAC YAML `on`（`type: github`）双向同步
- 需要先在**代码库**中关联对应的 GitHub 仓库并完成授权，蓝盾才能接收到仓库事件

## 能力概览

一个触发器只监听**一种事件类型**，需要监听多种事件时添加多个触发器。

| 事件类型 | PAC 关键字 | 典型场景 |
|----------|-----------|----------|
| Commit Push Hook | `push` | 推代码后自动构建 |
| Create Branch Or Tag | —（暂不支持 PAC） | 新建分支或 Tag 时触发 |
| Pull Request Hook | `mr` | 提 PR / PR 更新时跑检查 |
| Code Review Hook | `review` | PR 评审通过后自动部署 |
| Issue Hook | `issue` | Issue 变更时同步到其他系统 |
| Note Hook | `note` | 在评论里写指令触发 |

---

## 配置触发器

### 通用说明

- **代码库**：可视化支持「按代码库选择」或「按代码库别名输入」；PAC 使用 `repo-name` 填代码库别名，省略时监听 PAC 流水线所在的代码库。
- **包含 / 排除规则**：多个值用英文逗号分隔（PAC 写成列表）；留空 = 不限制；**排除优先于包含**。
- **流水线变量**：分支、人员、负责人、标签等输入框支持 `${{ variables.xxx }}` 引用流水线变量。
- **人员过滤**：按触发人的 GitHub 登录名**精确匹配**。
- 分支匹配使用 Ant 风格通配符（`*` 不跨 `/`、`**` 可跨多层、`?` 单个字符），大小写敏感。

### Commit Push Hook（push）

| 字段 | 说明 | 示例 |
|------|------|------|
| 监听以下分支 | 留空 = 全部分支 | `main,release/*` |
| 排除以下分支 | 命中则不触发 | `dev_*` |
| 排除以下操作人 | 多个用逗号分隔 | `dependabot[bot]` |

> GitHub 触发器**不支持按路径过滤**，也不支持「包含以下人员」。

### Create Branch Or Tag

新建分支或 Tag 时触发，字段与 push 相同（监听分支 / 排除分支 / 排除操作人）。该事件**暂不支持 PAC**，只能在可视化中配置。

### Pull Request Hook（mr）

| 字段 | 说明 | 示例 |
|------|------|------|
| 动作 | `open` 创建 / `close` 关闭 / `reopen` 重新打开 / `push-update` 源分支有推送 / `merge` 已合并 / `edit` 编辑 / `assigned` 指派 / `unassigned` 取消指派 / `labeled` 添加标签 / `unlabeled` 移除标签。可视化新建时默认 `open,reopen,push-update` | — |
| 监听以下分支 / 排除以下分支 | 按 PR 的**目标分支**过滤 | `main` |
| 排除以下操作人 | 多个用逗号分隔 | — |
| 监听以下负责人 / 排除以下负责人 | 仅在事件本身带负责人（`assigned` / `unassigned`）时参与判断 | — |
| 监听以下标签 / 排除以下标签 | 仅在事件本身带标签（`labeled` / `unlabeled`）时参与判断 | `need-ci` |

### Code Review Hook（review）

| 字段 | 说明 |
|------|------|
| 监听以下状态 | `approving` 评审中（有人通过但还不满足合并条件）/ `approved` 评审通过且可合并 / `change_denied` 评审被驳回 / `change_required` 要求修改。留空 = 全部 |

### Issue Hook（issue）

| 字段 | 说明 |
|------|------|
| 监听以下动作 | `open` / `close` / `reopen` / `update` / `assigned` / `unassigned` / `labeled` / `unlabeled`。留空 = 全部 |
| 排除以下操作人 | 多个用逗号分隔 |
| 监听 / 排除以下负责人 | 仅在事件本身带负责人时参与判断 |
| 监听 / 排除以下标签 | 仅在事件本身带标签时参与判断 |

### Note Hook（note）

| 字段 | 说明 | 示例 |
|------|------|------|
| 监听以下评论类型 | 可视化为「Commit / Review / Issue」三类；PAC 写 `commit` / `merge_request` / `issue`。留空 = 全部 | — |
| 监听以下评论内容 | **正则表达式**，评论内容命中任一条即触发。留空 = 全部 | `^/rebuild` |

---

## 输出变量

触发成功后，可在流水线中通过 `${{ ci.* }}` 引用本次事件信息。与当前事件类型无关的变量为空。

### 公共（所有事件）

| 变量 | 说明 |
|------|------|
| `${{ ci.event }}` | 事件类型，如 `PUSH` / `CREATE` / `PULL_REQUEST` / `REVIEW` / `ISSUES` / `NOTE` |
| `${{ ci.action }}` | 事件动作 |
| `${{ ci.actor }}` | 触发人 |
| `${{ ci.branch }}` | 触发分支 |
| `${{ ci.ref }}` | 触发的分支或 Tag 引用 |
| `${{ ci.sha }}` / `${{ ci.sha_short }}` | 本次触发的 commit ID / 短 ID |
| `${{ ci.commit_message }}` | 提交信息 |
| `${{ ci.repo }}` | 仓库全名，如 `owner/repo` |
| `${{ ci.repo_name }}` / `${{ ci.repo_group }}` | 仓库名 / 所属组织或用户 |
| `${{ ci.repo_url }}` | 仓库地址 |
| `${{ ci.repo_alias_name }}` | 蓝盾代码库别名 |
| `${{ ci.event_url }}` | 事件链接 |

### Create Branch Or Tag

| 变量 | 说明 |
|------|------|
| `${{ ci.create_ref }}` | 新建的分支名或 Tag 名 |
| `${{ ci.create_type }}` | 新建类型：`branch` / `tag` |

### Pull Request

| 变量 | 说明 |
|------|------|
| `${{ ci.head_ref }}` / `${{ ci.base_ref }}` | **目标分支 / 源分支**（见下方说明） |
| `${{ ci.mr_id }}` / `${{ ci.mr_iid }}` | PR ID / PR 编号 |
| `${{ ci.mr_url }}` | PR 地址 |
| `${{ ci.mr_title }}` / `${{ ci.mr_desc }}` | PR 标题 / 描述 |
| `${{ ci.mr_proposer }}` | PR 发起人 |
| `${{ ci.mr_action }}` | PR 动作 |
| `${{ ci.mr_labels }}` | PR 标签 |
| `${{ ci.mr_assignee_logins }}` | PR 负责人登录名 |
| `${{ ci.mr_reviewers }}` | PR 评审人 |
| `${{ ci.milestone_name }}` / `${{ ci.milestone_id }}` | 里程碑名称 / ID |

> `ci.head_ref` / `ci.base_ref` 的含义与 GitHub Actions 相反（为兼容历史用法保留）：`head_ref` 是目标分支，`base_ref` 是源分支。

### Code Review / Issue / Note

| 变量 | 说明 |
|------|------|
| `${{ ci.review_id }}` / `${{ ci.review_iid }}` | 评审 ID / 对应 PR 编号 |
| `${{ ci.review_state }}` / `${{ ci.review_owner }}` | 评审状态 / 评审人 |
| `${{ ci.issue_id }}` / `${{ ci.issue_iid }}` | Issue ID / 编号 |
| `${{ ci.issue_title }}` / `${{ ci.issue_description }}` | Issue 标题 / 描述 |
| `${{ ci.issue_state }}` / `${{ ci.issue_owner }}` | Issue 状态 / 创建人 |
| `${{ ci.note_id }}` / `${{ ci.note_comment }}` | 评论 ID / 评论内容 |

---

## PAC YAML 配置

PAC v3.0 中，每个代码库对应 `on` 列表里的一项，用 `type: github` 标识，`repo-name` 填代码库别名。同一项里可同时配置 `push`、`mr` 等多种事件。

> **`type: github` 必须写**。省略 `type` 时一律按 `git` 解析，GitHub 的事件配置会被当成 Git 触发器。`repo-name` 可以省略，省略时监听 PAC 流水线所在的代码库。

> **只有下表中的字段在运行时生效**。YAML 里写了 `paths`、`users`、`source-branches` 等其他 Git 触发器字段也不会参与匹配。

| PAC 关键字 | 生效字段 |
|------------|----------|
| `push` | `branches`、`branches-ignore`、`users-ignore` |
| `mr` | `action`、`target-branches`、`target-branches-ignore`、`users-ignore`、`assignees`、`assignees-ignore`、`labels`、`labels-ignore` |
| `review` | `states` |
| `issue` | `action`、`users-ignore`、`assignees`、`assignees-ignore`、`labels`、`labels-ignore` |
| `note` | `types`、`comment` |

> **PAC 下 `mr` 省略 `action` = 监听全部动作**，与可视化新建时的默认值（`open,reopen,push-update`）不同，建议显式写出。

### Push

```yaml
on:
  - type: github
    repo-name: owner/repo
    push:
      branches:
        - main
        - "release/*"
      branches-ignore:
        - "dev_*"
      users-ignore:
        - "dependabot[bot]"
```

### Pull Request

```yaml
on:
  - type: github
    repo-name: owner/repo
    mr:
      target-branches:
        - main
      action:
        - open
        - reopen
        - push-update
      labels:
        - need-ci
```

### Code Review / Issue / Note

```yaml
on:
  - type: github
    repo-name: owner/repo
    review:
      states:
        - approved
    issue:
      action:
        - open
        - labeled
      labels:
        - bug
    note:
      types:
        - merge_request
      comment:
        - "^/rebuild"
```

### 与其他触发并列

```yaml
on:
  - manual: enabled
  - type: github
    repo-name: owner/repo
    push:
      branches:
        - main
```

---

## 常见问题

**Q：配了触发器却没有启动？**
进入**流水线详情 → 触发事件**标签页，查看对应事件的未匹配原因。常见原因：代码库授权失效或 Webhook 未生效，导致事件没推送过来；分支或操作人没有命中或被排除；PR 动作不在监听范围内。

**Q：PR 配了标签过滤，推送代码时也触发了？**
标签和负责人过滤只在事件本身带标签/负责人时参与判断（即 `labeled`、`unlabeled`、`assigned`、`unassigned` 动作）。`open`、`push-update` 等动作不会按标签过滤。如果只想让带某个标签的 PR 触发，把动作限定为 `labeled` 并配置监听标签。

**Q：评审「通过」了却没有触发 `approved`？**
仓库要求多名评审人时，单人通过后 PR 仍不可合并，此时状态记为 `approving`；满足合并条件后才是 `approved`。需要单人通过就触发时，同时监听 `approving`。

**Q：「创建分支或 Tag」在 PAC 里怎么写？**
该事件暂不支持 PAC。需要在 PAC 流水线里监听新建分支时，可改用 `push` 事件。
