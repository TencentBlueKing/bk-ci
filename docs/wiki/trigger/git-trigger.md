# Git 事件触发

当工蜂（Git）代码库发生**推送、Tag、合并请求、代码评审、Issue、评论**等事件时，自动启动流水线。

- 入口：**流水线编辑 → 触发器 → 新增「Git 事件触发」**
- 可视化配置与 PAC YAML `on`（`type: git`）双向同步
- 需要先在**代码库**中关联对应的工蜂仓库（建议使用 OAuth 授权），蓝盾会自动注册 Webhook

## 能力概览

一个触发器只监听**一种事件类型**，需要监听多种事件时添加多个触发器。

| 事件类型 | PAC 关键字 | 典型场景 |
|----------|-----------|----------|
| Commit Push Hook | `push` | 推代码 / 新建分支后自动构建 |
| Tag Push Hook | `tag` | 打 Tag 后自动发布 |
| Merge Request Hook | `mr` | 提 MR / MR 更新时跑检查，结果回写到 MR |
| Merge Request Accept Hook（**2.0 已下线**） | `mr-merged` | 仅兼容存量配置，新配置请用 `mr` + `action: merge` |
| Code Review Hook | `review` | 评审通过后自动部署 |
| Issue Hook | `issue` | Issue 创建/更新时同步到其他系统 |
| Note Hook | `note` | 在评论里写指令（如 `/rebuild`）触发 |

---

## 配置触发器

### 通用说明

