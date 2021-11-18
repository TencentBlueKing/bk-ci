# 数据库设计文档

**数据库名：** devops_ci_process

**文档版本：** 1.0.0

**文档描述：** devops_ci_process的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_AUDIT_RESOURCE](#T_AUDIT_RESOURCE) |  |
| [T_BUILD_STARTUP_PARAM](#T_BUILD_STARTUP_PARAM) | 流水线启动变量表 |
| [T_METADATA](#T_METADATA) |  |
| [T_PIPELINE_ATOM_REPLACE_BASE](#T_PIPELINE_ATOM_REPLACE_BASE) | 流水线插件替换基本信息表 |
| [T_PIPELINE_ATOM_REPLACE_HISTORY](#T_PIPELINE_ATOM_REPLACE_HISTORY) | 流水线插件替换历史信息表 |
| [T_PIPELINE_ATOM_REPLACE_ITEM](#T_PIPELINE_ATOM_REPLACE_ITEM) | 流水线插件替换项信息表 |
| [T_PIPELINE_BUILD_CONTAINER](#T_PIPELINE_BUILD_CONTAINER) | 流水线构建容器环境表 |
| [T_PIPELINE_BUILD_DETAIL](#T_PIPELINE_BUILD_DETAIL) | 流水线构建详情表 |
| [T_PIPELINE_BUILD_HISTORY](#T_PIPELINE_BUILD_HISTORY) | 流水线构建历史表 |
| [T_PIPELINE_BUILD_HIS_DATA_CLEAR](#T_PIPELINE_BUILD_HIS_DATA_CLEAR) | 流水线构建数据清理统计表 |
| [T_PIPELINE_BUILD_STAGE](#T_PIPELINE_BUILD_STAGE) | 流水线构建阶段表 |
| [T_PIPELINE_BUILD_SUMMARY](#T_PIPELINE_BUILD_SUMMARY) | 流水线构建摘要表 |
| [T_PIPELINE_BUILD_TASK](#T_PIPELINE_BUILD_TASK) | 流水线构建任务表 |
| [T_PIPELINE_BUILD_VAR](#T_PIPELINE_BUILD_VAR) | 流水线变量表 |
| [T_PIPELINE_CONTAINER_MONITOR](#T_PIPELINE_CONTAINER_MONITOR) |  |
| [T_PIPELINE_DATA_CLEAR](#T_PIPELINE_DATA_CLEAR) | 流水线数据清理统计表 |
| [T_PIPELINE_FAILURE_NOTIFY_USER](#T_PIPELINE_FAILURE_NOTIFY_USER) |  |
| [T_PIPELINE_FAVOR](#T_PIPELINE_FAVOR) | 流水线收藏表 |
| [T_PIPELINE_GROUP](#T_PIPELINE_GROUP) | 流水线分组表 |
| [T_PIPELINE_INFO](#T_PIPELINE_INFO) | 流水线信息表 |
| [T_PIPELINE_JOB_MUTEX_GROUP](#T_PIPELINE_JOB_MUTEX_GROUP) |  |
| [T_PIPELINE_LABEL](#T_PIPELINE_LABEL) | 流水线标签表 |
| [T_PIPELINE_LABEL_PIPELINE](#T_PIPELINE_LABEL_PIPELINE) | 流水线-标签映射表 |
| [T_PIPELINE_MODEL_TASK](#T_PIPELINE_MODEL_TASK) | 流水线模型task任务表 |
| [T_PIPELINE_MUTEX_GROUP](#T_PIPELINE_MUTEX_GROUP) | 流水线互斥表 |
| [T_PIPELINE_PAUSE_VALUE](#T_PIPELINE_PAUSE_VALUE) | 流水线暂停变量表 |
| [T_PIPELINE_REMOTE_AUTH](#T_PIPELINE_REMOTE_AUTH) | 流水线远程触发auth表 |
| [T_PIPELINE_RESOURCE](#T_PIPELINE_RESOURCE) | 流水线资源表 |
| [T_PIPELINE_RESOURCE_VERSION](#T_PIPELINE_RESOURCE_VERSION) | 流水线资源版本表 |
| [T_PIPELINE_RULE](#T_PIPELINE_RULE) | 流水线规则信息表 |
| [T_PIPELINE_SETTING](#T_PIPELINE_SETTING) | 流水线基础配置表 |
| [T_PIPELINE_SETTING_VERSION](#T_PIPELINE_SETTING_VERSION) | 流水线基础配置版本表 |
| [T_PIPELINE_STAGE_TAG](#T_PIPELINE_STAGE_TAG) |  |
| [T_PIPELINE_TEMPLATE](#T_PIPELINE_TEMPLATE) | 流水线模板表 |
| [T_PIPELINE_TIMER](#T_PIPELINE_TIMER) |  |
| [T_PIPELINE_USER](#T_PIPELINE_USER) |  |
| [T_PIPELINE_VIEW](#T_PIPELINE_VIEW) |  |
| [T_PIPELINE_VIEW_LABEL](#T_PIPELINE_VIEW_LABEL) |  |
| [T_PIPELINE_VIEW_PROJECT](#T_PIPELINE_VIEW_PROJECT) |  |
| [T_PIPELINE_VIEW_USER_LAST_VIEW](#T_PIPELINE_VIEW_USER_LAST_VIEW) |  |
| [T_PIPELINE_VIEW_USER_SETTINGS](#T_PIPELINE_VIEW_USER_SETTINGS) |  |
| [T_PIPELINE_WEBHOOK](#T_PIPELINE_WEBHOOK) |  |
| [T_PIPELINE_WEBHOOK_BUILD_LOG](#T_PIPELINE_WEBHOOK_BUILD_LOG) |  |
| [T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL](#T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL) |  |
| [T_PIPELINE_WEBHOOK_QUEUE](#T_PIPELINE_WEBHOOK_QUEUE) |  |
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

**表名：** <a id="T_BUILD_STARTUP_PARAM">T_BUILD_STARTUP_PARAM</a>

**说明：** 流水线启动变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建ID  |
|  3   | PARAM |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 参数  |
|  4   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  5   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |

**表名：** <a id="T_METADATA">T_METADATA</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  5   | META_DATA_ID |   varchar   | 128 |   0    |    N     |  N   |       | 元数据ID  |
|  6   | META_DATA_VALUE |   varchar   | 255 |   0    |    N     |  N   |       | 元数据值  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

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
|  31   | ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  32   | BUILD_MSG |   varchar   | 255 |   0    |    Y     |  N   |       | 构建信息  |
|  33   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |

**表名：** <a id="T_PIPELINE_BUILD_HIS_DATA_CLEAR">T_PIPELINE_BUILD_HIS_DATA_CLEAR</a>

**说明：** 流水线构建数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

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
|  27   | CONTAINER_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建Job唯一标识  |

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

**表名：** <a id="T_PIPELINE_CONTAINER_MONITOR">T_PIPELINE_CONTAINER_MONITOR</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | OS_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 系统类型  |
|  3   | BUILD_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 构建类型  |
|  4   | MAX_STARTUP_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 最长启动时间  |
|  5   | MAX_EXECUTE_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 最长执行时间  |
|  6   | USERS |   varchar   | 1024 |   0    |    N     |  N   |       | 用户列表  |

**表名：** <a id="T_PIPELINE_DATA_CLEAR">T_PIPELINE_DATA_CLEAR</a>

**说明：** 流水线数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

**表名：** <a id="T_PIPELINE_FAILURE_NOTIFY_USER">T_PIPELINE_FAILURE_NOTIFY_USER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 用户ID  |
|  3   | NOTIFY_TYPES |   varchar   | 32 |   0    |    Y     |  N   |       | 通知类型  |

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
|  14   | ATOM_VERSION |   varchar   | 20 |   0    |    Y     |  N   |       | 插件版本号  |
|  15   | CREATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 创建时间  |
|  16   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_PIPELINE_MUTEX_GROUP">T_PIPELINE_MUTEX_GROUP</a>

**说明：** 流水线互斥表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | GROUP_NAME |   varchar   | 127 |   0    |    N     |  Y   |       | 用户组名称  |

**表名：** <a id="T_PIPELINE_PAUSE_VALUE">T_PIPELINE_PAUSE_VALUE</a>

**说明：** 流水线暂停变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建ID  |
|  3   | TASK_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 任务ID  |
|  4   | DEFAULT_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 默认变量  |
|  5   | NEW_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 暂停后用户提供的变量  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 添加时间  |

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
|  6   | CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  7   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

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

**表名：** <a id="T_PIPELINE_TEMPLATE">T_PIPELINE_TEMPLATE</a>

**说明：** 流水线模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   FREEDOM    | 类型  |
|  3   | CATEGORY |   varchar   | 128 |   0    |    Y     |  N   |       | 应用范畴  |
|  4   | TEMPLATE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 模板名称  |
|  5   | ICON |   varchar   | 32 |   0    |    N     |  N   |       | 模板图标  |
|  6   | LOGO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | LOGOURL地址  |
|  7   | PROJECT_CODE |   varchar   | 32 |   0    |    Y     |  N   |       | 用户组所属项目  |
|  8   | SRC_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模版ID  |
|  9   | AUTHOR |   varchar   | 64 |   0    |    N     |  N   |       | 作者  |
|  10   | ATOMNUM |   int   | 10 |   0    |    N     |  N   |       | 插件数量  |
|  11   | PUBLIC_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否为公共镜像  |
|  12   | TEMPLATE |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 模板  |
|  13   | CREATOR |   varchar   | 32 |   0    |    N     |  N   |       | 创建者  |
|  14   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

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

**表名：** <a id="T_PIPELINE_USER">T_PIPELINE_USER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  4   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  6   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a id="T_PIPELINE_VIEW">T_PIPELINE_VIEW</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  4   | FILTER_BY_PIPEINE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 流水线名称过滤器  |
|  5   | FILTER_BY_CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者过滤器  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  9   | IS_PROJECT |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否项目  |
|  10   | LOGIC |   varchar   | 32 |   0    |    Y     |  N   |   AND    | 逻辑符  |
|  11   | FILTERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 过滤器  |

**表名：** <a id="T_PIPELINE_VIEW_LABEL">T_PIPELINE_VIEW_LABEL</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  2   | VIEW_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 视图ID  |
|  3   | LABEL_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 标签ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |

**表名：** <a id="T_PIPELINE_VIEW_PROJECT">T_PIPELINE_VIEW_PROJECT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 视图ID  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

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

**表名：** <a id="T_PIPELINE_WEBHOOK_BUILD_LOG">T_PIPELINE_WEBHOOK_BUILD_LOG</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CODE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 代码库类型  |
|  3   | REPO_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 代码库别名  |
|  4   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交ID  |
|  5   | REQUEST_CONTENT |   text   | 65535 |   0    |    Y     |  N   |       | 事件内容  |
|  6   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | RECEIVED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 接收时间  |
|  8   | FINISHED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 完成时间  |

**表名：** <a id="T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL">T_PIPELINE_WEBHOOK_BUILD_LOG_DETAIL</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LOG_ID |   bigint   | 20 |   0    |    N     |  N   |       |   |
|  3   | CODE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 代码库类型  |
|  4   | REPO_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 代码库别名  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交ID  |
|  6   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  7   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  8   | TASK_ID |   varchar   | 34 |   0    |    N     |  N   |       | 任务id  |
|  9   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务名称  |
|  10   | SUCCESS |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否成功  |
|  11   | TRIGGER_RESULT |   text   | 65535 |   0    |    Y     |  N   |       | 触发结果  |
|  12   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |

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
|  7   | CREATED_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  8   | TEMPLATE |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 模板  |
|  9   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   CUSTOMIZE    | 类型  |
|  10   | CATEGORY |   varchar   | 128 |   0    |    Y     |  N   |       | 应用范畴  |
|  11   | LOGO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | LOGOURL地址  |
|  12   | SRC_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模版ID  |
|  13   | STORE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否已关联到store  |
|  14   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |   0    | 权值  |

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
