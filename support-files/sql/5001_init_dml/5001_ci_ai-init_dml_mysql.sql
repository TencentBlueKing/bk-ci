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
     '从零或用模版生成流水线编排，并支持分析编排设计与性能优化建议。', NULL,
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
     '请帮我查询名称包含 [流水线关键字] 的流水线，并列出它们最近一次的执行状态和耗时。',
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('pipeline-ops-run', 'pipeline-ops', 'ACTION', '执行流水线', NULL,
     CONCAT(
         '请帮我执行流水线 [流水线名称/ID]，使用代码分支 [分支名称，如 master/main]，',
         '并设置启动参数 [参数名]=[参数值]。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 2),
    ('pipeline-ops-artifact', 'pipeline-ops', 'ACTION', '下载制品', NULL,
     CONCAT(
         '请帮我获取流水线 [流水线名称] 最新一次（或第 [#构建号] 次）成功构建的制品下载链接，',
         '制品名称关键字是 [制品文件关键字，如 .apk / report.zip]。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 3),
    ('pipeline-ops-analyze', 'pipeline-ops', 'ACTION', '分析构建错误', NULL,
     CONCAT(
         '我执行的流水线 [流水线名称/ID] 最新一次（或第 [#构建号] 次）构建失败了。',
         '请帮我分析完整的构建日志，指出导致错误的具体原因，并给出修复建议。'
     ),
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
    ('pipeline-gen-from-scratch', 'pipeline-gen', 'ACTION', '从零生成', NULL,
     CONCAT(
         '我需要为我的 [语言/框架，如 Java/Spring Boot] 项目创建一条全新的流水线。',
         '需包含环节：[代码拉取、Maven 编译、单元测试、Docker 镜像打包并推送]。',
         '请帮我生成完整的蓝盾流水线编排代码，并加上中文注释。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('pipeline-gen-from-template', 'pipeline-gen', 'ACTION', '从模版生成', NULL,
     CONCAT(
         '请基于模版 [模版名称，如 Go 语言通用构建模版]，帮我实例化一条名称为[流水线名称]的约束模式流水线。',
         '我的代码库地址是 [代码库地址]，并且需要在模版的基础上额外增加一个失败通知，',
         '通知方式为企业微信群消息，群ID为[123456]。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 2),
    ('pipeline-gen-analyze', 'pipeline-gen', 'ACTION', '分析流水线', NULL,
     CONCAT(
         '请帮我分析流水线 [流水线名称] 的编排设计。',
         '它最近的执行耗时较长，请指出它的性能瓶颈，并提供开启并发、缓存优化或插件替换的建议。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 3)
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
     CONCAT(
         '用户 [用户名/ID] 反馈他无法对流水线 [流水线名称] 进行 [操作类型，如 编辑/执行] 操作。',
         '请帮我分析该用户的当前权限，说明他缺少了什么角色的哪些具体权限点，并给出权限开通建议。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 1),
    ('auth-mgmt-grant', 'auth-mgmt', 'ACTION', '开通权限', NULL,
     CONCAT(
         '请帮我为用户 [用户名/ID1, 用户名/ID2] 开通项目下的 ',
         '[关键操作，如下载名称为[流水线名称]的流水线的制品] 操作权限，有效期为 [如 30] 天。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 2),
    ('auth-mgmt-renew-admin', 'auth-mgmt', 'ACTION', '续期权限', NULL,
     CONCAT(
         '用户 [用户名/ID] 的 [角色/权限名称] 权限即将过期，',
         '请帮我将该权限续期 [时长，如 3个月/半年]。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 3),
    ('auth-mgmt-revoke', 'auth-mgmt', 'ACTION', '回收权限', NULL,
     CONCAT(
         '请帮我立即回收用户 [用户名/ID] 在项目下的 ',
         '[具体权限/角色，如 流水线删除权限 / 所有权限]。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 4),
    ('auth-mgmt-handover-admin', 'auth-mgmt', 'ACTION', '移交权限', NULL,
     CONCAT(
         '因为工作交接，请帮我将用户 [原用户名/ID] 在项目下负责的所有流水线资源及相关管理员权限，',
         '完整移交给用户 [新用户名/ID]。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 5),
    ('auth-mgmt-remove-users', 'auth-mgmt', 'ACTION', '移除用户', NULL,
     CONCAT(
         '用户 [用户名/ID] 已不再参与本项目，',
         '请帮我将他从项目的所有用户组中彻底移除，清除相关访问权限。'
     ),
     'PROMPT_COMPLETION', NULL, 'ADMIN', NULL, 6),
    ('auth-mgmt-my-perms', 'auth-mgmt', 'ACTION', '我的权限', NULL,
     '请帮我查询我在项目下目前拥有的所有角色，以及这些角色对应的核心操作权限。',
     'DIRECT_TRIGGER', NULL, 'MEMBER', NULL, 7),
    ('auth-mgmt-apply', 'auth-mgmt', 'ACTION', '申请权限', NULL,
     CONCAT(
         '我需要操作项目下的流水线 [流水线名称]，进行 [具体操作，如 编排编辑/执行] 操作。',
         '请帮我生成并提交一份权限申请给管理员，申请理由是：[为了排查线上问题/日常开发需要]。'
     ),
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 8),
    ('auth-mgmt-renew-member', 'auth-mgmt', 'ACTION', '续期权限', NULL,
     CONCAT(
         '我在项目中的 [角色/资源名称] 权限即将过期，请帮我发起续期申请流程，',
         '续期时长为 [如 6个月]，理由是：[后续工作仍需持续跟进该项目]。'
     ),
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 9),
    ('auth-mgmt-handover-member', 'auth-mgmt', 'ACTION', '移交权限', NULL,
     CONCAT(
         '我需要将我名下的流水线 [流水线名称/ID] 的所有者和管理权限，',
         '主动移交给同事 [对方用户名/ID]，请帮我发起权限移交流程。'
     ),
     'PROMPT_COMPLETION', NULL, 'MEMBER', NULL, 10),
    ('auth-mgmt-exit-project', 'auth-mgmt', 'ACTION', '退出项目', NULL,
     CONCAT(
         '我已完成在项目 [项目名称/ID] 中的阶段性支持工作，',
         '请帮我执行退出项目操作，解除我在该项目下的所有角色和权限。'
     ),
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
     CONCAT(
         '请问在蓝盾中，如何实现 [具体需求，例如：多分支的自动化合并与触发构建 / 代码质量红线拦截]？',
         '请给我详细的配置指引或最佳实践。'
     ),
     'PROMPT_COMPLETION', NULL, NULL, NULL, 1),
    ('knowledge-api-query', 'knowledge-qa', 'ACTION', 'API 查询', NULL,
     CONCAT(
         '我需要通过蓝盾 OpenAPI 实现 [具体业务场景，例如：第三方系统触发流水线执行并传递参数]。',
         '请提供对应的 API 接口地址、请求方法、Headers 要求、Body 参数示例以及返回的数据结构。'
     ),
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
