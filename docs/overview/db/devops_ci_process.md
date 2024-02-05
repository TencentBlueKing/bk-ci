# 数据库设计文档

**数据库名：** devops_ci_process

**文档版本：** 1.0.1

**文档描述：** devops_ci_process的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_AUDIT_RESOURCE](#T_AUDIT_RESOURCE) |  |
| [T_PIPELINE_ATOM_REPLACE_BASE](#T_PIPELINE_ATOM_REPLACE_BASE) | 流水线插件替换基本信息表 |
| [T_PIPELINE_ATOM_REPLACE_HISTORY](#T_PIPELINE_ATOM_REPLACE_HISTORY) | 流水线插件替换历史信息表 |
| [T_PIPELINE_ATOM_REPLACE_ITEM](#T_PIPELINE_ATOM_REPLACE_ITEM) | 流水线插件替换项信息表 |
| [T_PIPELINE_BUILD_COMMITS](#T_PIPELINE_BUILD_COMMITS) |  |
| [T_PIPELINE_BUILD_CONTAINER](#T_PIPELINE_BUILD_CONTAINER) | 流水线构建容器环境表 |
| [T_PIPELINE_BUILD_DETAIL](#T_PIPELINE_BUILD_DETAIL) | 流水线构建详情表 |
| [T_PIPELINE_BUILD_HISTORY](#T_PIPELINE_BUILD_HISTORY) | 流水线构建历史表 |
| [T_PIPELINE_BUILD_HIS_DATA_CLEAR](#T_PIPELINE_BUILD_HIS_DATA_CLEAR) | 流水线构建数据清理统计表 |
| [T_PIPELINE_BUILD_RECORD_CONTAINER](#T_PIPELINE_BUILD_RECORD_CONTAINER) | 流水线构建容器环境表 |
| [T_PIPELINE_BUILD_RECORD_MODEL](#T_PIPELINE_BUILD_RECORD_MODEL) | 流水线构建详情表 |
| [T_PIPELINE_BUILD_RECORD_STAGE](#T_PIPELINE_BUILD_RECORD_STAGE) | 流水线构建阶段表 |
| [T_PIPELINE_BUILD_RECORD_TASK](#T_PIPELINE_BUILD_RECORD_TASK) | 流水线构建任务表 |
| [T_PIPELINE_BUILD_STAGE](#T_PIPELINE_BUILD_STAGE) | 流水线构建阶段表 |
| [T_PIPELINE_BUILD_SUMMARY](#T_PIPELINE_BUILD_SUMMARY) | 流水线构建摘要表 |
| [T_PIPELINE_BUILD_TASK](#T_PIPELINE_BUILD_TASK) | 流水线构建任务表 |
| [T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO](#T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO) | 流水线模板跨项目访问表 |
| [T_PIPELINE_BUILD_VAR](#T_PIPELINE_BUILD_VAR) | 流水线变量表 |
| [T_PIPELINE_DATA_CLEAR](#T_PIPELINE_DATA_CLEAR) | 流水线数据清理统计表 |
| [T_PIPELINE_FAVOR](#T_PIPELINE_FAVOR) | 流水线收藏表 |
| [T_PIPELINE_GROUP](#T_PIPELINE_GROUP) | 流水线分组表 |
| [T_PIPELINE_INFO](#T_PIPELINE_INFO) | 流水线信息表 |
| [T_PIPELINE_JOB_MUTEX_GROUP](#T_PIPELINE_JOB_MUTEX_GROUP) |  |
| [T_PIPELINE_LABEL](#T_PIPELINE_LABEL) | 流水线标签表 |
| [T_PIPELINE_LABEL_PIPELINE](#T_PIPELINE_LABEL_PIPELINE) | 流水线-标签映射表 |
| [T_PIPELINE_MODEL_TASK](#T_PIPELINE_MODEL_TASK) | 流水线模型task任务表 |
| [T_PIPELINE_PAUSE_VALUE](#T_PIPELINE_PAUSE_VALUE) | 流水线暂停变量表 |
| [T_PIPELINE_RECENT_USE](#T_PIPELINE_RECENT_USE) | 最近使用的流水线 |
| [T_PIPELINE_REMOTE_AUTH](#T_PIPELINE_REMOTE_AUTH) | 流水线远程触发auth表 |
| [T_PIPELINE_RESOURCE](#T_PIPELINE_RESOURCE) | 流水线资源表 |
| [T_PIPELINE_RESOURCE_VERSION](#T_PIPELINE_RESOURCE_VERSION) | 流水线资源版本表 |
| [T_PIPELINE_RULE](#T_PIPELINE_RULE) | 流水线规则信息表 |
| [T_PIPELINE_SETTING](#T_PIPELINE_SETTING) | 流水线基础配置表 |
| [T_PIPELINE_SETTING_VERSION](#T_PIPELINE_SETTING_VERSION) | 流水线基础配置版本表 |
| [T_PIPELINE_STAGE_TAG](#T_PIPELINE_STAGE_TAG) |  |
| [T_PIPELINE_TIMER](#T_PIPELINE_TIMER) |  |
| [T_PIPELINE_TRIGGER_DETAIL](#T_PIPELINE_TRIGGER_DETAIL) | 流水线触发事件明细表 |
| [T_PIPELINE_TRIGGER_EVENT](#T_PIPELINE_TRIGGER_EVENT) | 流水线触发事件表 |
| [T_PIPELINE_TRIGGER_REVIEW](#T_PIPELINE_TRIGGER_REVIEW) | 流水线触发审核信息 |
| [T_PIPELINE_VIEW](#T_PIPELINE_VIEW) | 流水线视图 |
| [T_PIPELINE_VIEW_GROUP](#T_PIPELINE_VIEW_GROUP) | 流水线组关系表 |
| [T_PIPELINE_VIEW_TOP](#T_PIPELINE_VIEW_TOP) | 流水线组置顶表 |
| [T_PIPELINE_VIEW_USER_LAST_VIEW](#T_PIPELINE_VIEW_USER_LAST_VIEW) |  |
| [T_PIPELINE_VIEW_USER_SETTINGS](#T_PIPELINE_VIEW_USER_SETTINGS) |  |
| [T_PIPELINE_WEBHOOK](#T_PIPELINE_WEBHOOK) |  |
| [T_PIPELINE_WEBHOOK_BUILD_PARAMETER](#T_PIPELINE_WEBHOOK_BUILD_PARAMETER) | webhook构建参数 |
| [T_PIPELINE_WEBHOOK_QUEUE](#T_PIPELINE_WEBHOOK_QUEUE) |  |
| [T_PIPELINE_WEBHOOK_REVISION](#T_PIPELINE_WEBHOOK_REVISION) |  |
| [T_PROJECT_PIPELINE_CALLBACK](#T_PROJECT_PIPELINE_CALLBACK) |  |
| [T_PROJECT_PIPELINE_CALLBACK_HISTORY](#T_PROJECT_PIPELINE_CALLBACK_HISTORY) |  |
| [T_REPORT](#T_REPORT) | 流水线产物表 |
| [T_TEMPLATE](#T_TEMPLATE) | 流水线模板信息表 |
| [T_TEMPLATE_INSTANCE_BASE](#T_TEMPLATE_INSTANCE_BASE) | 模板实列化基本信息表 |
| [T_TEMPLATE_INSTANCE_ITEM](#T_TEMPLATE_INSTANCE_ITEM) | 模板实列化项信息表 |
| [T_TEMPLATE_PIPELINE](#T_TEMPLATE_PIPELINE) | 流水线模板-实例映射表 |

**表名：** <a id="T_AUDIT_RESOURCE">T_AUDIT_RESOURCE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  3   | RESOURCE_ID |   varchar   | 128 |   0    |    N     |  N   |       | 资源ID  |
|  4   | RESOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 资源名称  |
|  5   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  6   | ACTION |   varchar   | 64 |   0    |    N     |  N   |       | 操作  |
|  7   | ACTION_CONTENT |   varchar   | 1024 |   0    |    N     |  N   |       | 操作内容  |
|  8   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 状态  |
|  10   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  N   |       | 项目ID  |

**表名：** <a id="T_PIPELINE_ATOM_REPLACE_BASE">T_PIPELINE_ATOM_REPLACE_BASE</a>

**说明：** 流水线插件替换基本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 流水线ID信息  |
|  4   | FROM_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  5   | TO_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |   INIT    | 状态  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  10   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_PIPELINE_ATOM_REPLACE_HISTORY">T_PIPELINE_ATOM_REPLACE_HISTORY</a>

**说明：** 流水线插件替换历史信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | BUS_ID |   varchar   | 34 |   0    |    N     |  N   |       | 业务ID  |
|  4   | BUS_TYPE |   varchar   | 32 |   0    |    N     |  N   |   PIPELINE    | 业务类型  |
|  5   | SOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 源版本号  |
|  6   | TARGET_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 目标版本号  |
|  7   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  8   | LOG |   varchar   | 128 |   0    |    Y     |  N   |       | 日志  |
|  9   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换基本信息ID  |
|  10   | ITEM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换项信息ID  |
|  11   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  12   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  13   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  14   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_PIPELINE_ATOM_REPLACE_ITEM">T_PIPELINE_ATOM_REPLACE_ITEM</a>

**说明：** 流水线插件替换项信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | FROM_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  3   | FROM_ATOM_VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 被替换插件版本号  |
|  4   | TO_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 替换插件代码  |
|  5   | TO_ATOM_VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 替换插件版本号  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |   INIT    | 状态  |
|  7   | PARAM_REPLACE_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 插件参数替换信息  |
|  8   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换基本信息ID  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_PIPELINE_BUILD_COMMITS">T_PIPELINE_BUILD_COMMITS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       |   |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  6   | MESSAGE |   longtext   | 2147483647 |   0    |    N     |  N   |       |   |
|  7   | AUTHOR_NAME |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  8   | MERGE_REQUEST_ID |   varchar   | 256 |   0    |    Y     |  N   |       |   |
|  9   | REPOSITORY_TYPE |   varchar   | 20 |   0    |    N     |  N   |       |   |
|  10   | COMMIT_TIME |   datetime   | 19 |   0    |    N     |  N   |       |   |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       |   |
|  12   | URL |   varchar   | 255 |   0    |    N     |  N   |       | 仓库url  |
|  13   | EVENT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 触发事件类型  |
|  14   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       |   |
|  15   | ACTION |   varchar   | 64 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_PIPELINE_BUILD_CONTAINER">T_PIPELINE_BUILD_CONTAINER</a>

**说明：** 流水线构建容器环境表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建ID  |
|  4   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前stageId  |
|  5   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器ID  |
|  6   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  7   | SEQ |   int   | 10 |   0    |    N     |  N   |       |   |
|  8   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  9   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 开始时间  |
|  10   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  11   | COST |   int   | 10 |   0    |    Y     |  N   |   0    | 花费  |
|  12   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   1    | 执行次数  |
|  13   | CONDITIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 状况  |
|  14   | CONTAINER_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 容器全局唯一ID  |
|  15   | MATRIX_GROUP_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 是否为构建矩阵  |
|  16   | MATRIX_GROUP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 所属的矩阵组ID  |

**表名：** <a id="T_PIPELINE_BUILD_DETAIL">T_PIPELINE_BUILD_DETAIL</a>

**说明：** 流水线构建详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  3   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |       | 构建次数  |
|  4   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  5   | START_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 启动者  |
|  6   | TRIGGER |   varchar   | 32 |   0    |    Y     |  N   |       | 触发器  |
|  7   | START_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  8   | END_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  9   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 状态  |
|  10   | CANCEL_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 取消者  |

**表名：** <a id="T_PIPELINE_BUILD_HISTORY">T_PIPELINE_BUILD_HISTORY</a>

**说明：** 流水线构建历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PARENT_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级构建ID  |
|  3   | PARENT_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级任务ID  |
|  4   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  6   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  7   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 版本号  |
|  8   | START_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 启动者  |
|  9   | TRIGGER |   varchar   | 32 |   0    |    N     |  N   |       | 触发器  |
|  10   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  11   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  12   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  13   | STAGE_STATUS |   text   | 65535 |   0    |    Y     |  N   |       | 流水线各阶段状态  |
|  14   | TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 流水线任务数量  |
|  15   | FIRST_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 首次任务id  |
|  16   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       | 项目渠道  |
|  17   | TRIGGER_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 触发者  |
|  18   | MATERIAL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 原材料  |
|  19   | QUEUE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 排队开始时间  |
|  20   | ARTIFACT_INFO |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构件列表信息  |
|  21   | REMARK |   varchar   | 4096 |   0    |    Y     |  N   |       | 评论  |
|  22   | EXECUTE_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 执行时间  |
|  23   | BUILD_PARAMETERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构建环境参数  |
|  24   | WEBHOOK_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | WEBHOOK类型  |
|  25   | RECOMMEND_VERSION |   varchar   | 64 |   0    |    Y     |  N   |       | 推荐版本号  |
|  26   | ERROR_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 错误类型  |
|  27   | ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 错误码  |
|  28   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  29   | WEBHOOK_INFO |   text   | 65535 |   0    |    Y     |  N   |       | WEBHOOK信息  |
|  30   | IS_RETRY |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否重试  |
|  31   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 执行次数  |
|  32   | ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  33   | BUILD_MSG |   varchar   | 255 |   0    |    Y     |  N   |       | 构建信息  |
|  34   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |
|  35   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的group  |
|  36   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_PIPELINE_BUILD_HIS_DATA_CLEAR">T_PIPELINE_BUILD_HIS_DATA_CLEAR</a>

**说明：** 流水线构建数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

**表名：** <a id="T_PIPELINE_BUILD_RECORD_CONTAINER">T_PIPELINE_BUILD_RECORD_CONTAINER</a>

**说明：** 流水线构建容器环境表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 编排版本  |
|  5   | STAGE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 步骤ID  |
|  6   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器ID  |
|  7   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  8   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  9   | CONTAINER_VAR |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  10   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  11   | CONTAIN_POST_TASK |   bit   | 1 |   0    |    Y     |  N   |       | 包含POST插件标识  |
|  12   | MATRIX_GROUP_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 矩阵标识  |
|  13   | MATRIX_GROUP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 所属的矩阵组ID  |
|  14   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  15   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  16   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a id="T_PIPELINE_BUILD_RECORD_MODEL">T_PIPELINE_BUILD_RECORD_MODEL</a>

**说明：** 流水线构建详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 编排版本  |
|  5   | BUILD_NUM |   int   | 10 |   0    |    N     |  N   |       | 构建次数  |
|  6   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |       | 执行次数  |
|  7   | START_USER |   varchar   | 32 |   0    |    N     |  N   |       | 启动者  |
|  8   | MODEL_VAR |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  9   | START_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 触发方式  |
|  10   | QUEUE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 触发时间  |
|  11   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 启动时间  |
|  12   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  13   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  14   | ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  15   | CANCEL_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 取消者  |
|  16   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a id="T_PIPELINE_BUILD_RECORD_STAGE">T_PIPELINE_BUILD_RECORD_STAGE</a>

**说明：** 流水线构建阶段表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 编排版本号  |
|  5   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 步骤ID  |
|  6   | SEQ |   int   | 10 |   0    |    N     |  N   |       | 步骤序列  |
|  7   | STAGE_VAR |   text   | 65535 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  8   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  9   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  10   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  11   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  12   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a id="T_PIPELINE_BUILD_RECORD_TASK">T_PIPELINE_BUILD_RECORD_TASK</a>

**说明：** 流水线构建任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 编排版本号  |
|  5   | STAGE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 步骤ID  |
|  6   | CONTAINER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建容器ID  |
|  7   | TASK_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 任务ID  |
|  8   | TASK_SEQ |   int   | 10 |   0    |    N     |  N   |   1    | 任务序列  |
|  9   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  10   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  11   | TASK_VAR |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  12   | POST_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 市场插件的POST关联信息  |
|  13   | CLASS_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  14   | ATOM_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  15   | ORIGIN_CLASS_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 所在矩阵组ID  |
|  16   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  17   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  18   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a id="T_PIPELINE_BUILD_STAGE">T_PIPELINE_BUILD_STAGE</a>

**说明：** 流水线构建阶段表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建ID  |
|  4   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前stageId  |
|  5   | SEQ |   int   | 10 |   0    |    N     |  N   |       |   |
|  6   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  7   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 开始时间  |
|  8   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  9   | COST |   int   | 10 |   0    |    Y     |  N   |   0    | 花费  |
|  10   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   1    | 执行次数  |
|  11   | CONDITIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 状况  |
|  12   | CHECK_IN |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 准入检查配置  |
|  13   | CHECK_OUT |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 准出检查配置  |

**表名：** <a id="T_PIPELINE_BUILD_SUMMARY">T_PIPELINE_BUILD_SUMMARY</a>

**说明：** 流水线构建摘要表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  4   | BUILD_NO |   int   | 10 |   0    |    Y     |  N   |   0    | 构建号  |
|  5   | FINISH_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 完成次数  |
|  6   | RUNNING_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 运行次数  |
|  7   | QUEUE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 排队次数  |
|  8   | LATEST_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 最近构建ID  |
|  9   | LATEST_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 最近任务ID  |
|  10   | LATEST_START_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 最近启动者  |
|  11   | LATEST_START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 最近启动时间  |
|  12   | LATEST_END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 最近结束时间  |
|  13   | LATEST_TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 最近任务计数  |
|  14   | LATEST_TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 最近任务名称  |
|  15   | LATEST_STATUS |   int   | 10 |   0    |    Y     |  N   |       | 最近状态  |
|  16   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |

**表名：** <a id="T_PIPELINE_BUILD_TASK">T_PIPELINE_BUILD_TASK</a>

**说明：** 流水线构建任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  4   | STAGE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 当前stageId  |
|  5   | CONTAINER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建容器ID  |
|  6   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务名称  |
|  7   | TASK_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 任务ID  |
|  8   | TASK_PARAMS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 任务参数集合  |
|  9   | TASK_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 任务类型  |
|  10   | TASK_ATOM |   varchar   | 128 |   0    |    Y     |  N   |       | 任务atom代码  |
|  11   | ATOM_CODE |   varchar   | 128 |   0    |    Y     |  N   |       | 插件的唯一标识  |
|  12   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  13   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  14   | STARTER |   varchar   | 64 |   0    |    N     |  N   |       | 执行人  |
|  15   | APPROVER |   varchar   | 64 |   0    |    Y     |  N   |       | 批准人  |
|  16   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  17   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 执行次数  |
|  18   | TASK_SEQ |   int   | 10 |   0    |    Y     |  N   |   1    | 任务序列  |
|  19   | SUB_PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 子项目id  |
|  20   | SUB_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 子构建id  |
|  21   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  22   | ADDITIONAL_OPTIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 其他选项  |
|  23   | TOTAL_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 总共时间  |
|  24   | ERROR_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 错误类型  |
|  25   | ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 错误码  |
|  26   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  27   | PLATFORM_CODE |   varchar   | 64 |   0    |    Y     |  N   |       | 对接平台代码  |
|  28   | PLATFORM_ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 对接平台错误码  |
|  29   | CONTAINER_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建Job唯一标识  |
|  30   | STEP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 标识上下文的自定义ID  |

**表名：** <a id="T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO">T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO</a>

**说明：** 流水线模板跨项目访问表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TEMPLATE_ID |   char   | 34 |   0    |    N     |  N   |       | 模板唯一UUID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID（P-32位UUID)=34位  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建ID  |
|  6   | TEMPLATE_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 模板类型  |
|  7   | TEMPLATE_INSTANCE_IDS |   text   | 65535 |   0    |    N     |  N   |       | 模板对应的实例ID  |
|  8   | TARGET_PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 使用的项目ID  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建人  |

**表名：** <a id="T_PIPELINE_BUILD_VAR">T_PIPELINE_BUILD_VAR</a>

**说明：** 流水线变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | KEY |   varchar   | 255 |   0    |    N     |  Y   |       | 键  |
|  3   | VALUE |   varchar   | 4000 |   0    |    Y     |  N   |       | 值  |
|  4   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  5   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |
|  6   | VAR_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 变量类型  |
|  7   | READ_ONLY |   bit   | 1 |   0    |    Y     |  N   |       | 是否只读  |

**表名：** <a id="T_PIPELINE_DATA_CLEAR">T_PIPELINE_DATA_CLEAR</a>

**说明：** 流水线数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

**表名：** <a id="T_PIPELINE_FAVOR">T_PIPELINE_FAVOR</a>

**说明：** 流水线收藏表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a id="T_PIPELINE_GROUP">T_PIPELINE_GROUP</a>

**说明：** 流水线分组表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a id="T_PIPELINE_INFO">T_PIPELINE_INFO</a>

**说明：** 流水线信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 流水线名称  |
|  4   | PIPELINE_DESC |   varchar   | 255 |   0    |    Y     |  N   |       | 流水线描述  |
|  5   | VERSION |   int   | 10 |   0    |    Y     |  N   |   1    | 版本号  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  8   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  9   | LAST_MODIFY_USER |   varchar   | 64 |   0    |    N     |  N   |       | 最近修改者  |
|  10   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       | 项目渠道  |
|  11   | MANUAL_STARTUP |   int   | 10 |   0    |    Y     |  N   |   1    | 是否手工启动  |
|  12   | ELEMENT_SKIP |   int   | 10 |   0    |    Y     |  N   |   0    | 是否跳过插件  |
|  13   | TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 流水线任务数量  |
|  14   | DELETE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除  |
|  15   | ID |   bigint   | 20 |   0    |    N     |  N   |       | 主键ID  |
|  16   | PIPELINE_NAME_PINYIN |   varchar   | 1300 |   0    |    Y     |  N   |       | 流水线名称拼音  |
|  17   | LATEST_START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 最近启动时间  |

**表名：** <a id="T_PIPELINE_JOB_MUTEX_GROUP">T_PIPELINE_JOB_MUTEX_GROUP</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | JOB_MUTEX_GROUP_NAME |   varchar   | 127 |   0    |    N     |  Y   |       | Job互斥组名字  |

**表名：** <a id="T_PIPELINE_LABEL">T_PIPELINE_LABEL</a>

**说明：** 流水线标签表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | GROUP_ID |   bigint   | 20 |   0    |    N     |  N   |       | 用户组ID  |
|  4   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  7   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  8   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a id="T_PIPELINE_LABEL_PIPELINE">T_PIPELINE_LABEL_PIPELINE</a>

**说明：** 流水线-标签映射表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | LABEL_ID |   bigint   | 20 |   0    |    N     |  N   |       | 标签ID  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a id="T_PIPELINE_MODEL_TASK">T_PIPELINE_MODEL_TASK</a>

**说明：** 流水线模型task任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  3   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前stageId  |
|  4   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器ID  |
|  5   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 任务ID  |
|  6   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务名称  |
|  7   | CLASS_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 插件大类  |
|  8   | TASK_ATOM |   varchar   | 128 |   0    |    Y     |  N   |       | 任务atom代码  |
|  9   | TASK_SEQ |   int   | 10 |   0    |    Y     |  N   |   1    | 任务序列  |
|  10   | TASK_PARAMS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 任务参数集合  |
|  11   | OS |   varchar   | 45 |   0    |    Y     |  N   |       | 操作系统  |
|  12   | ADDITIONAL_OPTIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 其他选项  |
|  13   | ATOM_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  14   | ATOM_VERSION |   varchar   | 30 |   0    |    Y     |  N   |       | 插件版本号  |
|  15   | CREATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 创建时间  |
|  16   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_PIPELINE_PAUSE_VALUE">T_PIPELINE_PAUSE_VALUE</a>

**说明：** 流水线暂停变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  3   | TASK_ID |   varchar   | 34 |   0    |    N     |  N   |       | 任务ID  |
|  4   | DEFAULT_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 默认变量  |
|  5   | NEW_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 暂停后用户提供的变量  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 添加时间  |
|  7   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 执行次数  |

**表名：** <a id="T_PIPELINE_RECENT_USE">T_PIPELINE_RECENT_USE</a>

**说明：** 最近使用的流水线

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 用户ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线ID  |
|  4   | USE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 使用时间  |

**表名：** <a id="T_PIPELINE_REMOTE_AUTH">T_PIPELINE_REMOTE_AUTH</a>

**说明：** 流水线远程触发auth表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PIPELINE_AUTH |   varchar   | 32 |   0    |    N     |  N   |       | 流水线权限  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a id="T_PIPELINE_RESOURCE">T_PIPELINE_RESOURCE</a>

**说明：** 流水线资源表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  3   | VERSION |   int   | 10 |   0    |    N     |  Y   |   1    | 版本号  |
|  4   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  5   | CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_PIPELINE_RESOURCE_VERSION">T_PIPELINE_RESOURCE_VERSION</a>

**说明：** 流水线资源版本表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  3   | VERSION |   int   | 10 |   0    |    N     |  Y   |   1    | 版本号  |
|  4   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  5   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  6   | REFER_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 是否还有构建记录引用该版本标识  |
|  7   | REFER_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 关联构建记录总数  |
|  8   | CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_PIPELINE_RULE">T_PIPELINE_RULE</a>

**说明：** 流水线规则信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | RULE_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 规则名称  |
|  3   | BUS_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 业务标识  |
|  4   | PROCESSOR |   varchar   | 128 |   0    |    N     |  N   |       | 处理器  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_PIPELINE_SETTING">T_PIPELINE_SETTING</a>

**说明：** 流水线基础配置表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | DESC |   varchar   | 1024 |   0    |    Y     |  N   |       | 描述  |
|  3   | RUN_TYPE |   int   | 10 |   0    |    Y     |  N   |       |   |
|  4   | NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 名称  |
|  5   | SUCCESS_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功接受者  |
|  6   | FAIL_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败接受者  |
|  7   | SUCCESS_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功组  |
|  8   | FAIL_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败组  |
|  9   | SUCCESS_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 成功的通知方式  |
|  10   | FAIL_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 失败的通知方式  |
|  11   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  12   | SUCCESS_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知开关  |
|  13   | SUCCESS_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 成功的企业微信群通知群ID  |
|  14   | FAIL_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知开关  |
|  15   | FAIL_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 失败的企业微信群通知群ID  |
|  16   | RUN_LOCK_TYPE |   int   | 10 |   0    |    Y     |  N   |   1    | Lock类型  |
|  17   | SUCCESS_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 成功的通知的流水线详情连接开关  |
|  18   | FAIL_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 失败的通知的流水线详情连接开关  |
|  19   | SUCCESS_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 成功的自定义通知内容  |
|  20   | FAIL_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 失败的自定义通知内容  |
|  21   | WAIT_QUEUE_TIME_SECOND |   int   | 10 |   0    |    Y     |  N   |   7200    | 最大排队时长  |
|  22   | MAX_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |   10    | 最大排队数量  |
|  23   | IS_TEMPLATE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否模板  |
|  24   | SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知转为Markdown格式开关  |
|  25   | FAIL_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知转为Markdown格式开关  |
|  26   | MAX_PIPELINE_RES_NUM |   int   | 10 |   0    |    Y     |  N   |   500    | 保存流水线编排的最大个数  |
|  27   | MAX_CON_RUNNING_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |   50    | 并发构建数量限制  |
|  28   | BUILD_NUM_RULE |   varchar   | 512 |   0    |    Y     |  N   |       | 构建号生成规则  |
|  29   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的group  |
|  30   | CONCURRENCY_CANCEL_IN_PROGRESS |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 并发时,是否相同group取消正在执行的流水线  |
|  31   | CLEAN_VARIABLES_WHEN_RETRY |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 重试时清理变量表  |
|  32   | PIPELINE_AS_CODE_SETTINGS |   varchar   | 512 |   0    |    Y     |  N   |       | YAML流水线相关配置  |

**表名：** <a id="T_PIPELINE_SETTING_VERSION">T_PIPELINE_SETTING_VERSION</a>

**说明：** 流水线基础配置版本表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | SUCCESS_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功接受者  |
|  4   | FAIL_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败接受者  |
|  5   | SUCCESS_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功组  |
|  6   | FAIL_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败组  |
|  7   | SUCCESS_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 成功的通知方式  |
|  8   | FAIL_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 失败的通知方式  |
|  9   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  10   | SUCCESS_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知开关  |
|  11   | SUCCESS_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 成功的企业微信群通知群ID  |
|  12   | FAIL_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知开关  |
|  13   | FAIL_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 失败的企业微信群通知群ID  |
|  14   | SUCCESS_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 成功的通知的流水线详情连接开关  |
|  15   | FAIL_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 失败的通知的流水线详情连接开关  |
|  16   | SUCCESS_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 成功的自定义通知内容  |
|  17   | FAIL_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 失败的自定义通知内容  |
|  18   | IS_TEMPLATE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否模板  |
|  19   | VERSION |   int   | 10 |   0    |    N     |  N   |   1    | 版本号  |
|  20   | SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知转为Markdown格式开关  |
|  21   | FAIL_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知转为Markdown格式开关  |

**表名：** <a id="T_PIPELINE_STAGE_TAG">T_PIPELINE_STAGE_TAG</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STAGE_TAG_NAME |   varchar   | 45 |   0    |    N     |  N   |       | 阶段标签名称  |
|  3   | WEIGHT |   int   | 10 |   0    |    N     |  N   |   0    | 阶段标签权值  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_PIPELINE_TIMER">T_PIPELINE_TIMER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  3   | CRONTAB |   varchar   | 2048 |   0    |    N     |  N   |       | 任务ID  |
|  4   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  5   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | CHANNEL |   varchar   | 32 |   0    |    N     |  N   |   BS    | 项目渠道  |

**表名：** <a id="T_PIPELINE_TRIGGER_DETAIL">T_PIPELINE_TRIGGER_DETAIL</a>

**说明：** 流水线触发事件明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | DETAIL_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 事件明细ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | EVENT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 事件ID  |
|  4   | STATUS |   varchar   | 100 |   0    |    Y     |  N   |       | 状态(successorfailure)  |
|  5   | PIPELINE_ID |   varchar   | 100 |   0    |    Y     |  N   |       | 流水线ID  |
|  6   | PIPELINE_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 流水线名称  |
|  7   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 流水线版本号  |
|  8   | BUILD_ID |   varchar   | 100 |   0    |    Y     |  N   |       | 构建ID  |
|  9   | BUILD_NUM |   varchar   | 100 |   0    |    Y     |  N   |       | 构建编号  |
|  10   | REASON |   varchar   | 100 |   0    |    Y     |  N   |       | 失败原因  |
|  11   | REASON_DETAIL |   text   | 65535 |   0    |    Y     |  N   |       | 原因详情  |
|  12   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_PIPELINE_TRIGGER_EVENT">T_PIPELINE_TRIGGER_EVENT</a>

**说明：** 流水线触发事件表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REQUEST_ID |   varchar   | 64 |   0    |    N     |  N   |       | 请求ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | EVENT_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 事件ID  |
|  4   | TRIGGER_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 触发类型  |
|  5   | EVENT_SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 触发源,代码库hashId/触发人/远程ip  |
|  6   | EVENT_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 事件类型  |
|  7   | TRIGGER_USER |   varchar   | 100 |   0    |    N     |  N   |       | 触发用户  |
|  8   | EVENT_DESC |   text   | 65535 |   0    |    N     |  N   |       | 事件描述  |
|  9   | REPLAY_REQUEST_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 重放请求ID  |
|  10   | REQUEST_PARAMS |   text   | 65535 |   0    |    Y     |  N   |       | 请求参数  |
|  11   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 事件时间  |

**表名：** <a id="T_PIPELINE_TRIGGER_REVIEW">T_PIPELINE_TRIGGER_REVIEW</a>

**说明：** 流水线触发审核信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | TRIGGER_REVIEWER |   text   | 65535 |   0    |    N     |  N   |       | 触发审核人列表  |
|  5   | TRIGGER_OPERATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 触发审核操作人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 审核时间  |

**表名：** <a id="T_PIPELINE_VIEW">T_PIPELINE_VIEW</a>

**说明：** 流水线视图

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  4   | FILTER_BY_PIPEINE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 流水线名称过滤器,已废弃,统一到filters管理  |
|  5   | FILTER_BY_CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者过滤器,已废弃,统一到filters管理  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  9   | IS_PROJECT |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否项目  |
|  10   | LOGIC |   varchar   | 32 |   0    |    Y     |  N   |   AND    | 逻辑符  |
|  11   | FILTERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 过滤器  |
|  12   | VIEW_TYPE |   int   | 10 |   0    |    N     |  N   |   1    | 1:动态流水线组,2:静态流水线组  |

**表名：** <a id="T_PIPELINE_VIEW_GROUP">T_PIPELINE_VIEW_GROUP</a>

**说明：** 流水线组关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 流水线组ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a id="T_PIPELINE_VIEW_TOP">T_PIPELINE_VIEW_TOP</a>

**说明：** 流水线组置顶表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 流水线组ID  |
|  4   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_PIPELINE_VIEW_USER_LAST_VIEW">T_PIPELINE_VIEW_USER_LAST_VIEW</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 用户ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目ID  |
|  3   | VIEW_ID |   varchar   | 64 |   0    |    N     |  N   |       | 视图ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_PIPELINE_VIEW_USER_SETTINGS">T_PIPELINE_VIEW_USER_SETTINGS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 255 |   0    |    N     |  Y   |       | 用户ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目ID  |
|  3   | SETTINGS |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 属性配置表  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_PIPELINE_WEBHOOK">T_PIPELINE_WEBHOOK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 新版的git插件的类型  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | REPO_HASH_ID |   varchar   | 45 |   0    |    Y     |  N   |       | 存储库HASHID  |
|  5   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  6   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  7   | REPO_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 代码库类型  |
|  8   | PROJECT_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 项目名称  |
|  9   | TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 任务id  |
|  10   | DELETE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除  |
|  11   | EVENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 事件类型  |
|  12   | EXTERNAL_ID |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库平台ID  |
|  13   | REPOSITORY_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库hashId  |
|  14   | EXTERNAL_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库平台仓库名  |

**表名：** <a id="T_PIPELINE_WEBHOOK_BUILD_PARAMETER">T_PIPELINE_WEBHOOK_BUILD_PARAMETER</a>

**说明：** webhook构建参数

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  4   | BUILD_PARAMETERS |   text   | 65535 |   0    |    Y     |  N   |       |   |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |

**表名：** <a id="T_PIPELINE_WEBHOOK_QUEUE">T_PIPELINE_WEBHOOK_QUEUE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | SOURCE_PROJECT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 源项目ID  |
|  5   | SOURCE_REPO_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 源代码库名称  |
|  6   | SOURCE_BRANCH |   varchar   | 255 |   0    |    N     |  N   |       | 源分支  |
|  7   | TARGET_PROJECT_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 目标项目ID  |
|  8   | TARGET_REPO_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 目标代码库名称  |
|  9   | TARGET_BRANCH |   varchar   | 255 |   0    |    Y     |  N   |       | 目标分支  |
|  10   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_PIPELINE_WEBHOOK_REVISION">T_PIPELINE_WEBHOOK_REVISION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_NAME |   varchar   | 255 |   0    |    Y     |  N   |       |   |
|  3   | REVISION |   varchar   | 64 |   0    |    Y     |  N   |       |   |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       |   |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_PROJECT_PIPELINE_CALLBACK">T_PROJECT_PIPELINE_CALLBACK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | EVENTS |   varchar   | 255 |   0    |    Y     |  N   |       | 事件  |
|  4   | CALLBACK_URL |   varchar   | 255 |   0    |    N     |  N   |       | 回调url地址  |
|  5   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  6   | UPDATOR |   varchar   | 64 |   0    |    N     |  N   |       | 更新人  |
|  7   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | SECRET_TOKEN |   text   | 65535 |   0    |    Y     |  N   |       | Sendtoyourwithhttpheader:X-DEVOPS-WEBHOOK-TOKEN  |
|  10   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 启用  |

**表名：** <a id="T_PROJECT_PIPELINE_CALLBACK_HISTORY">T_PROJECT_PIPELINE_CALLBACK_HISTORY</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | EVENTS |   varchar   | 255 |   0    |    Y     |  N   |       | 事件  |
|  4   | CALLBACK_URL |   varchar   | 255 |   0    |    N     |  N   |       | 回调url地址  |
|  5   | STATUS |   varchar   | 20 |   0    |    N     |  N   |       | 状态  |
|  6   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  7   | REQUEST_HEADER |   text   | 65535 |   0    |    Y     |  N   |       | 请求头  |
|  8   | REQUEST_BODY |   text   | 65535 |   0    |    N     |  N   |       | 请求body  |
|  9   | RESPONSE_CODE |   int   | 10 |   0    |    Y     |  N   |       | 响应code  |
|  10   | RESPONSE_BODY |   text   | 65535 |   0    |    Y     |  N   |       | 响应body  |
|  11   | START_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 开始时间  |
|  12   | END_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 结束时间  |
|  13   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  Y   |       | 创建时间  |

**表名：** <a id="T_REPORT">T_REPORT</a>

**说明：** 流水线产物表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  5   | ELEMENT_ID |   varchar   | 34 |   0    |    N     |  N   |       | 原子ID  |
|  6   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   INTERNAL    | 类型  |
|  7   | INDEX_FILE |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 入口文件  |
|  8   | NAME |   text   | 65535 |   0    |    N     |  N   |       | 名称  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  11   | TASK_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 任务名称  |
|  12   | ATOM_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 插件的唯一标识  |

**表名：** <a id="T_TEMPLATE">T_TEMPLATE</a>

**说明：** 流水线模板信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | VERSION |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ID |   varchar   | 32 |   0    |    N     |  N   |       | 主键ID  |
|  3   | TEMPLATE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 模板名称  |
|  4   | PROJECT_ID |   varchar   | 34 |   0    |    N     |  N   |       | 项目ID  |
|  5   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  6   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  7   | CREATED_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 更新时间  |
|  9   | TEMPLATE |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 模板  |
|  10   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   CUSTOMIZE    | 类型  |
|  11   | CATEGORY |   varchar   | 128 |   0    |    Y     |  N   |       | 应用范畴  |
|  12   | LOGO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | LOGOURL地址  |
|  13   | SRC_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模版ID  |
|  14   | STORE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否已关联到store  |
|  15   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |   0    | 权值  |

**表名：** <a id="T_TEMPLATE_INSTANCE_BASE">T_TEMPLATE_INSTANCE_BASE</a>

**说明：** 模板实列化基本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 模板ID  |
|  3   | TEMPLATE_VERSION |   varchar   | 32 |   0    |    N     |  N   |       | 模板版本  |
|  4   | USE_TEMPLATE_SETTINGS_FLAG |   bit   | 1 |   0    |    N     |  N   |       | 是否使用模板配置  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  6   | TOTAL_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 总实例化数量  |
|  7   | SUCCESS_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 实例化成功数量  |
|  8   | FAIL_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 实例化失败数量  |
|  9   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  12   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  13   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_TEMPLATE_INSTANCE_ITEM">T_TEMPLATE_INSTANCE_ITEM</a>

**说明：** 模板实列化项信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | PIPELINE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 流水线名称  |
|  5   | BUILD_NO_INFO |   varchar   | 512 |   0    |    Y     |  N   |       | 构建号信息  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  7   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 实列化基本信息ID  |
|  8   | PARAM |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 参数  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_TEMPLATE_PIPELINE">T_TEMPLATE_PIPELINE</a>

**说明：** 流水线模板-实例映射表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  3   | INSTANCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |   CONSTRAINT    | 实例化类型：FREEDOM自由模式CONSTRAINT约束模式  |
|  4   | ROOT_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模板ID  |
|  5   | VERSION |   bigint   | 20 |   0    |    N     |  N   |       | 版本号  |
|  6   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  7   | TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  8   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  9   | UPDATOR |   varchar   | 64 |   0    |    N     |  N   |       | 更新人  |
|  10   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  11   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  12   | BUILD_NO |   text   | 65535 |   0    |    Y     |  N   |       | 构建号  |
|  13   | PARAM |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 参数  |
|  14   | DELETED |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 流水线已被软删除  |
