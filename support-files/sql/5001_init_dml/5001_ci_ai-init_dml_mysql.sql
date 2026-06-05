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
