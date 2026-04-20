USE devops_ci_ai;

SET NAMES utf8mb4;

-- ==========================================
-- AI 服务初始化数据
-- ==========================================
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
