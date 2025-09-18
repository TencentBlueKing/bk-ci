# 数据库设计文档

**数据库名：** devops_ci_process

**文档版本：** 1.0.13

**文档描述：** devops_ci_process 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_AUDIT_RESOURCE |  |
| T_PIPELINE_ATOM_REPLACE_BASE | 流水线插件替换基本信息表 |
| T_PIPELINE_ATOM_REPLACE_HISTORY | 流水线插件替换历史信息表 |
| T_PIPELINE_ATOM_REPLACE_ITEM | 流水线插件替换项信息表 |
| T_PIPELINE_BUILD_CHECK_RUN | 构建任务关联检查项信息表 |
| T_PIPELINE_BUILD_CONTAINER | 流水线构建容器环境表 |
| T_PIPELINE_BUILD_DETAIL | 流水线构建详情表 |
| T_PIPELINE_BUILD_HISTORY | 流水线构建历史表 |
| T_PIPELINE_BUILD_HISTORY_DEBUG | 流水线调试构建历史表 |
| T_PIPELINE_BUILD_HIS_DATA_CLEAR | 流水线构建数据清理统计表 |
| T_PIPELINE_BUILD_RECORD_CONTAINER | 流水线构建容器环境表 |
| T_PIPELINE_BUILD_RECORD_MODEL | 流水线构建详情表 |
| T_PIPELINE_BUILD_RECORD_STAGE | 流水线构建阶段表 |
| T_PIPELINE_BUILD_RECORD_TASK | 流水线构建任务表 |
| T_PIPELINE_BUILD_STAGE | 流水线构建阶段表 |
| T_PIPELINE_BUILD_SUMMARY | 流水线构建摘要表 |
| T_PIPELINE_BUILD_TASK | 流水线构建任务表 |
| T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO | 流水线模板跨项目访问表 |
| T_PIPELINE_BUILD_VAR | 流水线变量表 |
| T_PIPELINE_CALLBACK | 流水线级别回调事件表 |
| T_PIPELINE_DATA_CLEAR | 流水线数据清理统计表 |
| T_PIPELINE_FAVOR | 流水线收藏表 |
| T_PIPELINE_GROUP | 流水线分组表 |
| T_PIPELINE_INFO | 流水线信息表 |
| T_PIPELINE_JOB_MUTEX_GROUP |  |
| T_PIPELINE_LABEL | 流水线标签表 |
| T_PIPELINE_LABEL_PIPELINE | 流水线-标签映射表 |
| T_PIPELINE_MODEL_TASK | 流水线模型 task 任务表 |
| T_PIPELINE_OPERATION_LOG | 流水线操作记录表 |
| T_PIPELINE_PAUSE_VALUE | 流水线暂停变量表 |
| T_PIPELINE_RECENT_USE | 最近使用的流水线 |
| T_PIPELINE_REMOTE_AUTH | 流水线远程触发 auth 表 |
| T_PIPELINE_RESOURCE | 流水线资源表 |
| T_PIPELINE_RESOURCE_VERSION | 流水线资源版本表 |
| T_PIPELINE_RULE | 流水线规则信息表 |
| T_PIPELINE_SETTING | 流水线基础配置表 |
| T_PIPELINE_SETTING_VERSION | 流水线基础配置版本表 |
| T_PIPELINE_STAGE_TAG |  |
| T_PIPELINE_SUB_REF | 子流水线依赖关系 |
| T_PIPELINE_TIMER |  |
| T_PIPELINE_TIMER_BRANCH | 定时触发分支版本 |
| T_PIPELINE_TRIGGER_DETAIL | 流水线触发事件明细表 |
| T_PIPELINE_TRIGGER_EVENT | 流水线触发事件表 |
| T_PIPELINE_TRIGGER_REVIEW | 流水线触发审核信息 |
| T_PIPELINE_VIEW | 流水线视图 |
| T_PIPELINE_VIEW_GROUP | 流水线组关系表 |
| T_PIPELINE_VIEW_TOP | 流水线组置顶表 |
| T_PIPELINE_VIEW_USER_LAST_VIEW |  |
| T_PIPELINE_VIEW_USER_SETTINGS |  |
| T_PIPELINE_WEBHOOK |  |
| T_PIPELINE_WEBHOOK_BUILD_PARAMETER | webhook 构建参数 |
| T_PIPELINE_WEBHOOK_QUEUE |  |
| T_PIPELINE_WEBHOOK_REVISION |  |
| T_PIPELINE_WEBHOOK_VERSION | 流水线 webhook 版本 |
| T_PIPELINE_YAML_BRANCH_FILE | yaml 分支文件 |
| T_PIPELINE_YAML_INFO | 流水线 yaml 信息表 |
| T_PIPELINE_YAML_SYNC | yaml 文件同步记录 |
| T_PIPELINE_YAML_VERSION | 流水线 yaml 版本 |
| T_PIPELINE_YAML_VIEW | yaml 流水线组 |
| T_PROJECT_PIPELINE_CALLBACK |  |
| T_PROJECT_PIPELINE_CALLBACK_HISTORY |  |
| T_REPORT | 流水线产物表 |
| T_TEMPLATE | 流水线模板信息表 |
| T_TEMPLATE_INSTANCE_BASE | 模板实列化基本信息表 |
| T_TEMPLATE_INSTANCE_ITEM | 模板实列化项信息表 |
| T_TEMPLATE_PIPELINE | 流水线模板-实例映射表 |

**表名：** <a>T_AUDIT_RESOURCE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  3   | RESOURCE_ID |   varchar   | 128 |   0    |    N     |  N   |       | 资源 ID  |
|  4   | RESOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 资源名称  |
|  5   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  6   | ACTION |   varchar   | 64 |   0    |    N     |  N   |       | 操作  |
|  7   | ACTION_CONTENT |   varchar   | 1024 |   0    |    N     |  N   |       | 操作内容  |
|  8   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 状态  |
|  10   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  N   |       | 项目 ID  |

**表名：** <a>T_PIPELINE_ATOM_REPLACE_BASE</a>

**说明：** 流水线插件替换基本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 流水线 ID 信息  |
|  4   | FROM_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  5   | TO_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |   INIT    | 状态  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  10   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_PIPELINE_ATOM_REPLACE_HISTORY</a>

