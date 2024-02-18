# 数据库设计文档

**数据库名：** devops_ci_environment

**文档版本：** 1.0.1

**文档描述：** devops_ci_environment的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_AGENT_FAILURE_NOTIFY_USER](#T_AGENT_FAILURE_NOTIFY_USER) |  |
| [T_AGENT_PIPELINE_REF](#T_AGENT_PIPELINE_REF) |  |
| [T_AGENT_SHARE_PROJECT](#T_AGENT_SHARE_PROJECT) |  |
| [T_ENV](#T_ENV) | 环境信息表 |
| [T_ENVIRONMENT_AGENT_PIPELINE](#T_ENVIRONMENT_AGENT_PIPELINE) |  |
| [T_ENVIRONMENT_SLAVE_GATEWAY](#T_ENVIRONMENT_SLAVE_GATEWAY) |  |
| [T_ENVIRONMENT_THIRDPARTY_AGENT](#T_ENVIRONMENT_THIRDPARTY_AGENT) | 第三方构建机agent信息表 |
| [T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION](#T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION) |  |
| [T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS](#T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS) |  |
| [T_ENV_NODE](#T_ENV_NODE) | 环境-节点映射表 |
| [T_ENV_SHARE_PROJECT](#T_ENV_SHARE_PROJECT) |  |
| [T_NODE](#T_NODE) | 节点信息表 |
| [T_PROJECT_CONFIG](#T_PROJECT_CONFIG) |  |

**表名：** <a id="T_AGENT_FAILURE_NOTIFY_USER">T_AGENT_FAILURE_NOTIFY_USER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 用户ID  |
|  3   | NOTIFY_TYPES |   varchar   | 32 |   0    |    Y     |  N   |       | 通知类型  |

**表名：** <a id="T_AGENT_PIPELINE_REF">T_AGENT_PIPELINE_REF</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NODE_ID |   bigint   | 20 |   0    |    N     |  N   |       | 节点ID  |
|  3   | AGENT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 构建机ID  |
|  4   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  5   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  6   | PIEPLINE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 流水线名称  |
|  7   | VM_SEQ_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建序列号  |
|  8   | JOB_ID |   varchar   | 34 |   0    |    Y     |  N   |       | JOBID  |
|  9   | JOB_NAME |   varchar   | 255 |   0    |    N     |  N   |       | JOBNAME  |
|  10   | LAST_BUILD_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 最近构建时间  |

**表名：** <a id="T_AGENT_SHARE_PROJECT">T_AGENT_SHARE_PROJECT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | AGENT_ID |   bigint   | 20 |   0    |    N     |  Y   |       | AgentID  |
|  2   | MAIN_PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主项目ID  |
|  3   | SHARED_PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 共享的目标项目ID  |
|  4   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  5   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_ENV">T_ENV</a>

**说明：** 环境信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ENV_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | ENV_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 环境名称  |
|  4   | ENV_DESC |   varchar   | 128 |   0    |    N     |  N   |       | 环境描述  |
|  5   | ENV_TYPE |   varchar   | 128 |   0    |    N     |  N   |       | 环境类型（开发环境{DEV}|测试环境{TEST}|构建环境{BUILD}）  |
|  6   | ENV_VARS |   text   | 65535 |   0    |    N     |  N   |       | 环境变量  |
|  7   | CREATED_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建人  |
|  8   | UPDATED_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |
|  9   | CREATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  10   | UPDATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 修改时间  |
|  11   | ENV_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 环境哈希ID  |
|  12   | IS_DELETED |   bit   | 1 |   0    |    N     |  N   |       | 是否删除  |

**表名：** <a id="T_ENVIRONMENT_AGENT_PIPELINE">T_ENVIRONMENT_AGENT_PIPELINE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | AGENT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 构建机ID  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  4   | USER_ID |   varchar   | 32 |   0    |    N     |  N   |       | 用户ID  |
|  5   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  7   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  8   | PIPELINE |   varchar   | 1024 |   0    |    N     |  N   |       | PipelineType  |
|  9   | RESPONSE |   text   | 65535 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_ENVIRONMENT_SLAVE_GATEWAY">T_ENVIRONMENT_SLAVE_GATEWAY</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 32 |   0    |    N     |  N   |       | 名称  |
|  3   | SHOW_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 展示名称  |
|  4   | GATEWAY |   varchar   | 127 |   0    |    Y     |  N   |       | 网关地址  |
|  5   | FILE_GATEWAY |   varchar   | 127 |   0    |    Y     |  N   |       | 文件网关地址  |
|  6   | VISIBILITY |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否在界面可见  |

**表名：** <a id="T_ENVIRONMENT_THIRDPARTY_AGENT">T_ENVIRONMENT_THIRDPARTY_AGENT</a>

**说明：** 第三方构建机agent信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NODE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 节点ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | HOSTNAME |   varchar   | 128 |   0    |    Y     |  N   |       | 主机名称  |
|  5   | IP |   varchar   | 64 |   0    |    Y     |  N   |       | ip地址  |
|  6   | OS |   varchar   | 16 |   0    |    N     |  N   |       | 操作系统  |
|  7   | DETECT_OS |   varchar   | 128 |   0    |    Y     |  N   |       | 检测操作系统  |
|  8   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  9   | SECRET_KEY |   varchar   | 256 |   0    |    N     |  N   |       | 密钥  |
|  10   | CREATED_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  11   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | START_REMOTE_IP |   varchar   | 64 |   0    |    Y     |  N   |       | 主机IP  |
|  13   | GATEWAY |   varchar   | 256 |   0    |    Y     |  N   |       | 目标服务网关  |
|  14   | VERSION |   varchar   | 128 |   0    |    Y     |  N   |       | 版本号  |
|  15   | MASTER_VERSION |   varchar   | 128 |   0    |    Y     |  N   |       | 主版本  |
|  16   | PARALLEL_TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 并行任务计数  |
|  17   | AGENT_INSTALL_PATH |   varchar   | 512 |   0    |    Y     |  N   |       | 构建机安装路径  |
|  18   | STARTED_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 启动者  |
|  19   | AGENT_ENVS |   text   | 65535 |   0    |    Y     |  N   |       | 环境变量  |
|  20   | FILE_GATEWAY |   varchar   | 256 |   0    |    Y     |  N   |       | 文件网关路径  |
|  21   | AGENT_PROPS |   text   | 65535 |   0    |    Y     |  N   |       | agentconfig配置项Json  |
|  22   | DOCKER_PARALLEL_TASK_COUNT |   int   | 10 |   0    |    Y     |  N   |       | Docker构建机并行任务计数  |

**表名：** <a id="T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION">T_ENVIRONMENT_THIRDPARTY_AGENT_ACTION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | AGENT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 构建机ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | ACTION |   varchar   | 64 |   0    |    N     |  N   |       | 操作  |
|  5   | ACTION_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 操作时间  |

**表名：** <a id="T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS">T_ENVIRONMENT_THIRDPARTY_ENABLE_PROJECTS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | ENALBE |   bit   | 1 |   0    |    Y     |  N   |       | 是否启用  |
|  3   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  4   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_ENV_NODE">T_ENV_NODE</a>

**说明：** 环境-节点映射表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ENV_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 环境ID  |
|  2   | NODE_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 节点ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |

**表名：** <a id="T_ENV_SHARE_PROJECT">T_ENV_SHARE_PROJECT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ENV_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 环境ID  |
|  2   | ENV_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 环境名称  |
|  3   | MAIN_PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主项目ID  |
|  4   | SHARED_PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 共享的目标项目ID  |
|  5   | SHARED_PROJECT_NAME |   varchar   | 1024 |   0    |    Y     |  N   |       | 目标项目名称  |
|  6   | TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 类型  |
|  7   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_NODE">T_NODE</a>

**说明：** 节点信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | NODE_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 节点ID主键ID  |
|  2   | NODE_STRING_ID |   varchar   | 255 |   0    |    Y     |  N   |       | 节点ID字符串  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | NODE_IP |   varchar   | 64 |   0    |    N     |  N   |       | 节点IP  |
|  5   | NODE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 节点名称  |
|  6   | NODE_STATUS |   varchar   | 64 |   0    |    N     |  N   |       | 节点状态  |
|  7   | NODE_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 节点类型  |
|  8   | NODE_CLUSTER_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 集群ID  |
|  9   | NODE_NAMESPACE |   varchar   | 128 |   0    |    Y     |  N   |       | 节点命名空间  |
|  10   | CREATED_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  11   | CREATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  12   | EXPIRE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 过期时间  |
|  13   | OS_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 操作系统名称  |
|  14   | OPERATOR |   varchar   | 256 |   0    |    Y     |  N   |       | 操作者  |
|  15   | BAK_OPERATOR |   varchar   | 256 |   0    |    Y     |  N   |       | 备份责任人  |
|  16   | AGENT_STATUS |   bit   | 1 |   0    |    Y     |  N   |       | 构建机状态  |
|  17   | DISPLAY_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 别名  |
|  18   | IMAGE |   varchar   | 512 |   0    |    Y     |  N   |       | 镜像  |
|  19   | TASK_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 任务id  |
|  20   | LAST_MODIFY_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 最近修改时间  |
|  21   | LAST_MODIFY_USER |   varchar   | 512 |   0    |    Y     |  N   |       | 最近修改者  |
|  22   | BIZ_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 所属业务  |
|  23   | NODE_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 节点哈希ID  |
|  24   | PIPELINE_REF_COUNT |   int   | 10 |   0    |    N     |  N   |   0    | 流水线Job引用数  |
|  25   | LAST_BUILD_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 最近构建时间  |

**表名：** <a id="T_PROJECT_CONFIG">T_PROJECT_CONFIG</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | UPDATED_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改者  |
|  3   | UPDATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 修改时间  |
|  4   | BCSVM_ENALBED |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
|  5   | BCSVM_QUOTA |   int   | 10 |   0    |    N     |  N   |   0    |   |
|  6   | IMPORT_QUOTA |   int   | 10 |   0    |    N     |  N   |   30    |   |
|  7   | DEV_CLOUD_ENALBED |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
|  8   | DEV_CLOUD_QUOTA |   int   | 10 |   0    |    N     |  N   |   0    |   |