- **代码库**：可视化支持「按代码库选择」或「按代码库别名输入」（别名可写成流水线变量）；PAC 使用 `repo-name` 填代码库别名，省略时监听 PAC 流水线所在的代码库。
- **包含 / 排除规则**：多个值用英文逗号分隔（PAC 写成列表）；留空 = 不限制；**排除优先于包含**。
- **流水线变量**：分支、路径、人员、标签等输入框都支持 `${{ variables.xxx }}` 引用流水线变量。
- **人员过滤**：按触发人的用户名**精确匹配**。
- 分支 / Tag 的匹配语法见 [分支匹配语法](#分支匹配语法)，路径的匹配语法见 [路径匹配语法](#路径匹配语法)。

### Commit Push Hook（push）

| 字段 | 说明 | 示例 |
|------|------|------|
| 动作 | `push-file`：推送代码变更；`new-branch`：新建分支。默认两者都监听 | — |
| 监听以下分支 | 留空 = 全部分支 | `master,release/*` |
| 排除以下分支 | 命中则不触发 | `dev_*` |
| 路径匹配方式 | 前缀匹配（默认）/ 通配符匹配 | — |
| 监听以下路径 | 本次提交变更的文件中，有任一命中即触发 | `src/,docs/` |
| 排除以下路径 | 参与判断的变更文件**全部**命中排除路径时不触发，见 [常见问题](#常见问题) | `docs/` |
| 包含以下人员 / 排除以下人员 | 按推送人过滤 | — |
| 第三方过滤 | 可选。填写第三方服务地址和凭证，由第三方服务判断是否触发 | — |

### Tag Push Hook（tag）

| 字段 | 说明 | 示例 |
|------|------|------|
| 动作 | `create`：创建 Tag；`delete`：删除 Tag。默认两者都监听 | — |
| 监听以下 Tag | 留空 = 全部 Tag | `v*` |
| 排除以下 Tag | 命中则不触发 | `*-beta` |
| 监听以下来源分支 | Tag 从哪些分支创建。**通过 Git 客户端创建的 Tag 不带来源分支信息**，此时该条件无法命中 | `master` |
| 包含以下人员 / 排除以下人员 | 按创建 Tag 的人过滤 | — |

### Merge Request Hook（mr）

| 字段 | 说明 | 示例 |
|------|------|------|
| 动作 | `open` 创建 / `close` 关闭 / `reopen` 重新打开 / `push-update` 源分支有推送 / `merge` 已合并 / `edit` 编辑。默认 `open,reopen,push-update` | — |
| 监听以下目标分支 / 排除以下目标分支 | 按 MR 的目标分支过滤 | `master` |
| 监听以下源分支 / 排除以下源分支 | 按 MR 的源分支过滤 | `feature/*` |
| 路径匹配方式 / 监听路径 / 排除路径 | 含义同 push，按 MR 的变更文件判断 | `src/` |
| 包含以下人员 / 排除以下人员 | 按 MR 操作人过滤 | — |
| 监听以下标签 / 排除以下标签 | 按 MR 标签过滤 | `need-ci` |
| 跳过 WIP | 勾选后，标题含 `WIP` 或 `[WIP]` 的 MR 不触发 | — |
| 回写 commit check | 默认开启，构建结果回写到 MR | — |
| 锁定提交 | 构建完成前禁止合并 MR。**只在开启回写 commit check 时生效** | — |
| 第三方过滤 | 同 push | — |

### Code Review Hook（review）

| 字段 | 说明 |
|------|------|
| 监听以下状态 | `approving` 评审中 / `approved` 评审通过 / `change_denied` 评审被拒绝 / `change_required` 要求修改。留空 = 全部 |
| 监听以下评审类型 | `merge_request`：MR 评审；`commit`：Commit 评审。留空 = 全部（仅 PAC 可配） |

### Issue Hook（issue）

| 字段 | 说明 |
|------|------|
| 监听以下动作 | `open` 创建 / `close` 关闭 / `reopen` 重新打开 / `update` 更新。留空 = 全部 |

### Note Hook（note）

| 字段 | 说明 | 示例 |
|------|------|------|
| 监听以下评论类型 | 可视化为「Commit / Review / Issue」三类；PAC 写 `commit` / `merge_request` / `issue`。留空 = 全部 | — |
| 监听以下评论内容 | **正则表达式**，评论内容命中任一条即触发。留空 = 全部 | `^/rebuild` |

### 分支匹配语法

分支、Tag 使用 Ant 风格通配符，**大小写敏感**：

| 通配符 | 含义 | 示例 |
|--------|------|------|
| `*` | 匹配一段文字，不跨 `/` | `release/*` 命中 `release/1.0`，不命中 `release/1.0/hotfix` |
| `**` | 可跨多层 `/` | `release/**` 命中 `release/1.0/hotfix` |
| `?` | 匹配单个字符 | `v1.?` 命中 `v1.2` |

### 路径匹配语法

| 路径匹配方式 | PAC 值 | 规则 | 示例 |
|--------------|--------|------|------|
| 前缀匹配（默认） | `NamePrefixFilter` | 变更文件路径以配置值开头即命中 | `src/` 命中 `src/main/a.kt` |
| 通配符匹配 | `RegexBasedFilter` | Ant 风格通配符，语法同分支匹配 | `src/**/*.kt` |

---

## 输出变量

触发成功后，可在流水线中通过 `${{ ci.* }}` 引用本次事件信息。与当前事件类型无关的变量为空。

### 公共（所有事件）

| 变量 | 说明 |
|------|------|
| `${{ ci.event }}` | 事件类型，如 `PUSH` / `TAG_PUSH` / `MERGE_REQUEST` / `REVIEW` / `ISSUES` / `NOTE` |
| `${{ ci.action }}` | 事件动作，如 `push-file` / `open` |
| `${{ ci.actor }}` | 触发人 |
| `${{ ci.branch }}` | 触发分支。MR 事件为源分支，`merge` 动作时为目标分支 |
| `${{ ci.ref }}` | 触发的分支或 Tag 引用 |
| `${{ ci.sha }}` / `${{ ci.sha_short }}` | 本次触发的 commit ID / 短 ID |
| `${{ ci.before_sha }}` / `${{ ci.before_sha_short }}` | 推送前的 commit ID / 短 ID |
| `${{ ci.commit_message }}` | 提交信息 |
| `${{ ci.commit_author }}` | 提交作者 |
| `${{ ci.repo }}` | 仓库全名，如 `group/repo` |
| `${{ ci.repo_name }}` / `${{ ci.repo_group }}` | 仓库名 / 仓库所属组 |
| `${{ ci.repo_url }}` | 仓库地址 |
| `${{ ci.repo_alias_name }}` | 蓝盾代码库别名 |
| `${{ ci.repo_type }}` | 代码库类型 |
| `${{ ci.event_url }}` | 事件链接（如 MR、Review 页面地址） |

### Tag

| 变量 | 说明 |
|------|------|
| `${{ ci.tag_from }}` | Tag 来源分支 |
| `${{ ci.tag_desc }}` | Tag 描述 |

### Merge Request

| 变量 | 说明 |
|------|------|
| `${{ ci.head_ref }}` / `${{ ci.base_ref }}` | **目标分支 / 源分支**（见下方说明） |
| `${{ ci.head_repo_url }}` / `${{ ci.base_repo_url }}` | **目标仓库地址 / 源仓库地址** |
| `${{ ci.mr_id }}` / `${{ ci.mr_iid }}` | MR ID / 仓库内编号 |
| `${{ ci.mr_url }}` | MR 地址 |
| `${{ ci.mr_title }}` / `${{ ci.mr_desc }}` | MR 标题 / 描述 |
| `${{ ci.mr_proposer }}` | MR 发起人 |
| `${{ ci.mr_action }}` | MR 动作 |
| `${{ ci.mr_labels }}` | MR 标签 |
| `${{ ci.mr_reviewers }}` | MR 评审人 |
| `${{ ci.milestone_name }}` / `${{ ci.milestone_id }}` | 里程碑名称 / ID |

> `ci.head_ref` / `ci.base_ref` 的含义与 GitHub Actions 相反（历史原因保留）：`head_ref` 是目标分支，`base_ref` 是源分支。

### Code Review

| 变量 | 说明 |
|------|------|
| `${{ ci.review_id }}` / `${{ ci.review_iid }}` | 评审 ID / 仓库内编号 |
| `${{ ci.review_type }}` | 评审类型：`merge_request` / `commit` |
| `${{ ci.review_state }}` | 评审状态 |
| `${{ ci.review_owner }}` | 评审发起人 |
| `${{ ci.review_reviewers }}` | 评审人 |

### Issue

| 变量 | 说明 |
|------|------|
| `${{ ci.issue_id }}` / `${{ ci.issue_iid }}` | Issue ID / 仓库内编号 |
| `${{ ci.issue_title }}` / `${{ ci.issue_description }}` | 标题 / 描述 |
| `${{ ci.issue_state }}` | 状态 |
| `${{ ci.issue_owner }}` | 创建人 |
| `${{ ci.issue_milestone_id }}` | 里程碑 ID |

### Note

| 变量 | 说明 |
|------|------|
| `${{ ci.note_id }}` | 评论 ID |
| `${{ ci.note_comment }}` | 评论内容 |
| `${{ ci.note_type }}` | 评论类型 |
| `${{ ci.note_author }}` | 评论人 |

---

## PAC YAML 配置

PAC v3.0 中，事件写在 `on` 下，同一个代码库可同时配置 `push`、`tag`、`mr` 等多种事件。字段用小写 kebab-case，列表字段对应可视化里逗号分隔的多个值，空值和默认值可省略。

`type` 和 `repo-name` 都可以省略：

| 字段 | 说明 | 省略时 |
|------|------|--------|
| `type` | 代码库类型，Git 为 `git` | 按 `git` 处理。其他类型的触发（GitHub、SVN、TAPD 等）必须写 `type` |
| `repo-name` | 要监听的代码库别名 | 监听 PAC 流水线所在的代码库 |

> **省略 `action` 时的默认值**：`push` 默认 `push-file,new-branch`；`tag` 默认 `create,delete`；`mr` 默认 `open,reopen,push-update`。

### 关键字速查

**所有事件通用**

| 关键字 | 类型 | 说明 |
|--------|------|------|
| `id` | 字符串 | 触发器的 step ID，可选 |
| `name` | 字符串 | 触发器名称，省略 = `Git事件触发` |
| `enable` | 布尔 | 是否启用，省略 = `true` |

**`push`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `branches` / `branches-ignore` | 列表 | 监听 / 排除以下分支 |
| `paths` / `paths-ignore` | 列表 | 监听 / 排除以下路径 |
| `path-filter-type` | `NamePrefixFilter` / `RegexBasedFilter` | 路径匹配方式，省略 = `NamePrefixFilter` |
| `users` / `users-ignore` | 列表 | 包含 / 排除以下人员 |
| `action` | 列表：`push-file` / `new-branch` | 动作 |
| `custom-filter.url` / `custom-filter.credentials` | 字符串 | 第三方过滤的服务地址 / 凭证 ID |

**`tag`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `tags` / `tags-ignore` | 列表 | 监听 / 排除以下 Tag |
| `from-branches` | 列表 | 监听以下来源分支 |
| `users` / `users-ignore` | 列表 | 包含 / 排除以下人员 |
| `action` | 列表：`create` / `delete` | 动作 |

**`mr` / `mr-merged`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `target-branches` / `target-branches-ignore` | 列表 | 监听 / 排除以下目标分支 |
| `source-branches` / `source-branches-ignore` | 列表 | 监听 / 排除以下源分支 |
| `paths` / `paths-ignore` | 列表 | 监听 / 排除以下路径 |
| `path-filter-type` | `NamePrefixFilter` / `RegexBasedFilter` | 路径匹配方式 |
| `users` / `users-ignore` | 列表 | 包含 / 排除以下人员 |
| `labels` / `labels-ignore` | 列表 | 监听 / 排除以下标签 |
| `action` | 列表：`open` / `close` / `reopen` / `push-update` / `merge` / `edit` | 动作，**仅 `mr` 生效** |
| `skip-wip` | 布尔 | 跳过 WIP，省略 = `false` |
| `report-commit-check` | 布尔 | 回写 commit check，省略 = `true` |
| `block-mr` | 布尔 | 锁定提交，**仅 `mr` 且开启回写时生效** |
| `webhook-queue` | 布尔 | Webhook 排队，省略 = `false` |
| `custom-filter.url` / `custom-filter.credentials` | 字符串 | 第三方过滤 |

> `mr-merged` 对应 Merge Request Accept Hook，**该事件在 Git 事件触发 2.0 中已下线**，仅为兼容存量配置保留（转换后按 1.* 版本生成）。它固定只在合并时触发，写了 `action`、`block-mr` 也不生效。新配置请用 `mr` + `action: [merge]`。

**`review`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `states` | 列表：`approving` / `approved` / `change_denied` / `change_required` | 监听以下状态 |
| `types` | 列表：`merge_request` / `commit` | 评审类型（仅 PAC 可配） |

**`issue`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `action` | 列表：`open` / `close` / `reopen` / `update` | 监听以下动作 |

**`note`**

| 关键字 | 类型 | 对应可视化字段 |
|--------|------|----------------|
| `types` | 列表：`commit` / `merge_request` / `issue` | 监听以下评论类型（对应可视化的 Commit / Review / Issue） |
| `comment` | 列表，正则表达式 | 监听以下评论内容 |

### 简写（监听 PAC 所在代码库）

只监听 PAC 流水线所在的代码库时，`on` 直接写成对象，事件和手动、定时等基础触发平铺在一起即可：

```yaml
on:
  manual: enabled
  push:
    branches:
      - master
  mr:
    target-branches:
      - master
```

### 监听其他代码库

需要监听其他代码库或同时监听多个代码库时，`on` 写成列表，每个代码库一项，用 `repo-name` 指定代码库别名。下面的示例都用这种写法，其中 `type: git` 可省略。

### Push

```yaml
on:
  - type: git
    repo-name: group/repo
    push:
      branches:
        - master
        - "release/*"
      branches-ignore:
        - "dev_*"
      path-filter-type: RegexBasedFilter   # 省略 = NamePrefixFilter（前缀匹配）
      paths:
        - "src/**"
      paths-ignore:
        - "docs/**"
      users-ignore:
        - robot
      action:
        - push-file
        - new-branch
```

### Tag

```yaml
on:
  - type: git
    repo-name: group/repo
    tag:
      tags:
        - "v*"
      tags-ignore:
        - "*-beta"
      from-branches:
        - master
      action:
        - create
```

### Merge Request

```yaml
on:
  - type: git
    repo-name: group/repo
    mr:
      target-branches:
        - master
      source-branches-ignore:
        - "tmp/*"
      paths:
        - "src/"
      labels:
        - need-ci
      action:
        - open
        - reopen
        - push-update
      skip-wip: true
      report-commit-check: true   # 默认 true，可省略
      block-mr: true              # 需同时开启 report-commit-check
      webhook-queue: false
```

### 第三方过滤

`push` 和 `mr` 支持：

```yaml
on:
  - type: git
    repo-name: group/repo
    push:
      branches:
        - master
      custom-filter:
        url: https://example.com/ci/filter
        credentials: my-credential-id
```

### Code Review / Issue / Note

```yaml
on:
  - type: git
    repo-name: group/repo
    review:
      states:
        - approved
      types:
        - merge_request
    issue:
      action:
        - open
        - reopen
    note:
      types:
        - merge_request
      comment:
        - "^/rebuild"
```

### 多个代码库 / 与其他触发并列

```yaml
on:
  - manual: enabled
    schedules:
      - cron: "0 2 * * *"
  - type: git
    repo-name: group/repo-a
    push:
      branches:
        - master
  - type: git
    repo-name: group/repo-b
    mr:
      target-branches:
        - master
```

---

## 常见问题

**Q：配了触发器却没有启动？**
进入**流水线详情 → 触发事件**标签页，查看对应事件的未匹配原因。常见原因：分支/路径/人员没有命中或被排除；动作（action）不在监听范围内；代码库 OAuth 授权失效导致 Webhook 未注册；MR 标题含 WIP 且勾选了跳过 WIP。

**Q：路径的「包含」和「排除」同时配置时怎么判断？**
先按包含路径筛出命中的变更文件（未配包含路径 = 全部变更文件），再看这些文件是否**全部**命中排除路径：全部命中则不触发，只要有一个没被排除就触发。例如包含 `src/`、排除 `src/test/`，只改了 `src/test/a.kt` 不触发，同时改了 `src/main/b.kt` 会触发。

**Q：锁定提交勾了但 MR 仍能合并？**
锁定提交依赖回写 commit check，关闭「回写 commit check」时锁定提交不生效。

**Q：Tag 事件配了来源分支，却一直不触发？**
通过 Git 客户端（`git tag` + `git push`）创建的 Tag 不带来源分支信息，来源分支条件无法命中。需要按来源分支过滤时，请在工蜂页面上从指定分支创建 Tag。

**Q：新建分支会触发 push 吗？**
会。`push` 默认同时监听 `push-file` 和 `new-branch`；只想在推送代码时触发，把动作设为 `push-file`。