**说明：** 流水线插件替换历史信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  3   | BUS_ID |   varchar   | 34 |   0    |    N     |  N   |       | 业务 ID  |
|  4   | BUS_TYPE |   varchar   | 32 |   0    |    N     |  N   |   PIPELINE    | 业务类型  |
|  5   | SOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 源版本号  |
|  6   | TARGET_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 目标版本号  |
|  7   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  8   | LOG |   varchar   | 128 |   0    |    Y     |  N   |       | 日志  |
|  9   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换基本信息 ID  |
|  10   | ITEM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换项信息 ID  |
|  11   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  12   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  13   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  14   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_PIPELINE_ATOM_REPLACE_ITEM</a>

**说明：** 流水线插件替换项信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | FROM_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 被替换插件代码  |
|  3   | FROM_ATOM_VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 被替换插件版本号  |
|  4   | TO_ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 替换插件代码  |
|  5   | TO_ATOM_VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 替换插件版本号  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |   INIT    | 状态  |
|  7   | PARAM_REPLACE_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 插件参数替换信息  |
|  8   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件替换基本信息 ID  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_PIPELINE_BUILD_CHECK_RUN</a>

**说明：** 构建任务关联检查项信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 蓝盾项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建任务 ID  |
|  4   | BUILD_NUM |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  5   | BUILD_STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 构建状态  |
|  6   | REPO_HASH_ID |   varchar   | 32 |   0    |    N     |  N   |       | 代码库 HASH_ID  |
|  7   | CONTEXT |   varchar   | 255 |   0    |    N     |  N   |       | 检查项名称  |
|  8   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 检查项关联版本  |
|  9   | PULL_REQUEST_ID |   bigint   | 20 |   0    |    N     |  N   |   0    | 合并请求 ID  |
|  10   | CHECK_RUN_STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 检查项状态  |
|  11   | CHECK_RUN_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 检查项 ID  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_PIPELINE_BUILD_CONTAINER</a>

**说明：** 流水线构建容器环境表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建 ID  |
|  4   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前 stageId  |
|  5   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器 ID  |
|  6   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  7   | SEQ |   int   | 10 |   0    |    N     |  N   |       |   |
|  8   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  9   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 开始时间  |
|  10   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  11   | COST |   int   | 10 |   0    |    Y     |  N   |   0    | 花费  |
|  12   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   1    | 执行次数  |
|  13   | CONDITIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 状况  |
|  14   | CONTAINER_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 容器全局唯一 ID  |
|  15   | MATRIX_GROUP_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 是否为构建矩阵  |
|  16   | MATRIX_GROUP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 所属的矩阵组 ID  |
|  17   | JOB_ID |   varchar   | 128 |   0    |    Y     |  N   |       | jobid  |

**表名：** <a>T_PIPELINE_BUILD_DETAIL</a>

**说明：** 流水线构建详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  3   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |       | 构建次数  |
|  4   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  5   | START_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 启动者  |
|  6   | TRIGGER |   varchar   | 32 |   0    |    Y     |  N   |       | 触发器  |
|  7   | START_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  8   | END_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  9   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 状态  |
|  10   | CANCEL_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 取消者  |

**表名：** <a>T_PIPELINE_BUILD_HISTORY</a>

**说明：** 流水线构建历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PARENT_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级构建 ID  |
|  3   | PARENT_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级任务 ID  |
|  4   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  6   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  7   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 编排版本号  |
|  8   | START_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 启动者  |
|  9   | TRIGGER |   varchar   | 32 |   0    |    N     |  N   |       | 触发器  |
|  10   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  11   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  12   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  13   | STAGE_STATUS |   text   | 65535 |   0    |    Y     |  N   |       | 流水线各阶段状态  |
|  14   | TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 流水线任务数量  |
|  15   | FIRST_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 首次任务 id  |
|  16   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       | 项目渠道  |
|  17   | TRIGGER_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 触发者  |
|  18   | MATERIAL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 原材料  |
|  19   | QUEUE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 排队开始时间  |
|  20   | ARTIFACT_INFO |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构件列表信息  |
|  21   | ARTIFACT_QUALITY_INFO |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 制品质量分析结果  |
|  22   | REMARK |   varchar   | 4096 |   0    |    Y     |  N   |       | 评论  |
|  23   | EXECUTE_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 执行时间  |
|  24   | BUILD_PARAMETERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构建环境参数  |
|  25   | WEBHOOK_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | WEBHOOK 类型  |
|  26   | RECOMMEND_VERSION |   varchar   | 64 |   0    |    Y     |  N   |       | 推荐版本号  |
|  27   | ERROR_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 错误类型  |
|  28   | ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 错误码  |
|  29   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  30   | WEBHOOK_INFO |   text   | 65535 |   0    |    Y     |  N   |       | WEBHOOK 信息  |
|  31   | IS_RETRY |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否重试  |
|  32   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 执行次数  |
|  33   | ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  34   | BUILD_MSG |   varchar   | 255 |   0    |    Y     |  N   |       | 构建信息  |
|  35   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |
|  36   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的 group  |
|  37   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  38   | VERSION_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 正式版本名称  |
|  39   | YAML_VERSION |   varchar   | 34 |   0    |    Y     |  N   |       | YAML 的版本标记  |

**表名：** <a>T_PIPELINE_BUILD_HISTORY_DEBUG</a>

**说明：** 流水线调试构建历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PARENT_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级构建 ID  |
|  3   | PARENT_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 父级任务 ID  |
|  4   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  6   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  7   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 编排版本号  |
|  8   | START_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 启动者  |
|  9   | TRIGGER |   varchar   | 32 |   0    |    N     |  N   |       | 触发器  |
|  10   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  11   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  12   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  13   | STAGE_STATUS |   text   | 65535 |   0    |    Y     |  N   |       | 流水线各阶段状态  |
|  14   | TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 流水线任务数量  |
|  15   | FIRST_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 首次任务 id  |
|  16   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       | 项目渠道  |
|  17   | TRIGGER_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 触发者  |
|  18   | MATERIAL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 原材料  |
|  19   | QUEUE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 排队开始时间  |
|  20   | ARTIFACT_INFO |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构件列表信息  |
|  21   | ARTIFACT_QUALITY_INFO |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 制品质量分析结果  |
|  22   | REMARK |   varchar   | 4096 |   0    |    Y     |  N   |       | 评论  |
|  23   | EXECUTE_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 执行时间  |
|  24   | BUILD_PARAMETERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 构建环境参数  |
|  25   | WEBHOOK_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | WEBHOOK 类型  |
|  26   | RECOMMEND_VERSION |   varchar   | 64 |   0    |    Y     |  N   |       | 推荐版本号  |
|  27   | ERROR_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 错误类型  |
|  28   | ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 错误码  |
|  29   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  30   | WEBHOOK_INFO |   text   | 65535 |   0    |    Y     |  N   |       | WEBHOOK 信息  |
|  31   | ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  32   | BUILD_MSG |   varchar   | 255 |   0    |    Y     |  N   |       | 构建信息  |
|  33   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |
|  34   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的 group  |
|  35   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  36   | REPO_TRIGGER_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 触发库信息  |
|  37   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 最大执行次数  |
|  38   | IS_RETRY |   bit   | 1 |   0    |    Y     |  N   |       | 是否进行过重试  |
|  39   | YAML_VERSION |   varchar   | 34 |   0    |    Y     |  N   |       | YAML 的版本标记  |
|  40   | RESOURCE_MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 本次调试的编排备份  |
|  41   | DELETE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 记录删除时间  |

