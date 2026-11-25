# TAPD 事件触发

当 TAPD 项目中的**需求（Story）或缺陷（Bug）**被创建、更新、流转状态、关联或评论时，自动启动流水线。

- 入口：**流水线编辑 → 触发器 → 新增「TAPD 事件触发」**
- 可视化配置与 PAC YAML `on`（`type: tapd`）双向同步
- **使用前需先安装 TAPD 应用**（触发器配置面板顶部有安装链接），否则 TAPD 事件不会推送到蓝盾
- TAPD 触发不依赖代码库，按 **TAPD 项目 ID（workspace ID）** 关联

## 能力概览

一个触发器只监听**一个 TAPD 项目的一种工单类型**。

| 事件类型 | PAC 关键字 | 典型场景 |
|----------|-----------|----------|
| 需求（Story） | `story` | 需求流转到「待测试」时自动部署测试环境 |
| 缺陷（Bug） | `bug` | 新建缺陷时自动拉取日志、通知负责人 |

---

## 配置触发器

### 字段说明

| 字段 | 说明 | 示例 |
|------|------|------|
| TAPD 项目 ID | **必填**。下拉仅展示当前用户参与且应用已授权/已安装的 TAPD 项目；未授权项目不展示 | `20001234` |
| 事件类型 | 需求（STORY）/ 缺陷（BUG）；状态候选值按事件类型分别取自 TAPD OpenAPI | — |
| 监听的事件来源 | `api` / `web` / `auto_task`（自动化任务）。默认全选；**PAC 不支持此项，等价于不限制** | — |
| 监听的动作 | 见下方 [动作取值](#动作取值)。默认 `create,update,delete` | — |
| 监听以下优先级 | 多个用英文逗号分隔，按工单优先级**精确匹配**。留空 = 不限制 | `High,Urgent` |
| 监听的状态 | 多选下拉，按 TAPD 状态**中文名**精确匹配。留空 = 监听所有状态 | `规划中,实现中` |
| 排除的状态 | 多选下拉，命中变更后的状态时触发失败；**优先于监听的状态** | `已拒绝` |
| 包含 / 排除以下操作人 | 按触发本次事件的 TAPD 用户过滤 | — |
| 包含 / 排除以下标签 | 多个用英文逗号分隔，支持 `*` 通配。工单有任一标签命中包含即触发；有任一标签命中排除则不触发 | `前端,release-*` |
| 包含 / 排除以下处理人 | 按工单**当前处理人**过滤（多个处理人时取第一个） | — |

- 包含 / 排除类规则留空 = 不限制，**排除优先于包含**；状态过滤对所有 TAPD 事件生效，按变更后的状态匹配，创建等事件使用现有状态字段兜底。
- UI 方式、Code 方式、HTTP 保存接口、流水线模型与 PAC YAML 均使用中文状态名。
- TAPD webhook 仍传入英文状态 key，后端在 webhook 入口实时拉取 TAPD 状态接口完成英文 key 到中文状态名映射；状态数据链路不使用缓存。
- 状态接口失败或英文 key 无法映射为中文名时，打印 error 日志、记录触发失败事件并判定不触发；中文状态名变化后需重新编辑触发器配置。
- 人员、标签、优先级都支持 `${{ variables.xxx }}` 引用流水线变量；需要监听公共账号时，可把账号定义成流水线变量再引用。

### 动作取值

| 动作 | 值 | 需求 | 缺陷 |
|------|----|:----:|:----:|
| 创建 | `create` | ✅ | ✅ |
| 更新 | `update` | ✅ | ✅ |
| 删除 | `delete` | ✅ | ✅ |
| 状态变更 | `status_change` | ✅ | ✅ |
| 添加评论 | `add_comment` | ✅ | ✅ |
| 更新评论 | `update_comment` | ✅ | ✅ |
| 删除评论 | `delete_comment` | ✅ | ✅ |
| 关联缺陷 / 取消关联缺陷 | `bug_link` / `bug_unlink` | ✅ | — |
| 关联需求 / 取消关联需求 | `story_link` / `story_unlink` | ✅ | — |

---

## 输出变量

触发成功后，可在流水线中通过 `${{ ci.* }}` 引用本次事件信息。

| 变量 | 说明 |
|------|------|
| `${{ ci.event }}` | 工单类型：`story` / `bug` |
| `${{ ci.action }}` | 动作，取值见 [动作取值](#动作取值) |
| `${{ ci.actor }}` | 触发人 |
| `${{ ci.event_from }}` | 事件来源：`api` / `web` / `auto_task` |
| `${{ ci.event_id }}` | TAPD 事件 ID |
| `${{ ci.event_url }}` | 工单详情页地址 |
| `${{ ci.tapd_workspace_id }}` | TAPD 项目 ID |
| `${{ ci.tapd_id }}` | 工单 ID |
| `${{ ci.tapd_title }}` | 工单标题 |
| `${{ ci.tapd_parent_id }}` | 父需求 ID |
| `${{ ci.tapd_priority }}` | 优先级 |
| `${{ ci.tapd_link_type }}` | 关联对象类型：`story` / `bug`，仅关联 / 取消关联动作有值 |
| `${{ ci.tapd_link_id }}` | 关联对象 ID，仅关联 / 取消关联动作有值 |
| `${{ ci.repo_alias_name }}` | TAPD 项目名称 |
| `${{ ci.build_msg }}` | 构建信息，格式为 `[动作 工单类型] 标题`，如 `[create story] 登录页改版` |

---

## PAC YAML 配置

PAC v3.0 中，每个 TAPD 项目对应 `on` 列表里的一项，用 `type: tapd` 标识，`workspace-id` 填 TAPD 项目 ID，`story`、`bug` 与 `workspace-id` 平级。

> **`type: tapd` 必须写**。省略 `type` 时一律按 `git` 解析，`story`、`bug` 会被忽略。

| 字段 | 对应可视化字段 |
|------|----------------|
| `action` | 监听的动作 |
| `users` / `users-ignore` | 包含 / 排除以下操作人 |
| `owners` / `owners-ignore` | 包含 / 排除以下处理人 |
| `labels` / `labels-ignore` | 包含 / 排除以下标签 |
| `priorities` | 监听以下优先级 |
| `statuses` | 监听的状态 |
| `statuses-ignore` | 排除的状态 |

> - **省略 `action` = 监听全部动作**，与可视化新建时的默认值（`create,update,delete`）不同，建议显式写出。
> - `statuses` / `statuses-ignore` 使用 TAPD 中文状态名；`statuses` 省略表示监听所有状态，`statuses-ignore` 命中时排除优先。
> - PAC 不支持「监听的事件来源」，所有来源的事件都会参与匹配。
> - 同一个 `workspace-id` 下 `story`、`bug` **各只能配置一个**。可视化里同一项目配了多个需求触发器时，转成 YAML 只会保留最后一个。
> - 存量环境执行增量 DML 更新 `T_ATOM.PROPS` 后，需失效 `AtomPropsCacheManager` 中 `codeTapdWebHookTrigger` 的缓存（或等待最长 6 小时），并刷新页面清理前端 `atomModalMap`，否则新下拉和状态字段不会立即生效。
> - 项目下拉接口仅返回已授权项目的 `{ id, name, disabled }`，状态多选下拉复用现有 `devops-select` group children 能力，无需修改前端组件代码。

### 需求和缺陷

```yaml
on:
  - type: tapd
    workspace-id: "20001234"
    story:
      action:
        - create
        - status_change
      owners:
        - zhangsan
      labels:
        - 前端
      priorities:
        - High
      statuses:
        - 规划中
        - 实现中
      statuses-ignore:
        - 已拒绝
    bug:
      action:
        - create
      users-ignore:
        - robot
      statuses:
        - 新
      statuses-ignore:
        - 已关闭
```

### 多个 TAPD 项目

```yaml
on:
  - type: tapd
    workspace-id: "20001234"
    story:
      action:
        - create
  - type: tapd
    workspace-id: "20005678"
    bug:
      action:
        - create
        - update
```

### 与其他触发并列

```yaml
on:
  - manual: enabled
  - type: git
    repo-name: group/repo
    push:
      branches:
        - master
  - type: tapd
    workspace-id: "20001234"
    story:
      action:
        - status_change
```

---

## 常见问题

**Q：配了触发器却没有启动？**
进入**流水线详情 → 触发事件**标签页，查看对应事件的未匹配原因。常见原因：没有安装 TAPD 应用；TAPD 项目 ID 填错；项目未在已授权列表中导致下拉不展示；动作、事件来源、优先级、标签、状态或处理人没有命中；操作人、处理人或状态被排除；流水线被锁定。

**Q：修改了需求状态，为什么 `update` 没触发？**
状态流转在 TAPD 中是独立的 `status_change` 动作，需要监听状态变更时请勾选「状态变更」。

**Q：优先级填了「高」却匹配不上？**
优先级按 TAPD 推送的优先级取值精确匹配，请以 TAPD 项目中配置的优先级名称为准（区分大小写和中英文）。

**Q：状态下拉显示中文，保存后 YAML 里是什么？**
模型与 PAC YAML 保存 TAPD 中文状态名，UI 与 Code 方式填写值一致；`statuses` 留空表示监听全部状态，`statuses-ignore` 命中时排除优先。

**Q：通过 OpenAPI 或自动化任务改的工单能触发吗？**
可以。可视化中「监听的事件来源」默认包含 `api`、`web`、`auto_task`；只想响应页面操作时只勾选 `web`。PAC 不支持该项，所有来源都会触发。

## 插件表单国际化

- `T_ATOM.PROPS` 保存默认语言文案；当前默认语言为 `zh_CN`，中文界面直接使用 PROPS 中的中文。
- `en_US` / `ja_JP` 等非默认语言由 `T_I18N_MESSAGE` 按 `ATOM.codeTapdWebHookTrigger.1.0.0.input...` key 替换。
- 修改 PROPS 或 i18n 后，需清理 `STORE_ATOM_TASK_JSON_KEY:codeTapdWebHookTrigger`、相关 `BK_I18N_MESSAGE:*` 缓存，并硬刷新页面清理前端 `atomModalMap`。


- 状态过滤字段位于「状态设置」分组，置于「处理人设置」之后；复用现有 `devops-select` group children 能力，不修改前端代码。
