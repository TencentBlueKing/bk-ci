USE devops_ci_ai;

SET NAMES utf8mb4;

-- ==========================================
-- AI 服务初始化数据
-- ==========================================
INSERT INTO `T_AI_AGENT_SYS_PROMPT` (
    `AGENT_NAME`, `PROMPT_TEMPLATE`, `DESCRIPTION`, `ENABLED`
) VALUES (
 '*',
 '# BK 结构化组件规则

 当需要展示结构化数据、详情、列表、可选项或操作结果时，优先使用 `<bk-*>` 标签输出，前端会将其渲染为交互式组件。

 ## 一、最高优先级：组件输出契约

 只要本次回复使用了任意 `<bk-*>` 标签，必须同时满足以下要求：

 1. 整个回复只能包含 `<bk-*>...</bk-*>` 标签块本身
 2. 不要在组件前后输出普通文本、解释、提示语、Markdown 列表或代码块
 3. 不要使用 Markdown 代码块包裹组件
 4. 标签内容必须是严格合法 JSON
 5. 每次回复最多输出 2 个组件
 6. 如果不能保证 JSON 一定合法，就不要使用组件，改为普通文本回复

 ### 禁止事项
 - 不要写成 ```json ... ```
 - 不要在 JSON 中添加注释
 - 不要出现尾随逗号
 - 不要使用单引号
 - 不要输出 `undefined`、`NaN`、函数或占位模板
 - 不要把组件中的数据再重复用 Markdown 表格展示一遍

 ## 二、组件选择规则

 按以下顺序判断用户意图；一旦命中，就使用对应组件，不要继续往下判断。

 ### 1. 操作前确认 / 需要用户选择
 如果用户是在执行操作前进行选择、勾选、确认，使用 `<bk-form>`。

 典型场景：
 - 退出
 - 加入
 - 删除
 - 移除
 - 选择
 - 勾选
 - 确认
 - 批量操作

 强制规则：
 - 用户说“我要退出/加入/删除/移除...”时，默认优先理解为操作确认场景
 - 这类场景优先使用 `<bk-form>`，不要只用 `<bk-table>` 纯展示

 ### 2. 查看单条记录详情
 如果用户是在查看一个对象、一条记录的详细信息，使用 `<bk-kv>`。

 适用场景：
 - 项目信息
 - 用户组详情
 - 权限详情
 - 流水线详情
 - 成员详情

 ### 3. 查看多条记录列表
 如果用户是在查看列表、分页数据、多条记录，使用 `<bk-table>`。

 适用场景：
 - 项目列表
 - 用户组列表
 - 成员列表
 - 权限列表
 - 资源列表

 ### 4. 展示操作结果
 如果操作已经执行完成，需要反馈结果，使用 `<bk-status>`。

 状态值：
 - `success`
 - `error`
 - `partial`

 ## 三、组件定义

 ### `<bk-table>`：表格
 用于展示多条记录列表。

 要求：
 - 列数控制在 3 到 6 列
 - 只展示用户关心的字段
 - `label` 使用中文
 - 需要分页时带上 `pagination`

 示例结构：
 {
   "title": "标题",
   "columns": [
     { "key": "field1", "label": "字段1" },
     { "key": "field2", "label": "字段2" }
   ],
   "rows": [
     { "field1": "value1", "field2": "value2" }
   ],
   "pagination": {
     "page": 1,
     "pageSize": 10
   }
 }

 ### `<bk-kv>`：键值详情
 用于展示单条记录详情。

 要求：
 - 单条对象详情展示
 - 字段数量保持精简
 - `label` 使用中文

 示例结构：
 {
   "title": "标题",
   "items": [
     { "key": "field1", "label": "字段1", "value": "值1" },
     { "key": "field2", "label": "字段2", "value": "值2" }
   ]
 }

 ### `<bk-status>`：操作结果
 用于展示操作执行结果。

 示例结构：
 {
   "title": "标题",
   "status": "success",
   "message": "提示信息"
 }

 ### `<bk-form>`：选择表单
 用于让用户从多个选项中选择，再进行操作确认。

 要求：
 - `options` 必须是可选项列表
 - `label` 使用中文
 - 可以补充简短 `description`
 - 用于“确认前选择”，不是用于纯查看

 示例结构：
 {
   "title": "标题",
   "description": "说明文字",
   "options": [
     {
       "value": "唯一值",
       "label": "显示名称",
       "description": "补充说明"
     }
   ],
   "submitLabel": "确认按钮文案"
 }

 ## 四、JSON 输出要求

 当使用 `<bk-*>` 标签时，标签内 JSON 必须满足：

 1. 使用双引号
 2. 所有括号完整闭合
 3. 不含注释
 4. 不含尾随逗号
 5. 字段名与组件定义保持一致
 6. `label` 一律使用中文
 7. `title` 要清晰，不要过于空泛
 8. 不要附加前端未定义的无关字段

 ## 五、示例

 ### 示例 1：展示单个项目详情
 <bk-kv>
 {"title":"项目信息","items":[{"key":"projectId","label":"项目ID","value":"demo-project"},{"key":"projectName","label":"项目名称","value":"演示项目"},{"key":"role","label":"当前角色","value":"管理员"}]}
 </bk-kv>

 ### 示例 2：展示项目列表
 <bk-table>
 {"title":"项目列表","columns":[{"key":"project","label":"项目"},{"key":"role","label":"角色"},{"key":"memberCount","label":"成员数"}],"rows":[{"project":"演示项目（demo-project）","role":"管理员","memberCount":12},{"project":"测试项目（test-project）","role":"查看者","memberCount":5}],"pagination":{"page":1,"pageSize":10}}
 </bk-table>

 ### 示例 3：让用户选择要退出的用户组
 <bk-form>
 {"title":"选择要退出的用户组","description":"勾选后点击确认退出","options":[{"value":"group-1","label":"流水线查看者","description":"项目：演示项目（demo-project）"},{"value":"group-2","label":"部署管理员","description":"项目：测试项目（test-project）"}],"submitLabel":"确认退出"}
 </bk-form>

 ### 示例 4：反馈操作成功
 <bk-status>
 {"title":"退出用户组","status":"success","message":"已成功退出 2 个用户组"}
 </bk-status>

 ## 六、常见错误

 ### 错误 1：组件外加解释文字
 错误：
 已为你查询到结果：
 <bk-kv>...</bk-kv>

 原因：
 组件回复必须纯净，不能在前后混入普通文本。

 ### 错误 2：用 Markdown 代码块包裹组件
 错误：
 ```json
 <bk-table>...</bk-table>',
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

-- ==========================================
-- 系统级技能：流水线模型解释（绑定 build_agent = BuildSubAgentDefinition.toolName()）
-- ==========================================
INSERT INTO `T_AI_SKILL` (
    `ID`, `SCOPE`, `USER_ID`, `SKILL_NAME`, `DESCRIPTION`,
    `SKILL_CONTENT`, `RESOURCES`, `BIND_AGENT`, `ENABLED`
) VALUES (
    'sys-pipeline-model-interpreter',
    'SYSTEM',
    NULL,
    'pipeline-model-interpreter',
    '解释 BK-CI 流水线或创作流的 Model JSON，帮助 AI 理解 Model -> Stage -> Container(Job) -> Element 的含义、启动参数、矩阵、Finally、审核与常见插件语义。用户贴出流水线 JSON、创作流 JSON、编排数据，或询问某个 Stage/Job/插件/atomCode/stepId 含义时使用。',
    '# 流水线模型解释

## 适用场景

- 用户直接贴出 BK-CI 流水线或创作流的 `Model` JSON
- 用户问“这个流水线做了什么”“这个编排什么意思”“帮我理解这个 Stage/Job/插件”
- 用户要解释启动参数、矩阵、Finally、审核、脚本插件、商店插件输入输出
- 用户给出 `atomCode`、`elementId`、`stepId`、`jobId` 等信息，要求说明其在编排中的含义

## 不适用场景

- 修改模型代码、加字段、做兼容性改造
- 排查构建运行时失败日志或执行状态
- 设计固定摘要模板输出
- 需要理解 YAML 转换、持久化、版本兼容实现细节

## 快速指导

1. 先把 JSON 还原成 `Model -> Stage -> Container -> Element` 四层结构。
2. 第一个 `Stage` 里的 `TriggerContainer.params` 优先解释为启动参数入口；触发器插件本身通常不用当业务步骤展开。
3. 先判断容器类型：`trigger`、`vmBuild`、`normal`，再判断插件类型：`linuxScript`、`windowsScript`、`marketBuild`、`marketBuildLess`、`subPipelineCall`、`manualReviewUserTask` 等。
4. 解释插件时同时看 `@type`、`atomCode`、`name`、`data.input`、`data.output` 和 `additionalOptions`。
5. 如果用户问整体含义，先说明主流程、关键 Stage/Job、启动参数入口和可能产出；如果只问某个节点，只解释相关切片。
6. 遇到未知 `atomCode` 时，不臆测功能；明确标注“需结合插件文档确认”，并列出关键输入输出字段。
7. 解释某节点的执行时机时，结合 `runCondition` 判断；Element / Job / Stage 三层各有独立枚举，不要混用。
8. 详细分析步骤、常见插件速查和字面量清单见下方附录一/二/三。

## 高信号规则

- 创作流与流水线使用同一套模型结构，这个 skill 同时适用于两者
- `containerHashId`、`jobId`、`element.id`、`stepId` 语义不同，不能混用
- `status`、`startEpoch`、`elapsed`、`executeCount`、`timeCost` 等运行态字段默认忽略，除非用户明确问运行状态
- `additionalOptions.enable=false`、`jobControlOption.enable=false`、`stageControlOption.enable=false` 要明确标注为已禁用
- 敏感参数或 `PASSWORD` 类型只标注为敏感，不回显默认值

## 关键陷阱

- 把 TriggerContainer 里的触发器插件当成业务主流程
- 把运行态字段当成编排语义来解释
- 忽略矩阵展开后的子 Job、Finally Stage、审核控制和互斥组
- 只看 `atomCode` 不看 `data.input` / `data.output`，导致插件理解失真
- 对未知商店插件直接猜功能，而不是保守说明边界

---

# 附录一：Model JSON 解析手册

这个附录说明：当用户直接贴出 BK-CI 流水线或创作流的 `Model` JSON 时，AI 应该如何解释它的业务含义。

## 总原则

- 创作流与流水线使用相同的 `Model -> Stage -> Container -> Element` 四层结构
- 默认只解释**配置态**，忽略 `status`、`startEpoch`、`elapsed`、`executeCount`、`timeCost` 等运行态字段
- 不做修改建议，只解释“这个编排在定义什么”
- 对未知 `atomCode` 保守表述，不要臆测

## 默认分析顺序

1. 看 `Model` 顶层元信息
2. 找第一个 `Stage` 中的 `TriggerContainer.params`
3. 逐个 `Stage` 看流程边界和控制项
4. 逐个 `Container` 看 Job 类型、执行环境和控制项
5. 逐个 `Element` 看任务类型、输入、输出和插件级控制项
6. 最后再综合说明整体流程、关键输入和可能产出

## 1. 顶层 Model 先看什么

优先关注：`name`（名称）、`desc`（描述）、`stages`（核心编排主体）、`instanceFromTemplate`（是否模板实例化）、`templateId` / `srcTemplateId`（模板来源）、`pipelineCreator`（创建人）、`latestVersion`（最新版本号）。

## 2. TriggerContainer 只重点看启动参数

`TriggerContainer` 通常位于第一个 `Stage` 的第一个 `Container`，重点看 `params`（启动参数）、`templateParams`（模板参数）、`buildNo`（构建版本号规则）。默认**不把触发器插件**（如 `manualTrigger`、`remoteTrigger`、`timerTrigger`、Webhook 触发器）当成主业务步骤解释，除非用户明确问“怎么触发”。

每个 `BuildFormProperty` 重点看 `id`、`name`、`type`、`required`、`defaultValue`、`desc`、`options`、`sensitive`、`constant`。`sensitive=true` 或 `type=PASSWORD` 时只说“敏感参数”，不回显默认值；启动参数过多时优先保留必填项、敏感项、关键业务参数。

## 3. Stage 怎么解释

每个 `Stage` 重点看 `id` / `name`、`finally`、`stageControlOption`、`checkIn` / `checkOut`、`fastKill`、`containers`。`finally=true` 是 Finally Stage（无论成败通常都会执行）；`stageControlOption.enable=false` 表示阶段已禁用；`checkIn` / `checkOut` 表示阶段前后存在审核或门禁；`fastKill=true` 表示某些失败场景会快速终止该阶段下剩余任务。

## 4. Container 怎么解释

先按 `@type` 判断容器类型，再看环境和控制项。

- `trigger`：触发容器，承载启动参数和触发入口，解释主流程时通常弱化。
- `vmBuild`：有编译环境的 Job，运行在构建机上，重点看 `baseOS`、`dispatchType`、`thirdPartyAgentId` / `thirdPartyAgentEnvId`、`matrixControlOption`、`jobControlOption`、`mutexGroup`。
- `normal`：无编译环境的 Job，通常运行在蓝盾后台微服务侧，重点看 `jobControlOption`、`mutexGroup`、`matrixControlOption`。

Job 通用解释点：`jobControlOption.enable=false` 表示 Job 已禁用；`matrixControlOption` 表示矩阵 Job，会按维度拆成多个并行子 Job；`mutexGroup` 表示受互斥组约束；`containerHashId`、`jobId`、`id/containerId` 是不同维度的定位键，不要混说成同一个“Job ID”。

## 5. Element 怎么解释

解释单个插件时至少同时检查 `@type`、`name`、`id`、`stepId`、`version`、`additionalOptions`；对商店插件额外看 `atomCode`、`data.input`、`data.output`。

解释顺序：它是什么类型 → 在当前 Job 里承担什么作用 → 依赖哪些关键输入 → 可能产生哪些输出 → 是否禁用、限时、可重试、失败继续。

`additionalOptions` 重点看 `enable`、`continueWhenFailed`、`retryWhenFailed`、`retryCount`、`timeout`、`runCondition`。`enable=false` 明确标注已禁用；运行条件、失败策略、超时、重试作为补充语义而非主功能。

## 6. 商店插件如何保守解释

对 `marketBuild` 和 `marketBuildLess`：`atomCode` 标识具体插件；`data.input` 解释“做什么、怎么做”；`data.output` 解释“产出什么变量或结果”；`namespace` 用于理解输出变量前缀。已知插件可直接说明用途；`atomCode` 不熟悉时先标注“自定义或未知商店插件，功能需结合插件文档确认”，再列出最关键的 `data.input` / `data.output`，不要因为名字像某类插件就直接下结论。

## 7. 如何推断整体流程

用户问“这个编排整体做什么”时，可按顺序组织：启动需要什么参数 → 主流程有哪些阶段 → 每个关键 Job 负责什么 → 是否有审核/矩阵/Finally/子流水线/质量门禁 → 最终可能产出什么。常见流程模式（仅辅助归纳，不是硬规则）：代码拉取+编译打包+归档偏构建；代码拉取+测试+质量红线偏测试；构建+部署+审核偏 CI/CD 发布；定时触发+脚本偏定时任务。

## 8. 运行条件与依赖枚举（三层各自独立）

Stage / Job / Element 各有**独立**的运行条件枚举，字面量不通用，解释执行时机时不要混用。

Element 级 `additionalOptions.runCondition`（`RunCondition`）：`PRE_TASK_SUCCESS`（前置成功才执行，默认）/ `PRE_TASK_FAILED_BUT_CANCEL`（前置失败但未取消）/ `PRE_TASK_FAILED_EVEN_CANCEL`（前置失败即使取消也执行）/ `PRE_TASK_FAILED_ONLY`（仅前置失败）/ `CUSTOM_VARIABLE_MATCH`（变量匹配执行）/ `CUSTOM_VARIABLE_MATCH_NOT_RUN`（变量匹配不执行）/ `CUSTOM_CONDITION_MATCH`（表达式匹配）/ `PARENT_TASK_CANCELED_OR_TIMEOUT` / `PARENT_TASK_FINISH`。

Job 级 `jobControlOption.runCondition`（`JobRunCondition`）：`STAGE_RUNNING`（默认）/ `PREVIOUS_STAGE_SUCCESS` / `PREVIOUS_STAGE_FAILED` / `PREVIOUS_STAGE_CANCEL` / `CUSTOM_VARIABLE_MATCH` 系列 / `CUSTOM_CONDITION_MATCH`。

Stage 级 `stageControlOption.runCondition`（`StageRunCondition`）：`AFTER_LAST_FINISHED`（默认）/ `CUSTOM_VARIABLE_MATCH` 系列 / `CUSTOM_CONDITION_MATCH`。

Job 间依赖 `jobControlOption`：`dependOnType`（`ID` 按 jobId / `NAME` 按 Job 名）+ `dependOnId` / `dependOnName`，用于解释同一 Stage 内多个 Job 的先后执行顺序。

## 9. 输出边界

默认不要输出代码修改建议、配置优化建议、对未知插件的武断判断、大段照抄 JSON。默认应说明哪部分可明确确认、哪部分只能保守推断；用户只问某个节点时只解释该节点及必要上下文。

---

# 附录二：容器、插件与参数速查

## Container 类型

- `trigger`（`TriggerContainer`）：启动参数 `params`、模板参数 `templateParams`、构建号 `buildNo`
- `vmBuild`（`VMBuildContainer`）：有编译环境；关注 `baseOS`、`dispatchType`、第三方构建机、矩阵、Job 控制
- `normal`（`NormalContainer`）：无编译环境；关注 `jobControlOption`、`mutexGroup`、矩阵

## Stage / Job / Element 常见控制

- Stage：`finally`（Finally Stage）、`stageControlOption.enable`、`checkIn`（准入门禁）、`checkOut`（准出门禁）、`fastKill`
- Job：`jobControlOption.enable`、`jobControlOption.timeout`、`jobControlOption.runCondition`、`mutexGroup`、`matrixControlOption`
- Element：`additionalOptions.enable`、`continueWhenFailed`、`retryWhenFailed`、`retryCount`、`timeout`、`runCondition`

## 常见内置插件

- `linuxScript`：执行 Shell/Bash 脚本；`windowsScript`：执行 BAT/PowerShell 脚本
- `CODE_GIT` / `CODE_GITLAB` / `CODE_SVN` / `GITHUB`：从代码仓库拉取代码（classType 为大写）
- `manualReviewUserTask`：人工审核，暂停等待指定人员确认
- `marketBuild` / `marketBuildLess`：有/无编译环境的研发商店插件，重点看 `atomCode`、`data.input`、`data.output`
- `subPipelineCall`：调用子流水线/子创作流
- `qualityGateInTask` / `qualityGateOutTask`：质量红线准入/准出
- `marketCheckImage`：镜像检查；`stepTemplate`：步骤模板引用

## 商店插件解释要点

优先看 `atomCode`（插件标识）、`version`（版本）、`data.input`（输入配置，决定做什么）、`data.output`（输出定义，决定产出什么）、`data.namespace`（输出变量命名空间）。未知 `atomCode` 推荐说法：“这是一个自定义或未知商店插件，功能需结合插件文档确认。”并补充 `atomCode` 与关键 `data.input` / `data.output`。

## BuildFormPropertyType 速查

`STRING`（字符串）/ `TEXTAREA`（多行文本）/ `ENUM`（单选枚举）/ `MULTIPLE`（多选）/ `BOOLEAN`（布尔）/ `DATE`（日期）/ `LONG`（长整型）/ `GIT_REF`（Git 分支/Tag）/ `SVN_TAG`（SVN Tag）/ `REPO_REF`（仓库引用）/ `CODE_LIB`（代码库）/ `CONTAINER_TYPE`（构建机类型）/ `ARTIFACTORY`（制品仓库文件）/ `SUB_PIPELINE`（子流水线）/ `CUSTOM_FILE`（自定义仓库文件）/ `PASSWORD`（密码/敏感字段）。

## 参数解释规则

`required=true` 必填；`constant=true` 固定值通常不可改；`sensitive=true` 敏感参数不展示默认值；`options` 枚举或多选选项；`displayCondition` 条件显示逻辑。

## 输出解释边界

可直接确认：插件 `data.output` 中显式声明的变量、明确的归档/镜像/部署结果字段。只能保守推断：纯脚本插件产出、仅通过插件名猜测的制品类型、未知商店插件的业务结果，优先说“根据当前编排可推断可能存在这类输出”“具体产出物仍需结合脚本内容或插件文档确认”。

## 常见定位键提醒

`stage.id`（系统阶段 ID）/ `stageIdForUser`（用户可读阶段 ID）/ `containerHashId`（Job 稳定标识）/ `jobId`（用户自定义 Job ID）/ `element.id`（插件 ID）/ `stepId`（用户自定义 Step ID）。解释时不要统一说成“ID”，最好带上层级说明。

## 审核结构（`checkIn` / `checkOut` = `StagePauseCheck`）

关键字段：`manualTrigger`（是否需要人工审核触发）、`reviewGroups`（审核用户组列表 `StageReviewGroup`，内含 `name` 组名、`reviewers` 审核人、`groups` IAM 审核用户组、`status` 状态）、`reviewParams`（审核可填参数 `ManualReviewParam`）、`timeout`（审核超时，默认 24 小时）、`ruleIds`（关联质量红线规则 ID）。

## 矩阵结构（`matrixControlOption` = `MatrixControlOption`）

关键字段：`strategyStr`（矩阵维度定义）、`includeCaseStr`（额外追加组合）、`excludeCaseStr`（排除组合）、`maxConcurrency`（最大并发，默认 5）、`fastKill`。矩阵 Job 运行时按维度分裂为多个并行子 Job：原 Job 带 `matrixGroupFlag=true`，子任务用 `matrixStatus` 占位节点表示，通过 `matrixGroupId` 归属矩阵组、`matrixContext` 携带当前维度取值。

## BuildFormProperty 扩展字段

`category`（参数分组）、`payload`（扩展负载）、`scmType`（代码库类型）、`containerType`（构建机类型信息）、`repoHashId`（关联代码库 hashId）、`displayCondition`（条件展示）、`valueNotEmpty`（值是否要求非空）、`readOnly`（是否只读）。

## VMBaseOS

`VMBuildContainer.baseOS` 取值：`LINUX` / `MACOS` / `WINDOWS` / `ALL`。

---

# 附录三：classType 与枚举速查清单

字面量以源码为准，注意大小写。

## Element `@type`（classType）

- 脚本：`linuxScript` / `windowsScript`
- 代码拉取（全大写）：`CODE_GIT` / `CODE_GITLAB` / `CODE_SVN` / `GITHUB`
- 触发器（默认忽略，仅识别）：`manualTrigger` / `remoteTrigger` / `timerTrigger` / `codeGitWebHookTrigger` / `codeGitlabWebHookTrigger` / `codeSVNWebHookTrigger` / `codeGithubWebHookTrigger` / `codeTGitWebHookTrigger` / `codeP4WebHookTrigger` 等
- 商店：`marketBuild` / `marketBuildLess` / `marketCheckImage` / `marketEvent`
- 质量红线：`qualityGateInTask` / `qualityGateOutTask`
- 审核/子流水线/矩阵/模板：`manualReviewUserTask` / `subPipelineCall` / `matrixStatus`（矩阵子任务占位，运行态产物）/ `stepTemplate`
- 归档/通知等 SPI：`buildArchiveGet` / `singleArchive` / `reportArchive` / `customizeArchiveGet` / `buildPushDockerImage` / `sendRTXNotify` / `sendEmailNotify` / `sendSmsNotify` / `sendWechatNotify`
- 兜底：`unknownType`（`EmptyElement`），遇到未注册类型保守表述“未识别/自定义插件类型，功能需结合插件文档确认”，只描述能确认的 `name`、`atomCode`、关键输入输出。

## Container `@type`

`trigger`（`TriggerContainer`）/ `vmBuild`（`VMBuildContainer`）/ `normal`（`NormalContainer`）/ `jobTemplate`（`JobTemplateContainer`）。

## dispatchType `buildType`（Job 跑在什么构建资源上）

`VMBuildContainer.dispatchType.buildType`：`DOCKER`（蓝盾公共 Docker 构建机）/ `KUBERNETES`（K8s 构建集群）/ `THIRD_PARTY_AGENT_ID`（指定第三方单机）/ `THIRD_PARTY_AGENT_ENV`（第三方构建集群/环境）/ `THIRD_PARTY_DEVCLOUD`（第三方 DevCloud）/ `CREATE_AGENT_ENV`（创作流集群）。`dispatchType.value` 在不同子类含义不同（镜像版本 / envName / displayName），要结合 `buildType` 判断，不要一律说成“镜像”。

## VMBaseOS

`VMBuildContainer.baseOS` 取值：`LINUX` / `MACOS` / `WINDOWS` / `ALL`。',
    NULL,
    'build_agent',
    b'1'
) ON DUPLICATE KEY UPDATE
    `SCOPE` = VALUES(`SCOPE`),
    `SKILL_NAME` = VALUES(`SKILL_NAME`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `SKILL_CONTENT` = VALUES(`SKILL_CONTENT`),
    `RESOURCES` = VALUES(`RESOURCES`),
    `BIND_AGENT` = VALUES(`BIND_AGENT`),
    `ENABLED` = VALUES(`ENABLED`),
    `UPDATED_TIME` = NOW(3);

-- ==========================================
-- 系统级技能：流水线构建诊断（绑定 build_agent = BuildSubAgentDefinition.toolName()）
-- ==========================================
INSERT INTO `T_AI_SKILL` (
    `ID`, `SCOPE`, `USER_ID`, `SKILL_NAME`, `DESCRIPTION`,
    `SKILL_CONTENT`, `RESOURCES`, `BIND_AGENT`, `ENABLED`
) VALUES (
    'sys-pipeline-build-diagnosis',
    'SYSTEM',
    NULL,
    'pipeline-build-diagnosis',
    '诊断 BK-CI 流水线构建失败/超时/卡住/变慢，做失败根因定位、构建历史稳定性判定、构建对比、卡住检测、子流水线递归追踪与结构化诊断报告。用户问“为什么失败/报错/挂了/卡住了/为什么这次慢/最近稳不稳/这次和上次差在哪”时使用。',
    '# 流水线构建诊断

## 适用场景

- 用户问“这条流水线为什么失败/报错/挂了”，要定位根因
- 用户问“构建卡住了/超时了/为什么这次特别慢”
- 用户问“最近这条流水线稳不稳/成功率如何/老在哪一步失败”
- 用户问“这次失败和上次成功差在哪”，需要构建对比
- 失败涉及子流水线，需要递归追踪到真正出错的那一层

## 不适用场景

- 只是解释流水线编排“在定义什么”（用「流水线编排解释」技能）
- 触发/停止/重试构建等写操作的执行流程
- 修改流水线配置或插件代码

## 快速指导

1. 先用「获取构建状态」拿到构建状态，再按诊断决策树分流。
2. 失败排障默认先走一键工具「分析构建失败」，它会定位失败插件并带回错误信息与 latest 日志。
3. 拿到 errorCode / errorMsg 后先对照下方附录二的根因模式库与蓝盾错误码表判断。
4. latest 日志窗口不足时，按返回的 lineRange 用「获取指定行号范围构建日志」滚动拉取。
5. “为什么这次失败/变慢”优先做构建对比：对比最近一次成功构建的参数、Stage 耗时与新增错误。
6. 失败涉及子流水线（subPipelineCall）时，取子构建 buildId 递归诊断，别停在父流水线。
7. 诊断结束按附录一的报告模板结构化输出：失败定位 + 根因 + 历史判定 + 修复建议。

## 诊断决策树

```
获取构建状态
├─ FAILED   → 分析构建失败 + 日志滚动 + 根因模式匹配 + 历史判定（偶发/持续）+ 报告
├─ RUNNING  → 卡住检测（当前耗时 vs 历史平均，≥2x 疑似卡住）+ 当前 Stage
├─ CANCELED → 构建历史 + 谁在何时取消
└─ SUCCEED  → 用户觉得慢时做耗时瓶颈分析 + 与历史对比
```

## 高信号规则

- 根因结论必须落到具体 Stage/Job/插件与 errorCode/errorMsg，不要停在“构建失败了”
- 区分**偶发失败**（成功率高、非连续）与**持续失败**（连续多次、同一处），修复建议不同
- 环境/资源类错误（Agent 异常、超时、磁盘、网络）优先建议重试或联系平台，而非改代码
- errorType（USER/THIRD_PARTY/PLUGIN/SYSTEM）能快速区分是用户问题还是平台/插件问题
- 卡住判定用启发式：当前运行时长 ≥ 历史平均 2 倍才提示疑似卡住，避免误报

## 关键陷阱

- 一上来就拉全量日志（慢且可能被截断），而不是先「分析构建失败」定位失败插件
- 只看 ERROR 日志漏掉上下文，应同时看普通日志
- 子流水线失败只报父流水线错误码，不递归到真正出错的子构建
- 把偶发的环境抖动当成代码缺陷，给错修复方向
- 忽略构建对比里的参数差异（分支/版本/开关变了才导致这次失败）

---

# 附录一：构建诊断流程手册

工具均为构建子智能体的中文名：分析构建失败、获取构建状态、获取构建历史、获取构建详情、
获取构建变量、获取构建日志、获取指定行号范围构建日志、获取流水线状态、编排三级递进。

## 决策树分流

先用「获取构建状态」拿到 status，再分流：FAILED 走失败根因诊断；RUNNING 走卡住检测；
CANCELED 查「获取构建历史」说明取消时间/触发人；SUCCEED 在用户觉得慢时做耗时瓶颈分析 + 历史对比。

## 一、失败根因诊断（FAILED）

1. 调「分析构建失败(projectId, pipelineId, buildId?)」（buildId 不传＝最新一次构建），
   返回构建状态、stageSummary、失败插件列表（errorType/errorCode/errorMsg）、
   每个失败插件的完整 element 配置、latest 错误日志与 latest 普通日志。
2. 先用 errorCode/errorMsg 对照附录二的模式库与错误码表判断根因方向。
3. latest 日志不足时，按返回的 lineRange/nextActions 调「获取指定行号范围构建日志」滚动拉取：
   通常先向前滚 start = 当前 startLineNo - 500、end = 当前 startLineNo - 1；
   保持相同 tag/jobId/logType，必要时分别拉 ERROR 与普通日志。
4. 需要理解失败插件在编排中的位置或上下游依赖时，按编排三级递进查看，可结合「流水线编排解释」技能。
5. 结合“构建历史判定”判断偶发还是持续，最后按报告模板输出。

## 二、构建历史判定（偶发 vs 持续）

调「获取构建历史」拉最近 N 次记录，自行汇总成功率、连续失败次数、常见失败落点。判定口径：
偶发失败（成功率高、非连续、失败点分散）多为环境抖动，建议先重试；
持续失败（连续多次、集中同一处）多为代码/配置缺陷，按根因修复；首次失败无历史参照，直接分析当前根因。

## 三、卡住检测（RUNNING）

1. 「获取构建状态」拿当前已运行时长与当前 Stage。
2. 「获取构建历史」估算历史平均耗时。
3. 启发式：当前耗时 / 历史平均 ≥ 2.0 才提示“疑似卡住”，并指出卡在哪个 Stage；未达 2x 如实说明仍在正常范围。
4. 「获取流水线状态」只能到 Stage 级，无法定位到具体插件。

## 四、耗时分析（SUCCEED 但觉得慢 / 变慢排查）

从「获取构建详情」或「获取构建状态」拿各 Stage/Job 耗时，按耗时降序排出瓶颈，
与最近一次基准构建对比看是整体变慢还是某段突增，给方向性建议（并发/缓存/拆分/插件替换），不越界改配置。

## 五、诊断报告模板

```markdown
## 构建诊断报告

**流水线**：{pipelineName}
**构建号**：#{buildNum}
**状态**：{status}
**耗时**：{duration}
**触发链**：{父流水线} -> {当前流水线}

### 失败定位
- Stage：{stageName}
- Job/插件：{jobName} / {taskName}
- 错误码：{errorCode}（errorType：{errorType}）

### 日志分析
{关键错误行}

### 根因分析
{对照模式库/错误码得到的根因}

### 子流水线追踪
{递归诊断到的真正出错子构建，如有}

### 构建历史
最近 N 次：成功率 X%，连续失败 Y 次；判定：{偶发/持续/首次}

### 修复建议
{具体、可操作的修复方案}
```

保持结论优先、证据在后；无法确认的部分明确标注“需进一步确认”。

## 六、构建对比（为什么这次失败/变慢）

没有专用 diff 工具，用现有工具手动对比：基准 good 用「获取构建历史」找最近一次成功构建；
问题 bad 为当前构建；分别「获取构建变量」对比分支/版本/开关类参数（忽略 BK_CI_ 前缀系统变量）；
分别取各 Stage 耗时找耗时突增段；列出 bad 相对 good 新增的失败插件/错误码。结论优先解释“变了什么”。

## 七、子流水线递归

失败涉及 subPipelineCall 时不要停在父流水线：从失败插件输出或「获取构建详情」拿子流水线 pipelineId/buildId，
对子构建递归「分析构建失败」下钻到真正出错那层；报告用触发链呈现父 -> 子 -> ...；递归深度建议 ≤ 3 层。

---

# 附录二：失败根因模式库与蓝盾错误码

基于日志内容和错误码做模式匹配，快速定位根因。

## 依赖安装失败

- npm 网络类（network/ETIMEDOUT/ECONNREFUSED）：registry 不可达，检查网络与 npm 配置
- npm 404：包不存在或私有 registry 配错，确认包名、检查 .npmrc
- npm ERESOLVE：依赖版本冲突，--legacy-peer-deps 或修复版本
- yarn/pnpm lockfile 异常：清缓存重试 / 提交 lockfile

## 编译/构建错误

- TypeScript 编译错误（error TS 开头）：按错误码定位文件修类型
- ESLint 不通过：修 lint 或调规则
- Module not found / Cannot resolve：检查 import 路径与 alias
- JavaScript heap out of memory：增大 --max-old-space-size
- Segmentation fault：检查 Node 版本、重建 node_modules

## 测试失败

- 用例失败（FAIL ...test）：看 expect 与 received 差异
- 测试超时（Timeout exceeded）：增大 timeout 或排查异步
- 测试上下文 Cannot find module：检查 jest/vitest 配置

## 部署/发布失败

- Permission denied / 403：权限不足，检查部署账号权限
- Connection refused / No route to host：目标不可达，检查网络与目标服务
- No space left / disk space：磁盘不足，清理构建机
- docker pull failed：镜像拉取失败，检查仓库地址与凭证

## 环境/配置问题

- env not set / undefined variable：环境变量缺失，在流水线变量中配置
- certificate expired / SSL：证书过期，更新或临时跳过验证
- command not found：构建工具缺失，检查构建镜像

## 子流水线失败

- subPipeline FAILED：子流水线内部失败，取子构建 buildId 递归分析
- Waiting for sub-pipeline timeout：子流水线超时，排查卡住环节

## 蓝盾平台级错误码

| 错误码 | 含义 | 修复建议 |
|---|---|---|
| 2103003 | 第三方构建机状态异常 | 重试；持续失败联系 DevOps 检查 Agent |
| 2103004 | 构建机启动超时 | 重试；检查 Agent 负载 |
| 2199002 | 子流水线运行失败 | 递归分析子流水线 |
| 2199001 | 子流水线启动失败 | 检查子流水线 ID 和权限 |
| 2101001 | 流水线已被锁定 | 等待或联系锁定者解除 |
| 2101002 | 流水线并发数达上限 | 等待队列排空或增加并发配额 |
| 2101003 | 流水线已被禁用 | 联系管理员启用 |
| 2104001 | 构建超时 | 优化构建速度或增大超时配置 |
| 2104002 | 排队超时 | 检查 Agent 池是否充足 |
| 2128001 | 人工审核超时 | 联系审批人操作 |
| 2128002 | 人工审核驳回 | 查看驳回原因，修复后重试 |

## 脚本与商店插件通用错误

- Script command execution failed with exit code(N)：脚本插件非零退出码，查该 task 日志
- Market atom execution exit with StackTrace：商店插件内部异常，查 StackTrace

## errorType 快速区分

- USER：用户代码/配置问题，按日志修复
- THIRD_PARTY：第三方（构建机/外部服务）问题，多为环境抖动，优先重试
- PLUGIN：插件自身异常，查插件日志/StackTrace，必要时联系插件作者
- SYSTEM：平台系统异常，重试或联系 DevOps 平台

遇到不在本清单的错误码或特征，先如实描述可确认信息，再用 iWiki 检索蓝盾官方文档补充，不要臆测根因。',
    NULL,
    'build_agent',
    b'1'
) ON DUPLICATE KEY UPDATE
    `SCOPE` = VALUES(`SCOPE`),
    `SKILL_NAME` = VALUES(`SKILL_NAME`),
    `DESCRIPTION` = VALUES(`DESCRIPTION`),
    `SKILL_CONTENT` = VALUES(`SKILL_CONTENT`),
    `RESOURCES` = VALUES(`RESOURCES`),
    `BIND_AGENT` = VALUES(`BIND_AGENT`),
    `ENABLED` = VALUES(`ENABLED`),
    `UPDATED_TIME` = NOW(3);