**表名：** <a>T_PIPELINE_BUILD_HIS_DATA_CLEAR</a>

**说明：** 流水线构建数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  4   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

**表名：** <a>T_PIPELINE_BUILD_RECORD_CONTAINER</a>

**说明：** 流水线构建容器环境表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 编排版本  |
|  5   | STAGE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 步骤 ID  |
|  6   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器 ID  |
|  7   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  8   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  9   | CONTAINER_VAR |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  10   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  11   | CONTAIN_POST_TASK |   bit   | 1 |   0    |    Y     |  N   |       | 包含 POST 插件标识  |
|  12   | MATRIX_GROUP_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 矩阵标识  |
|  13   | MATRIX_GROUP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 所属的矩阵组 ID  |
|  14   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  15   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  16   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a>T_PIPELINE_BUILD_RECORD_MODEL</a>

**说明：** 流水线构建详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
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

**表名：** <a>T_PIPELINE_BUILD_RECORD_STAGE</a>

**说明：** 流水线构建阶段表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 编排版本号  |
|  5   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 步骤 ID  |
|  6   | SEQ |   int   | 10 |   0    |    N     |  N   |       | 步骤序列  |
|  7   | STAGE_VAR |   text   | 65535 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  8   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  9   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  10   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  11   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  12   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |

**表名：** <a>T_PIPELINE_BUILD_RECORD_TASK</a>

**说明：** 流水线构建任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | RESOURCE_VERSION |   int   | 10 |   0    |    N     |  N   |       | 编排版本号  |
|  5   | STAGE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 步骤 ID  |
|  6   | CONTAINER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建容器 ID  |
|  7   | TASK_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 任务 ID  |
|  8   | TASK_SEQ |   int   | 10 |   0    |    N     |  N   |   1    | 任务序列  |
|  9   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 执行次数  |
|  10   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 构建状态  |
|  11   | TASK_VAR |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 当次执行的变量记录  |
|  12   | POST_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 市场插件的 POST 关联信息  |
|  13   | CLASS_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  14   | ATOM_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  15   | ORIGIN_CLASS_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 所在矩阵组 ID  |
|  16   | START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 开始时间  |
|  17   | END_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 结束时间  |
|  18   | TIMESTAMPS |   text   | 65535 |   0    |    Y     |  N   |       | 运行中产生的时间戳集合  |
|  19   | ASYNC_STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 插件异步执行状态  |

**表名：** <a>T_PIPELINE_BUILD_STAGE</a>

**说明：** 流水线构建阶段表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建 ID  |
|  4   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前 stageId  |
|  5   | SEQ |   int   | 10 |   0    |    N     |  N   |       |   |
|  6   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  7   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 开始时间  |
|  8   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  9   | COST |   int   | 10 |   0    |    Y     |  N   |   0    | 花费  |
|  10   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   1    | 执行次数  |
|  11   | CONDITIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 状况  |
|  12   | CHECK_IN |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 准入检查配置  |
|  13   | CHECK_OUT |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 准出检查配置  |
|  14   | STAGE_ID_FOR_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 当前 stageId 阶段 ID(用户可编辑)  |

**表名：** <a>T_PIPELINE_BUILD_SUMMARY</a>

**说明：** 流水线构建摘要表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  4   | BUILD_NO |   int   | 10 |   0    |    Y     |  N   |   0    | 构建号  |
|  5   | FINISH_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 完成次数  |
|  6   | RUNNING_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 运行次数  |
|  7   | QUEUE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 排队次数  |
|  8   | LATEST_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 最近构建 ID  |
|  9   | LATEST_TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 最近任务 ID  |
|  10   | LATEST_START_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 最近启动者  |
|  11   | LATEST_START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 最近启动时间  |
|  12   | LATEST_END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 最近结束时间  |
|  13   | LATEST_TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 最近任务计数  |
|  14   | LATEST_TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 最近任务名称  |
|  15   | LATEST_STATUS |   int   | 10 |   0    |    Y     |  N   |       | 最近状态  |
|  16   | BUILD_NUM_ALIAS |   varchar   | 256 |   0    |    Y     |  N   |       | 自定义构建号  |
|  17   | DEBUG_BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 调试构建次数  |
|  18   | DEBUG_BUILD_NO |   int   | 10 |   0    |    Y     |  N   |   0    | 调试构建号  |

**表名：** <a>T_PIPELINE_BUILD_TASK</a>

**说明：** 流水线构建任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  4   | STAGE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 当前 stageId  |
|  5   | CONTAINER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建容器 ID  |
|  6   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务名称  |
|  7   | TASK_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 任务 ID  |
|  8   | TASK_PARAMS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 任务参数集合  |
|  9   | TASK_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 任务类型  |
|  10   | TASK_ATOM |   varchar   | 128 |   0    |    Y     |  N   |       | 任务 atom 代码  |
|  11   | ATOM_CODE |   varchar   | 128 |   0    |    Y     |  N   |       | 插件的唯一标识  |
|  12   | START_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  13   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  14   | STARTER |   varchar   | 64 |   0    |    N     |  N   |       | 执行人  |
|  15   | APPROVER |   varchar   | 64 |   0    |    Y     |  N   |       | 批准人  |
|  16   | STATUS |   int   | 10 |   0    |    Y     |  N   |       | 状态  |
|  17   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 执行次数  |
|  18   | TASK_SEQ |   int   | 10 |   0    |    Y     |  N   |   1    | 任务序列  |
|  19   | SUB_PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 子项目 id  |
|  20   | SUB_BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 子构建 id  |
|  21   | CONTAINER_TYPE |   varchar   | 45 |   0    |    Y     |  N   |       | 容器类型  |
|  22   | ADDITIONAL_OPTIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 其他选项  |
|  23   | TOTAL_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 总共时间  |
|  24   | ERROR_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 错误类型  |
|  25   | ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 错误码  |
|  26   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  27   | PLATFORM_CODE |   varchar   | 64 |   0    |    Y     |  N   |       | 对接平台代码  |
|  28   | PLATFORM_ERROR_CODE |   int   | 10 |   0    |    Y     |  N   |       | 对接平台错误码  |
|  29   | CONTAINER_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建 Job 唯一标识  |
|  30   | STEP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 标识上下文的自定义 ID  |
|  31   | JOB_ID |   varchar   | 128 |   0    |    Y     |  N   |       | jobid  |

