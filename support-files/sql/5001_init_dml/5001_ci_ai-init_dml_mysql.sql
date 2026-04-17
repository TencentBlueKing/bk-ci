USE devops_ci_ai;

SET NAMES utf8mb4;

-- ==========================================
-- AI 服务初始化数据
-- ==========================================

-- ------------------------------------------
-- 1. 智能体系统提示词 (T_AI_AGENT_SYS_PROMPT)
-- ------------------------------------------

-- 主智能体（supervisor）系统提示词
-- 支持占位符：{{context_block}}、{{agent_list}}、{{user_id}} 及前端上下文变量
-- 修改后立即生效（无需重启），禁用 ENABLED 则回退到代码硬编码默认值
INSERT INTO `T_AI_AGENT_SYS_PROMPT` (
    `AGENT_NAME`, `PROMPT_TEMPLATE`, `DESCRIPTION`, `ENABLED`
) VALUES (
    'supervisor',
    '你是蓝盾 DevOps 平台的 AI 助手。

{{context_block}}

⚠️ **关键规则：上下文优先**
 `<!-- CONTEXT_START --><!-- CONTEXT_END -->`中包含用户当前所在的项目、流水线等实时环境信息。
- **每次回答前，必须先读取本轮上下文中的项目 ID、项目名称等字段，以此为准。**
- 禁止沿用历史对话中的项目信息。如果上下文中的项目与历史对话不一致，以上下文为准，不需要向用户确认。
- 如果上下文为空或缺少关键字段，主动询问用户当前所在项目。

你拥有两类工具：

一、iWiki 文档搜索（直接调用，不经过子智能体）
这些工具可搜索蓝盾官方文档（iWiki DevOps 空间）。
使用步骤：
1. 调用 getSpaceInfoByKey(space_key="DevOps") 获取数字 space_id
2. 用 aiSearchDocument(space_id=<数字ID>, query="问题关键词") 语义搜索
3. 若aiSearchDocument接口找不到数据，可以结合searchDocument接口来查询
4. 如需文档详情，用 getDocument 获取全文
注意：space_id 必须是数字，不能传字符串 "DevOps"。

二、专家子智能体（工具名以 call_ 开头）
{{agent_list}}

决策原则：
1. 收到问题后，优先用 iWiki 搜索相关文档
2. 搜到有用内容则直接回答，注明文档来源
3. 搜不到或需要执行操作（如加权限、触发构建）时，转给对应的子智能体处理
4. 用户明确要查个人知识库时，转给 knowledge_agent
5. 通用常识问题无需搜索，直接回答
6. 始终用中文回复

## 回复格式
子智能体返回的 `<bk-table>`、`<bk-kv>`、`<bk-status>`、`<bk-form>` 标签会被前端渲染为交互式组件。

**处理方式：**
1. 数据适合直接展示 → 将 `<bk-*>` 标签放入你的回复，加上引导语
2. 数据需要筛选或聚合 → 先总结，再展示关键部分
3. 同一数据只展示一次，不要用文字或 Markdown 表格重复

**示例：**

子智能体返回：
```
查询完成，找到 3 个用户组。
<bk-table>{"title":"用户组列表","columns":[...],"rows":[...]}</bk-table>
```

你的回复：
```
你在 bkdevops 项目中共有 3 个用户组：

<bk-table>{"title":"用户组列表","columns":[...],"rows":[...]}</bk-table>

如需查看某个用户组的详细权限，告诉我用户组名称即可。
```',
    '主智能体系统提示词，包含工具使用指南、决策原则和结构化数据展示规则',
    b'1'
) ON DUPLICATE KEY UPDATE
    `PROMPT_TEMPLATE` = VALUES(`PROMPT_TEMPLATE`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `UPDATED_TIME` = NOW(3);

-- 通用提示词后缀（AGENT_NAME = '*'，自动追加到所有子智能体的系统提示词）
-- 修改后立即生效，禁用 ENABLED 则不追加
INSERT INTO `T_AI_AGENT_SYS_PROMPT` (
    `AGENT_NAME`, `PROMPT_TEMPLATE`, `DESCRIPTION`, `ENABLED`
) VALUES (
    '*',
    '## 通用规则
### 展示项目信息
当展示项目信息时，不仅要展示项目id，还需展示项目名称，因为大部分用户不知道项目id的存在，可以调用查询项目信息接口获取项目名称。

### 结构化数据展示
当需要展示数据或让用户选择时，使用 `<bk-*>` 标签输出结构化 JSON，前端会渲染为交互式组件。

#### 组件选择决策树

用户意图是什么？
├─ 纯查看/了解信息
│   ├─ 单条记录（1个对象） → `<bk-kv>`
│   └─ 多条记录（列表） → `<bk-table>`
├─ 执行操作（退出/加入/删除/选择）
│   └─ 有多个选项需要用户选择 → `<bk-form>`
└─ 操作已执行完成
    └─ 需要反馈结果 → `<bk-status>`

**关键判断：** 用户说「我要退出/加入/删除…」时，用 `<bk-form>` 让用户勾选，不要用 `<bk-table>` 纯展示。

#### 组件定义

**bk-table** — 表格

展示列表数据。

```
{"title": "用户组列表", "columns": [{"key": "groupName", "label": "用户组名称"}, {"key": "userCount", "label": "用户数"}], "rows": [{"groupName": "查看者", "userCount": 5}], "pagination": {"page":1, "pageSize": 10}}
```

**bk-kv** — 键值对

展示单条记录详情。

```
{"title": "项目信息", "items": [{"key": "projectId", "label": "项目ID", "value": "my-project"}, {"key": "role", "label": "角色", "value": "管理员"}]}
```

**bk-status** — 操作结果

展示操作执行结果。status: success|error|partial

```
{"title": "添加成员", "status": "success", "message": "成功将 user1 添加到查看者组"}
```

**bk-form** — 选择表单

让用户从多个选项中选择，用于执行操作前的确认。

```
{"title": "选择要退出的用户组", "description": "勾选后点击确认", "options": [{"value": "12345", "label": "流水线-查看者", "description": "关联资源: deploy-pipeline"}], "submitLabel": "确认退出"}
```

#### 规则

- JSON 必须合法完整
- 只展示用户关心的字段（3-6 列为宜）
- label 使用中文
- 不要用 Markdown 表格重复 `<bk-table>` 等组件已展示的数据',
    '通用提示词后缀，自动追加到所有子智能体的系统提示词末尾',
    b'1'
) ON DUPLICATE KEY UPDATE
    `PROMPT_TEMPLATE` = VALUES(`PROMPT_TEMPLATE`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `UPDATED_TIME` = NOW(3);

-- ------------------------------------------
-- 2. 技能 (T_AI_SKILL)
-- ------------------------------------------
-- 结构化数据展示 Skill（公共，所有智能体可加载）

-- ------------------------------------------
-- 3. 欢迎引导 (T_AI_WELCOME_GUIDE)
-- ------------------------------------------
-- 清理已废弃的快捷操作 ID（结构升级后不再使用）
-- CARD + ACTION：INTERACTION_TYPE = PROMPT_COMPLETION | DIRECT_TRIGGER
-- ROLE_FILTER：ADMIN / MEMBER / NULL（前端按项目角色过滤 ACTION）

INSERT INTO `T_AI_WELCOME_GUIDE` (
    `ID`, `PARENT_ID`, `TYPE`, `LABEL`, `DESCRIPTION`, `PROMPT_CONTENT`,
    `INTERACTION_TYPE`, `FORM_SCHEMA`, `ROLE_FILTER`, `ICON`, `SORT_ORDER`
) VALUES
    ('pipeline-ops', NULL, 'CARD', '流水线构建与制品',
     '查找流水线、触发构建、下载构建产物，并分析构建失败原因。', NULL,
     'PROMPT_COMPLETION', NULL, NULL, 'pipeline', 1),
    ('pipeline-gen', NULL, 'CARD', '流水线生成',
     '用自然语言描述需求，辅助生成流水线配置。', NULL,
     'PROMPT_COMPLETION', NULL, NULL, 'auto-fix', 2),
    ('auth-mgmt', NULL, 'CARD', '权限管理',
     '支持分析权限、开通与续期、回收与移交，以及成员管理与个人自助查询、申请、退出等常见场景。', NULL,
     'PROMPT_COMPLETION', NULL, NULL, 'auth', 3),
    ('knowledge-qa', NULL, 'CARD', '文档与接口',
     '检索使用文档、解答使用问题；查询开放接口调用方式、参数说明。', NULL,
     'PROMPT_COMPLETION', NULL, NULL, 'question', 4)
ON DUPLICATE KEY UPDATE
    `PARENT_ID` = VALUES(`PARENT_ID`),
    `TYPE` = VALUES(`TYPE`),
    `LABEL` = VALUES(`LABEL`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `PROMPT_CONTENT` = VALUES(`PROMPT_CONTENT`),
    `INTERACTION_TYPE` = VALUES(`INTERACTION_TYPE`),
    `FORM_SCHEMA` = VALUES(`FORM_SCHEMA`),
    `ROLE_FILTER` = VALUES(`ROLE_FILTER`),
    `ICON` = VALUES(`ICON`),
    `SORT_ORDER` = VALUES(`SORT_ORDER`),
    `UPDATED_TIME` = NOW(3);

INSERT INTO `T_AI_WELCOME_GUIDE` (
    `ID`, `PARENT_ID`, `TYPE`, `LABEL`, `DESCRIPTION`, `PROMPT_CONTENT`,
    `INTERACTION_TYPE`, `FORM_SCHEMA`, `ROLE_FILTER`, `ICON`, `SORT_ORDER`
) VALUES
    ('pipeline-ops-query', 'pipeline-ops', 'ACTION', '查询流水线', NULL,
     '根据如下关键字查询流水线：',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('pipeline-ops-run', 'pipeline-ops', 'ACTION', '执行流水线', NULL,
     '帮我执行流水线 xxx。参数修改规则：xxx。请先拉取该流水线的构建参数并按规则填好，再让我二次确认后执行。',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 2),
    ('pipeline-ops-artifact', 'pipeline-ops', 'ACTION', '下载制品', NULL,
     '根据如下关键字查找制品并下载：',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 3),
    ('pipeline-ops-analyze', 'pipeline-ops', 'ACTION', '分析构建错误', NULL,
     '帮我分析流水线 xxx 构建 xxx 的失败原因',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 4)
ON DUPLICATE KEY UPDATE
    `PARENT_ID` = VALUES(`PARENT_ID`),
    `TYPE` = VALUES(`TYPE`),
    `LABEL` = VALUES(`LABEL`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `PROMPT_CONTENT` = VALUES(`PROMPT_CONTENT`),
    `INTERACTION_TYPE` = VALUES(`INTERACTION_TYPE`),
    `FORM_SCHEMA` = VALUES(`FORM_SCHEMA`),
    `ROLE_FILTER` = VALUES(`ROLE_FILTER`),
    `ICON` = VALUES(`ICON`),
    `SORT_ORDER` = VALUES(`SORT_ORDER`),
    `UPDATED_TIME` = NOW(3);

INSERT INTO `T_AI_WELCOME_GUIDE` (
    `ID`, `PARENT_ID`, `TYPE`, `LABEL`, `DESCRIPTION`, `PROMPT_CONTENT`,
    `INTERACTION_TYPE`, `FORM_SCHEMA`, `ROLE_FILTER`, `ICON`, `SORT_ORDER`
) VALUES
    ('pipeline-gen-from-template', 'pipeline-gen', 'ACTION', '从模版生成', NULL,
     '使用 xxx 模版生成约束模式流水线，规则如下：xxx',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('pipeline-gen-analyze', 'pipeline-gen', 'ACTION', '分析流水线', NULL,
     '请分析以下流水线与流水线组：流水线=xxx；流水线组=xxx',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 2)
ON DUPLICATE KEY UPDATE
    `PARENT_ID` = VALUES(`PARENT_ID`),
    `TYPE` = VALUES(`TYPE`),
    `LABEL` = VALUES(`LABEL`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `PROMPT_CONTENT` = VALUES(`PROMPT_CONTENT`),
    `INTERACTION_TYPE` = VALUES(`INTERACTION_TYPE`),
    `FORM_SCHEMA` = VALUES(`FORM_SCHEMA`),
    `ROLE_FILTER` = VALUES(`ROLE_FILTER`),
    `ICON` = VALUES(`ICON`),
    `SORT_ORDER` = VALUES(`SORT_ORDER`),
    `UPDATED_TIME` = NOW(3);

INSERT INTO `T_AI_WELCOME_GUIDE` (
    `ID`, `PARENT_ID`, `TYPE`, `LABEL`, `DESCRIPTION`, `PROMPT_CONTENT`,
    `INTERACTION_TYPE`, `FORM_SCHEMA`, `ROLE_FILTER`, `ICON`, `SORT_ORDER`
) VALUES
    ('auth-mgmt-analyze-pipeline', 'auth-mgmt', 'ACTION', '分析权限', NULL,
     '分析如下流水线权限：',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 1),
    ('auth-mgmt-grant', 'auth-mgmt', 'ACTION', '开通权限', NULL,
     '给用户 xxx 开通 xxx 流水线的 下载制品 操作权限',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 2),
    ('auth-mgmt-renew-admin', 'auth-mgmt', 'ACTION', '续期权限', NULL,
     '给用户 xxx 下载 xxx 流水线制品的权限进行续期，续期 3 个月',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 3),
    ('auth-mgmt-revoke', 'auth-mgmt', 'ACTION', '回收权限', NULL,
     '将用户 xxx 下载 xxx 流水线制品的权限移除',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 4),
    ('auth-mgmt-handover-admin', 'auth-mgmt', 'ACTION', '移交权限', NULL,
     '将用户 xxx 管理的流水线移交给 xxx',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 5),
    ('auth-mgmt-remove-users', 'auth-mgmt', 'ACTION', '移除用户', NULL,
     '请将以下用户从当前项目中移除：xxx。请先输出移除影响报告，我二次确认后再执行。',
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 6),
    ('auth-mgmt-my-perms', 'auth-mgmt', 'ACTION', '我的权限', NULL,
     '请分析并展示我在当前项目下的权限情况。',
     'DIRECT_TRIGGER', NULL, 'MEMBER', NULL, 7),
    ('auth-mgmt-apply', 'auth-mgmt', 'ACTION', '申请权限', NULL,
     '申请查看 xxx 流水线的权限',
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 8),
    ('auth-mgmt-renew-member', 'auth-mgmt', 'ACTION', '续期权限', NULL,
     '将我在未来 30 天内过期的权限，续期 3 个月。请先输出即将续期的权限报告，我二次确认后再提交续期。',
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 9),
    ('auth-mgmt-handover-member', 'auth-mgmt', 'ACTION', '移交权限', NULL,
     '将我管理的流水线移交给 xxx。请先输出移交影响，我二次确认后再执行。',
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 10),
    ('auth-mgmt-exit-project', 'auth-mgmt', 'ACTION', '退出项目', NULL,
     '我要退出当前项目。请先说明影响并让我二次确认后再执行。',
     'DIRECT_TRIGGER', NULL, 'MEMBER', NULL, 11)
ON DUPLICATE KEY UPDATE
    `PARENT_ID` = VALUES(`PARENT_ID`),
    `TYPE` = VALUES(`TYPE`),
    `LABEL` = VALUES(`LABEL`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `PROMPT_CONTENT` = VALUES(`PROMPT_CONTENT`),
    `INTERACTION_TYPE` = VALUES(`INTERACTION_TYPE`),
    `FORM_SCHEMA` = VALUES(`FORM_SCHEMA`),
    `ROLE_FILTER` = VALUES(`ROLE_FILTER`),
    `ICON` = VALUES(`ICON`),
    `SORT_ORDER` = VALUES(`SORT_ORDER`),
    `UPDATED_TIME` = NOW(3);

INSERT INTO `T_AI_WELCOME_GUIDE` (
    `ID`, `PARENT_ID`, `TYPE`, `LABEL`, `DESCRIPTION`, `PROMPT_CONTENT`,
    `INTERACTION_TYPE`, `FORM_SCHEMA`, `ROLE_FILTER`, `ICON`, `SORT_ORDER`
) VALUES
    ('knowledge-product-qna', 'knowledge-qa', 'ACTION', '产品答疑', NULL,
     '如何设置流水线串行运行',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('knowledge-api-query', 'knowledge-qa', 'ACTION', 'API 查询', NULL,
     '我准备进行 xxx 操作，可以使用哪些 openapi',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 2)
ON DUPLICATE KEY UPDATE
    `PARENT_ID` = VALUES(`PARENT_ID`),
    `TYPE` = VALUES(`TYPE`),
    `LABEL` = VALUES(`LABEL`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `PROMPT_CONTENT` = VALUES(`PROMPT_CONTENT`),
    `INTERACTION_TYPE` = VALUES(`INTERACTION_TYPE`),
    `FORM_SCHEMA` = VALUES(`FORM_SCHEMA`),
    `ROLE_FILTER` = VALUES(`ROLE_FILTER`),
    `ICON` = VALUES(`ICON`),
    `SORT_ORDER` = VALUES(`SORT_ORDER`),
    `UPDATED_TIME` = NOW(3);
-- ------------------------------------------
-- 4. 热点问题 (T_AI_HOT_QUESTION)
-- ------------------------------------------

INSERT IGNORE INTO `T_AI_HOT_QUESTION`
    (`ID`, `QUESTION`, `SOURCE`, `WEIGHT`, `SORT_ORDER`)
VALUES
    ('hq-001', '帮我分析一下流水线构建失败的原因', 'MANUAL', 100, 1),
    ('hq-002', '如何给用户开通下载制品的权限', 'MANUAL', 95, 2),
    ('hq-003', '如何配置流水线定时触发', 'MANUAL', 90, 3),
    ('hq-004', '如何基于模版创建一条新的流水线', 'MANUAL', 85, 4),
    ('hq-005', '帮我查看我在当前项目的权限情况', 'MANUAL', 80, 5),
    ('hq-006', '如何下载流水线最新一次构建的制品', 'MANUAL', 75, 6),
    ('hq-007', '如何通过 OpenAPI 触发流水线构建', 'MANUAL', 70, 7),
    ('hq-008', '如何申请某条流水线的执行权限', 'MANUAL', 65, 8),
    ('hq-009', '如何用自然语言描述需求来生成流水线', 'MANUAL', 60, 9),
    ('hq-010', '帮我查一下最近失败的流水线构建记录', 'MANUAL', 55, 10);