**表名：** <a>T_PIPELINE_BUILD_TEMPLATE_ACROSS_INFO</a>

**说明：** 流水线模板跨项目访问表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | TEMPLATE_ID |   char   | 34 |   0    |    N     |  N   |       | 模板唯一 UUID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID（P-32 位 UUID)=34 位  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建 ID  |
|  6   | TEMPLATE_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 模板类型  |
|  7   | TEMPLATE_INSTANCE_IDS |   text   | 65535 |   0    |    N     |  N   |       | 模板对应的实例 ID  |
|  8   | TARGET_PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 使用的项目 ID  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建人  |

**表名：** <a>T_PIPELINE_BUILD_VAR</a>

**说明：** 流水线变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | KEY |   varchar   | 255 |   0    |    N     |  Y   |       | 键  |
|  3   | VALUE |   varchar   | 4000 |   0    |    Y     |  N   |       | 值  |
|  4   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  5   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线 ID  |
|  6   | VAR_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 变量类型  |
|  7   | READ_ONLY |   bit   | 1 |   0    |    Y     |  N   |       | 是否只读  |

**表名：** <a>T_PIPELINE_CALLBACK</a>

**说明：** 流水线级别回调事件表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 蓝盾项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | NAME |   varchar   | 255 |   0    |    N     |  Y   |       | 回调名称  |
|  4   | EVENT_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 事件类型  |
|  5   | REGION |   varchar   | 32 |   0    |    Y     |  N   |       | 网络域  |
|  6   | URL |   varchar   | 256 |   0    |    N     |  N   |       | 回调地址  |
|  7   | SECRET_TOKEN |   varchar   | 100 |   0    |    Y     |  N   |       | 鉴权参数  |
|  8   | USER_ID |   varchar   | 100 |   0    |    Y     |  N   |       | 创建人  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a>T_PIPELINE_DATA_CLEAR</a>

**说明：** 流水线数据清理统计表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | DEL_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    |   |

**表名：** <a>T_PIPELINE_FAVOR</a>

**说明：** 流水线收藏表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a>T_PIPELINE_GROUP</a>

**说明：** 流水线分组表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a>T_PIPELINE_INFO</a>

**说明：** 流水线信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
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
|  15   | ID |   bigint   | 20 |   0    |    N     |  N   |       | 主键 ID  |
|  16   | PIPELINE_NAME_PINYIN |   varchar   | 1300 |   0    |    Y     |  N   |       | 流水线名称拼音  |
|  17   | LATEST_START_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 最近启动时间  |
|  18   | LATEST_VERSION_STATUS |   varchar   | 64 |   0    |    Y     |  N   |       | 最新分布版本状态  |
|  19   | LOCKED |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否锁定，PACv3.0 新增锁定，取代原来 setting 表中的 LOCK  |

**表名：** <a>T_PIPELINE_JOB_MUTEX_GROUP</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | JOB_MUTEX_GROUP_NAME |   varchar   | 127 |   0    |    N     |  Y   |       | Job 互斥组名字  |

**表名：** <a>T_PIPELINE_LABEL</a>

**说明：** 流水线标签表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | GROUP_ID |   bigint   | 20 |   0    |    N     |  N   |       | 用户组 ID  |
|  4   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  7   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  8   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a>T_PIPELINE_LABEL_PIPELINE</a>

**说明：** 流水线-标签映射表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | LABEL_ID |   bigint   | 20 |   0    |    N     |  N   |       | 标签 ID  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a>T_PIPELINE_MODEL_TASK</a>

**说明：** 流水线模型 task 任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  3   | STAGE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 当前 stageId  |
|  4   | CONTAINER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建容器 ID  |
|  5   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 任务 ID  |
|  6   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务名称  |
|  7   | CLASS_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 插件大类  |
|  8   | TASK_ATOM |   varchar   | 128 |   0    |    Y     |  N   |       | 任务 atom 代码  |
|  9   | TASK_SEQ |   int   | 10 |   0    |    Y     |  N   |   1    | 任务序列  |
|  10   | TASK_PARAMS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 任务参数集合  |
|  11   | OS |   varchar   | 45 |   0    |    Y     |  N   |       | 操作系统  |
|  12   | ADDITIONAL_OPTIONS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 其他选项  |
|  13   | ATOM_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  14   | ATOM_VERSION |   varchar   | 30 |   0    |    Y     |  N   |       | 插件版本号  |
|  15   | CREATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 创建时间  |
|  16   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a>T_PIPELINE_OPERATION_LOG</a>

**说明：** 流水线操作记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 自增 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | VERSION |   int   | 10 |   0    |    N     |  N   |       | 操作版本号  |
|  5   | OPERATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  6   | OPERATION_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 操作类型  |
|  7   | PARAMS |   varchar   | 255 |   0    |    Y     |  N   |       | 操作参数  |
|  8   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 版本变更说明  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_PIPELINE_PAUSE_VALUE</a>

**说明：** 流水线暂停变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建 ID  |
|  3   | TASK_ID |   varchar   | 34 |   0    |    N     |  N   |       | 任务 ID  |
|  4   | DEFAULT_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 默认变量  |
|  5   | NEW_VALUE |   text   | 65535 |   0    |    Y     |  N   |       | 暂停后用户提供的变量  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 添加时间  |
|  7   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 执行次数  |

**表名：** <a>T_PIPELINE_RECENT_USE</a>

**说明：** 最近使用的流水线

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 用户 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线 ID  |
|  4   | USE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 使用时间  |

**表名：** <a>T_PIPELINE_REMOTE_AUTH</a>

**说明：** 流水线远程触发 auth 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PIPELINE_AUTH |   varchar   | 32 |   0    |    N     |  N   |       | 流水线权限  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a>T_PIPELINE_RESOURCE</a>

**说明：** 流水线资源表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | VERSION |   int   | 10 |   0    |    N     |  Y   |   1    | 版本号  |
|  4   | VERSION_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 版本名称  |
|  5   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  6   | YAML |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | YAML 编排  |
|  7   | YAML_VERSION |   varchar   | 34 |   0    |    Y     |  N   |       | YAML 的版本标记  |
|  8   | CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | VERSION_NUM |   int   | 10 |   0    |    Y     |  N   |       | 流水线发布版本  |
|  11   | PIPELINE_VERSION |   int   | 10 |   0    |    Y     |  N   |   0    | 流水线模型版本  |
|  12   | TRIGGER_VERSION |   int   | 10 |   0    |    Y     |  N   |   0    | 触发器模型版本  |
|  13   | SETTING_VERSION |   int   | 10 |   0    |    Y     |  N   |   0    | 关联的流水线设置版本号  |

**表名：** <a>T_PIPELINE_RESOURCE_VERSION</a>

**说明：** 流水线资源版本表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | VERSION |   int   | 10 |   0    |    N     |  Y   |   1    | 版本号  |
|  4   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  5   | MODEL |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 流水线模型  |
|  6   | YAML |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | YAML 编排  |
|  7   | YAML_VERSION |   varchar   | 34 |   0    |    Y     |  N   |       | YAML 的版本标记  |
|  8   | CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | REFER_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 是否还有构建记录引用该版本标识  |
|  11   | REFER_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 关联构建记录总数  |
|  12   | VERSION_NUM |   int   | 10 |   0    |    Y     |  N   |       | 流水线发布版本  |
|  13   | PIPELINE_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 流水线模型版本  |
|  14   | TRIGGER_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 触发器模型版本  |
|  15   | SETTING_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 关联的流水线设置版本号  |
|  16   | BASE_VERSION |   int   | 10 |   0    |    Y     |  N   |       | 草稿的来源版本  |
|  17   | DEBUG_BUILD_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 调试构建 ID  |
|  18   | STATUS |   varchar   | 16 |   0    |    Y     |  N   |       | 版本状态  |
|  19   | BRANCH_ACTION |   varchar   | 32 |   0    |    Y     |  N   |       | 分支状态  |
|  20   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 版本变更说明  |
|  21   | UPDATER |   varchar   | 64 |   0    |    Y     |  N   |       | 最近更新人  |
|  22   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  23   | RELEASE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 发布时间  |

**表名：** <a>T_PIPELINE_RULE</a>

**说明：** 流水线规则信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | RULE_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 规则名称  |
|  3   | BUS_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 业务标识  |
|  4   | PROCESSOR |   varchar   | 128 |   0    |    N     |  N   |       | 处理器  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_PIPELINE_SETTING</a>

**说明：** 流水线基础配置表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  3   | DESC |   varchar   | 1024 |   0    |    Y     |  N   |       | 描述  |
|  4   | RUN_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 运行锁定类型  |
|  5   | NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 名称  |
|  6   | SUCCESS_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功接受者  |
|  7   | FAIL_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败接受者  |
|  8   | SUCCESS_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 成功组  |
|  9   | FAIL_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 失败组  |
|  10   | SUCCESS_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 成功的通知方式  |
|  11   | FAIL_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 失败的通知方式  |
|  12   | SUCCESS_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知开关  |
|  13   | SUCCESS_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 成功的企业微信群通知群 ID  |
|  14   | FAIL_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知开关  |
|  15   | FAIL_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       | 失败的企业微信群通知群 ID  |
|  16   | RUN_LOCK_TYPE |   int   | 10 |   0    |    Y     |  N   |   1    | Lock 类型  |
|  17   | SUCCESS_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 成功的通知的流水线详情连接开关  |
|  18   | FAIL_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 失败的通知的流水线详情连接开关  |
|  19   | SUCCESS_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 成功的自定义通知内容  |
|  20   | FAIL_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 失败的自定义通知内容  |
|  21   | WAIT_QUEUE_TIME_SECOND |   int   | 10 |   0    |    Y     |  N   |   7200    | 最大排队时长  |
|  22   | MAX_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |   10    | 最大排队数量  |
|  23   | IS_TEMPLATE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否模板  |
|  24   | SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 成功的企业微信群通知转为 Markdown 格式开关  |
|  25   | FAIL_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 失败的企业微信群通知转为 Markdown 格式开关  |
|  26   | MAX_PIPELINE_RES_NUM |   int   | 10 |   0    |    Y     |  N   |   500    | 保存流水线编排的最大个数  |
|  27   | MAX_CON_RUNNING_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |       | 并发构建数量限制,为 null 时表示取系统默认值  |
|  28   | BUILD_NUM_RULE |   varchar   | 512 |   0    |    Y     |  N   |       | 构建号生成规则  |
|  29   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的 group  |
|  30   | CONCURRENCY_CANCEL_IN_PROGRESS |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 并发时,是否相同 group 取消正在执行的流水线  |
|  31   | CLEAN_VARIABLES_WHEN_RETRY |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 重试时清理变量表  |
|  32   | PIPELINE_AS_CODE_SETTINGS |   varchar   | 512 |   0    |    Y     |  N   |       | YAML 流水线相关配置  |
|  33   | VERSION |   int   | 10 |   0    |    Y     |  N   |   1    | 设置版本  |
|  34   | SUCCESS_SUBSCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 成功订阅设置  |
|  35   | FAILURE_SUBSCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 失败订阅设置  |
|  36   | FAIL_IF_VARIABLE_INVALID |   bit   | 1 |   0    |    Y     |  N   |       | 是否配置流水线变量值超长时终止执行  |

**表名：** <a>T_PIPELINE_SETTING_VERSION</a>

**说明：** 流水线基础配置版本表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | VERSION |   int   | 10 |   0    |    N     |  N   |   1    | 版本号  |
|  5   | IS_TEMPLATE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否模板  |
|  6   | NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 名称  |
|  7   | DESC |   varchar   | 1024 |   0    |    Y     |  N   |       | 描述  |
|  8   | LABELS |   text   | 65535 |   0    |    Y     |  N   |       | 版本修改的标签  |
|  9   | WAIT_QUEUE_TIME_SECOND |   int   | 10 |   0    |    Y     |  N   |   7200    | 最大排队时长  |
|  10   | MAX_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |   10    | 最大排队数量  |
|  11   | BUILD_NUM_RULE |   varchar   | 512 |   0    |    Y     |  N   |       | 构建号生成规则  |
|  12   | CONCURRENCY_GROUP |   varchar   | 255 |   0    |    Y     |  N   |       | 并发时,设定的 group  |
|  13   | CONCURRENCY_CANCEL_IN_PROGRESS |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 并发时,是否相同 group 取消正在执行的流水线  |
|  14   | PIPELINE_AS_CODE_SETTINGS |   varchar   | 512 |   0    |    Y     |  N   |       | YAML 流水线相关配置  |
|  15   | SUCCESS_SUBSCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 成功订阅设置  |
|  16   | FAILURE_SUBSCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 失败订阅设置  |
|  17   | RUN_LOCK_TYPE |   int   | 10 |   0    |    Y     |  N   |   1    | 运行并发配置  |
|  18   | SUCCESS_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       |   |
|  19   | FAIL_RECEIVER |   mediumtext   | 16777215 |   0    |    Y     |  N   |       |   |
|  20   | SUCCESS_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       |   |
|  21   | FAIL_GROUP |   mediumtext   | 16777215 |   0    |    Y     |  N   |       |   |
|  22   | SUCCESS_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       |   |
|  23   | FAIL_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       |   |
|  24   | SUCCESS_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
|  25   | SUCCESS_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       |   |
|  26   | FAIL_WECHAT_GROUP_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
|  27   | FAIL_WECHAT_GROUP |   varchar   | 1024 |   0    |    N     |  N   |       |   |
|  28   | SUCCESS_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    |   |
|  29   | FAIL_DETAIL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    |   |
|  30   | SUCCESS_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       |   |
|  31   | FAIL_CONTENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       |   |
|  32   | SUCCESS_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
|  33   | FAIL_WECHAT_GROUP_MARKDOWN_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    |   |
|  34   | MAX_CON_RUNNING_QUEUE_SIZE |   int   | 10 |   0    |    Y     |  N   |       | 并发构建数量限制,值为-1 时表示取系统默认值。  |
|  35   | FAIL_IF_VARIABLE_INVALID |   bit   | 1 |   0    |    Y     |  N   |       | 是否配置流水线变量值超长时终止执行  |

**表名：** <a>T_PIPELINE_STAGE_TAG</a>

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

**表名：** <a>T_PIPELINE_SUB_REF</a>

**说明：** 子流水线依赖关系

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 蓝盾项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 蓝盾流水线 ID  |
|  3   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | TASKID  |
|  4   | PIPELINE_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 流水线名称  |
|  5   | TASK_POSITION |   varchar   | 256 |   0    |    Y     |  N   |       | 插件所在位置[stageIndex-containerIndex-taskIndex]  |
|  6   | TASK_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | TASK 名称  |
|  7   | SUB_PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 子流水线项目 ID  |
|  8   | SUB_PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 子流水线流水线 ID  |
|  9   | SUB_PIPELINE_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 子流水线名称  |
|  10   | TASK_PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 插件源参数_projectId  |
|  11   | TASK_PIPELINE_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 插件源参数_type  |
|  12   | TASK_PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 插件源参数_pipelineId  |
|  13   | TASK_PIPELINE_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 插件源参数_pipelineName  |
|  14   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |       | 流水线渠道  |

**表名：** <a>T_PIPELINE_TIMER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 插件 ID  |
|  4   | CRONTAB |   varchar   | 2048 |   0    |    N     |  N   |       | 任务 ID  |
|  5   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | CHANNEL |   varchar   | 32 |   0    |    N     |  N   |   BS    | 项目渠道  |
|  8   | REPO_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库 HASHID  |
|  9   | BRANCHS |   text   | 65535 |   0    |    Y     |  N   |       | 分支列表  |
|  10   | NO_SCM |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 源代码未更新则不触发构建  |
|  11   | START_PARAM |   text   | 65535 |   0    |    Y     |  N   |       | 启动参数  |

**表名：** <a>T_PIPELINE_TIMER_BRANCH</a>

**说明：** 定时触发分支版本

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 插件 ID  |
|  4   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 代码库 HASHID  |
|  5   | BRANCH |   varchar   | 255 |   0    |    N     |  Y   |       | 分支  |
|  6   | REVISION |   varchar   | 40 |   0    |    N     |  N   |       | 提交版本  |
|  7   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_PIPELINE_TRIGGER_DETAIL</a>

**说明：** 流水线触发事件明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | DETAIL_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 事件明细 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | EVENT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 事件 ID  |
|  4   | STATUS |   varchar   | 100 |   0    |    Y     |  N   |       | 状态(successorfailure)  |
|  5   | PIPELINE_ID |   varchar   | 100 |   0    |    Y     |  N   |       | 流水线 ID  |
|  6   | PIPELINE_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 流水线名称  |
|  7   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 流水线版本号  |
|  8   | BUILD_ID |   varchar   | 100 |   0    |    Y     |  N   |       | 构建 ID  |
|  9   | BUILD_NUM |   varchar   | 100 |   0    |    Y     |  N   |       | 构建编号  |
|  10   | REASON |   varchar   | 100 |   0    |    Y     |  N   |       | 失败原因  |
|  11   | REASON_DETAIL |   text   | 65535 |   0    |    Y     |  N   |       | 原因详情  |
|  12   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_PIPELINE_TRIGGER_EVENT</a>

**说明：** 流水线触发事件表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REQUEST_ID |   varchar   | 64 |   0    |    N     |  N   |       | 请求 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | EVENT_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 事件 ID  |
|  4   | TRIGGER_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 触发类型  |
|  5   | EVENT_SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 触发源,代码库 hashId/触发人/远程 ip  |
|  6   | EVENT_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 事件类型  |
|  7   | TRIGGER_USER |   varchar   | 100 |   0    |    N     |  N   |       | 触发用户  |
|  8   | EVENT_DESC |   text   | 65535 |   0    |    N     |  N   |       | 事件描述  |
|  9   | REPLAY_REQUEST_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 重放请求 ID  |
|  10   | REQUEST_PARAMS |   text   | 65535 |   0    |    Y     |  N   |       | 请求参数  |
|  11   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 事件时间  |
|  12   | EVENT_BODY |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 事件体  |

**表名：** <a>T_PIPELINE_TRIGGER_REVIEW</a>

**说明：** 流水线触发审核信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 构建 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | TRIGGER_REVIEWER |   text   | 65535 |   0    |    N     |  N   |       | 触发审核人列表  |
|  5   | TRIGGER_OPERATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 触发审核操作人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 审核时间  |

**表名：** <a>T_PIPELINE_VIEW</a>

**说明：** 流水线视图

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | NAME |   varchar   | 255 |   0    |    N     |  N   |       | 名称  |
|  4   | FILTER_BY_PIPEINE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 流水线名称过滤器,已废弃,统一到 filters 管理  |
|  5   | FILTER_BY_CREATOR |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者过滤器,已废弃,统一到 filters 管理  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  9   | IS_PROJECT |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否项目  |
|  10   | LOGIC |   varchar   | 32 |   0    |    Y     |  N   |   AND    | 逻辑符  |
|  11   | FILTERS |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 过滤器  |
|  12   | VIEW_TYPE |   int   | 10 |   0    |    N     |  N   |   1    | 1:动态流水线组,2:静态流水线组  |

**表名：** <a>T_PIPELINE_VIEW_GROUP</a>

**说明：** 流水线组关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 流水线组 ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a>T_PIPELINE_VIEW_TOP</a>

**说明：** 流水线组置顶表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 流水线组 ID  |
|  4   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a>T_PIPELINE_VIEW_USER_LAST_VIEW</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 用户 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  3   | VIEW_ID |   varchar   | 64 |   0    |    N     |  N   |       | 视图 ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_PIPELINE_VIEW_USER_SETTINGS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 255 |   0    |    N     |  Y   |       | 用户 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  3   | SETTINGS |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 属性配置表  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_PIPELINE_WEBHOOK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 新版的 git 插件的类型  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | REPO_HASH_ID |   varchar   | 45 |   0    |    Y     |  N   |       | 存储库 HASHID  |
|  5   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  6   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  7   | REPO_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 代码库类型  |
|  8   | PROJECT_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 项目名称  |
|  9   | TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 任务 id  |
|  10   | DELETE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除  |
|  11   | EVENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 事件类型  |
|  12   | EXTERNAL_ID |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库平台 ID  |
|  13   | REPOSITORY_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库 hashId  |
|  14   | EXTERNAL_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库平台仓库名  |

**表名：** <a>T_PIPELINE_WEBHOOK_BUILD_PARAMETER</a>

**说明：** webhook 构建参数

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 34 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  4   | BUILD_PARAMETERS |   text   | 65535 |   0    |    Y     |  N   |       |   |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |

**表名：** <a>T_PIPELINE_WEBHOOK_QUEUE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | SOURCE_PROJECT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 源项目 ID  |
|  5   | SOURCE_REPO_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 源代码库名称  |
|  6   | SOURCE_BRANCH |   varchar   | 255 |   0    |    N     |  N   |       | 源分支  |
|  7   | TARGET_PROJECT_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 目标项目 ID  |
|  8   | TARGET_REPO_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 目标代码库名称  |
|  9   | TARGET_BRANCH |   varchar   | 255 |   0    |    Y     |  N   |       | 目标分支  |
|  10   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建 ID  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_PIPELINE_WEBHOOK_REVISION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_NAME |   varchar   | 255 |   0    |    Y     |  N   |       |   |
|  3   | REVISION |   varchar   | 64 |   0    |    Y     |  N   |       |   |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       |   |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       |   |

**表名：** <a>T_PIPELINE_WEBHOOK_VERSION</a>

**说明：** 流水线 webhook 版本

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 流水线版本  |
|  4   | TASK_ID |   varchar   | 34 |   0    |    N     |  N   |       | 插件 ID  |
|  5   | TASK_PARAMS |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 插件参数  |
|  6   | TASK_REPO_HASH_ID |   varchar   | 45 |   0    |    Y     |  N   |       | 插件配置的 hashId  |
|  7   | TASK_REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 插件配置的代码库别名  |
|  8   | TASK_REPO_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 插件配置的代码库类型,ID|NAME  |
|  9   | REPOSITORY_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 代码库类型  |
|  10   | REPOSITORY_HASH_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码库 hashId  |
|  11   | EVENT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 事件类型  |

**表名：** <a>T_PIPELINE_YAML_BRANCH_FILE</a>

**说明：** yaml 分支文件

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 代码库 HASHID  |
|  3   | BRANCH |   varchar   | 255 |   0    |    N     |  Y   |       | 分支  |
|  4   | FILE_PATH |   varchar   | 512 |   0    |    N     |  N   |       | 文件路径  |
|  5   | FILE_PATH_MD5 |   varchar   | 64 |   0    |    N     |  Y   |       | 文件路径 MD5  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | COMMIT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 文件 commitId  |
|  9   | BLOB_ID |   varchar   | 64 |   0    |    N     |  N   |       | 文件 blob_id  |
|  10   | COMMIT_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 提交时间  |
|  11   | DELETED |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否删除  |

**表名：** <a>T_PIPELINE_YAML_INFO</a>

**说明：** 流水线 yaml 信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 蓝盾项目 ID  |
|  2   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 代码库 HASHID  |
|  3   | FILE_PATH |   varchar   | 512 |   0    |    N     |  Y   |       | 文件路径  |
|  4   | DIRECTORY |   varchar   | 512 |   0    |    N     |  N   |   .ci    | yaml 文件目录  |
|  5   | DEFAULT_BRANCH |   varchar   | 512 |   0    |    Y     |  N   |       | 默认分支  |
|  6   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  7   | STATUS |   varchar   | 10 |   0    |    Y     |  N   |   OK    | 状态  |
|  8   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建人  |
|  9   | MODIFIER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |
|  10   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  11   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a>T_PIPELINE_YAML_SYNC</a>

**说明：** yaml 文件同步记录

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 代码库 HASHID  |
|  3   | FILE_PATH |   varchar   | 512 |   0    |    N     |  Y   |       | 文件路径  |
|  4   | FILE_URL |   text   | 65535 |   0    |    Y     |  N   |       | 文件 URL  |
|  5   | SYNC_STATUS |   varchar   | 10 |   0    |    Y     |  N   |       | ci 文件同步状态  |
|  6   | REASON |   varchar   | 100 |   0    |    Y     |  N   |       | 失败原因  |
|  7   | REASON_DETAIL |   text   | 65535 |   0    |    Y     |  N   |       | 原因详情  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_PIPELINE_YAML_VERSION</a>

**说明：** 流水线 yaml 版本

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 蓝盾项目 ID  |
|  3   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码库 HASHID  |
|  4   | FILE_PATH |   varchar   | 512 |   0    |    N     |  N   |       | 文件路径  |
|  5   | REF |   varchar   | 512 |   0    |    Y     |  N   |       | 来源分支/tag  |
|  6   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 文件 commitId  |
|  7   | COMMIT_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 提交时间  |
|  8   | BLOB_ID |   varchar   | 64 |   0    |    N     |  N   |       | 文件 blob_id  |
|  9   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  10   | VERSION |   int   | 10 |   0    |    Y     |  N   |       | 流水线版本  |
|  11   | BRANCH_ACTION |   varchar   | 32 |   0    |    N     |  N   |   ACTIVE    | 分支状态  |
|  12   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建人  |
|  13   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_PIPELINE_YAML_VIEW</a>

**说明：** yaml 流水线组

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | REPO_HASH_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 代码库 HASHID  |
|  3   | DIRECTORY |   varchar   | 512 |   0    |    N     |  Y   |       | yaml 文件目录  |
|  4   | VIEW_ID |   bigint   | 20 |   0    |    N     |  N   |       | 流水线组 ID  |

**表名：** <a>T_PROJECT_PIPELINE_CALLBACK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | EVENTS |   varchar   | 255 |   0    |    Y     |  N   |       | 事件  |
|  4   | CALLBACK_URL |   varchar   | 255 |   0    |    N     |  N   |       | 回调 url 地址  |
|  5   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  6   | UPDATOR |   varchar   | 64 |   0    |    N     |  N   |       | 更新人  |
|  7   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | SECRET_TOKEN |   text   | 65535 |   0    |    Y     |  N   |       | Sendtoyourwithhttpheader:X-DEVOPS-WEBHOOK-TOKEN  |
|  10   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 启用  |
|  11   | FAILURE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 失败时间  |
|  12   | SECRET_PARAM |   text   | 65535 |   0    |    Y     |  N   |       | 鉴权参数  |

**表名：** <a>T_PROJECT_PIPELINE_CALLBACK_HISTORY</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | EVENTS |   varchar   | 255 |   0    |    Y     |  N   |       | 事件  |
|  4   | CALLBACK_URL |   varchar   | 255 |   0    |    N     |  N   |       | 回调 url 地址  |
|  5   | STATUS |   varchar   | 20 |   0    |    N     |  N   |       | 状态  |
|  6   | ERROR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 错误描述  |
|  7   | REQUEST_HEADER |   text   | 65535 |   0    |    Y     |  N   |       | 请求头  |
|  8   | REQUEST_BODY |   text   | 65535 |   0    |    N     |  N   |       | 请求 body  |
|  9   | RESPONSE_CODE |   int   | 10 |   0    |    Y     |  N   |       | 响应 code  |
|  10   | RESPONSE_BODY |   text   | 65535 |   0    |    Y     |  N   |       | 响应 body  |
|  11   | START_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 开始时间  |
|  12   | END_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 结束时间  |
|  13   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  Y   |       | 创建时间  |

**表名：** <a>T_REPORT</a>

**说明：** 流水线产物表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建 ID  |
|  5   | ELEMENT_ID |   varchar   | 34 |   0    |    N     |  N   |       | 原子 ID  |
|  6   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   INTERNAL    | 类型  |
|  7   | INDEX_FILE |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 入口文件  |
|  8   | NAME |   text   | 65535 |   0    |    N     |  N   |       | 名称  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  11   | TASK_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 任务名称  |
|  12   | ATOM_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 插件的唯一标识  |

**表名：** <a>T_TEMPLATE</a>

**说明：** 流水线模板信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | VERSION |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | ID |   varchar   | 32 |   0    |    N     |  N   |       | 主键 ID  |
|  3   | TEMPLATE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 模板名称  |
|  4   | PROJECT_ID |   varchar   | 34 |   0    |    N     |  N   |       | 项目 ID  |
|  5   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  6   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  7   | CREATED_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 更新时间  |
|  9   | TEMPLATE |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 模板  |
|  10   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   CUSTOMIZE    | 类型  |
|  11   | CATEGORY |   varchar   | 128 |   0    |    Y     |  N   |       | 应用范畴  |
|  12   | LOGO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | LOGOURL 地址  |
|  13   | SRC_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模版 ID  |
|  14   | STORE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否已关联到 store  |
|  15   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |   0    | 权值  |
|  16   | DESC |   varchar   | 1024 |   0    |    Y     |  N   |       | 描述  |

**表名：** <a>T_TEMPLATE_INSTANCE_BASE</a>

**说明：** 模板实列化基本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 模板 ID  |
|  3   | TEMPLATE_VERSION |   varchar   | 32 |   0    |    N     |  N   |       | 模板版本  |
|  4   | USE_TEMPLATE_SETTINGS_FLAG |   bit   | 1 |   0    |    N     |  N   |       | 是否使用模板配置  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  6   | TOTAL_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 总实例化数量  |
|  7   | SUCCESS_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 实例化成功数量  |
|  8   | FAIL_ITEM_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 实例化失败数量  |
|  9   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  12   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  13   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_TEMPLATE_INSTANCE_ITEM</a>

**说明：** 模板实列化项信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | PIPELINE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 流水线名称  |
|  5   | BUILD_NO_INFO |   varchar   | 512 |   0    |    Y     |  N   |       | 构建号信息  |
|  6   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  7   | BASE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 实列化基本信息 ID  |
|  8   | PARAM |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 参数  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_TEMPLATE_PIPELINE</a>

**说明：** 流水线模板-实例映射表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  2   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       | 流水线 ID  |
|  3   | INSTANCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |   CONSTRAINT    | 实例化类型：FREEDOM 自由模式 CONSTRAINT 约束模式  |
|  4   | ROOT_TEMPLATE_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 源模板 ID  |
|  5   | VERSION |   bigint   | 20 |   0    |    N     |  N   |       | 版本号  |
|  6   | VERSION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 版本名称  |
|  7   | TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板 ID  |
|  8   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  9   | UPDATOR |   varchar   | 64 |   0    |    N     |  N   |       | 更新人  |
|  10   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  11   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  12   | BUILD_NO |   text   | 65535 |   0    |    Y     |  N   |       | 构建号  |
|  13   | PARAM |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 参数  |
|  14   | DELETED |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 流水线已被软删除  |
|  15   | INSTANCE_ERROR_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 实例化错误信息  |
