# 数据库设计文档

**数据库名：** devops_ci_dispatch

**文档版本：** 1.0.1

**文档描述：** devops_ci_dispatch的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_DISPATCH_KUBERNETES_BUILD](#T_DISPATCH_KUBERNETES_BUILD) | dispatch-kubernetes流水线构建机信息 |
| [T_DISPATCH_KUBERNETES_BUILD_CONTAINER_POOL_NO](#T_DISPATCH_KUBERNETES_BUILD_CONTAINER_POOL_NO) | buildId和containerName,poolNo的映射关系 |
| [T_DISPATCH_KUBERNETES_BUILD_HIS](#T_DISPATCH_KUBERNETES_BUILD_HIS) | dispatchkubernetes构建历史记录 |
| [T_DISPATCH_PIPELINE_BUILD](#T_DISPATCH_PIPELINE_BUILD) |  |
| [T_DISPATCH_PIPELINE_DOCKER_BUILD](#T_DISPATCH_PIPELINE_DOCKER_BUILD) |  |
| [T_DISPATCH_PIPELINE_DOCKER_DEBUG](#T_DISPATCH_PIPELINE_DOCKER_DEBUG) |  |
| [T_DISPATCH_PIPELINE_DOCKER_ENABLE](#T_DISPATCH_PIPELINE_DOCKER_ENABLE) |  |
| [T_DISPATCH_PIPELINE_DOCKER_HOST](#T_DISPATCH_PIPELINE_DOCKER_HOST) |  |
| [T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE](#T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE) |  |
| [T_DISPATCH_PIPELINE_DOCKER_IP_INFO](#T_DISPATCH_PIPELINE_DOCKER_IP_INFO) | DOCKER构建机负载表 |
| [T_DISPATCH_PIPELINE_DOCKER_POOL](#T_DISPATCH_PIPELINE_DOCKER_POOL) | DOCKER并发构建池状态表 |
| [T_DISPATCH_PIPELINE_DOCKER_TASK](#T_DISPATCH_PIPELINE_DOCKER_TASK) |  |
| [T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT](#T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT) | DOCKER构建任务漂移记录表 |
| [T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE](#T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE) | DOCKER构建任务表 |
| [T_DISPATCH_PROJECT_RUN_TIME](#T_DISPATCH_PROJECT_RUN_TIME) | 项目当月已使用额度 |
| [T_DISPATCH_QUOTA_JOB_SYSTEM](#T_DISPATCH_QUOTA_JOB_SYSTEM) | 流水线JOB配额系统表 |
| [T_DISPATCH_QUOTA_PROJECT](#T_DISPATCH_QUOTA_PROJECT) | 项目配额 |
| [T_DISPATCH_QUOTA_SYSTEM](#T_DISPATCH_QUOTA_SYSTEM) | 系统配额 |
| [T_DISPATCH_RUNNING_JOBS](#T_DISPATCH_RUNNING_JOBS) | 运行中的JOB |
| [T_DISPATCH_THIRDPARTY_AGENT_BUILD](#T_DISPATCH_THIRDPARTY_AGENT_BUILD) |  |
| [T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG](#T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG) | 第三方构建机Docker登录调试 |
| [T_DOCKER_RESOURCE_OPTIONS](#T_DOCKER_RESOURCE_OPTIONS) | docker基础配额表 |

**表名：** <a id="T_DISPATCH_KUBERNETES_BUILD">T_DISPATCH_KUBERNETES_BUILD</a>

**说明：** dispatch-kubernetes流水线构建机信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  Y   |       |   |
|  2   | VM_SEQ_ID |   varchar   | 34 |   0    |    N     |  Y   |       |   |
|  3   | POOL_NO |   int   | 10 |   0    |    N     |  Y   |       |   |
|  4   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  5   | CONTAINER_NAME |   varchar   | 128 |   0    |    N     |  N   |       |   |
|  6   | IMAGES |   varchar   | 1024 |   0    |    N     |  N   |       |   |
|  7   | STATUS |   int   | 10 |   0    |    N     |  N   |       |   |
|  8   | CREATED_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       |   |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       |   |
|  10   | USER_ID |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  11   | DEBUG_STATUS |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否处于debug状态  |
|  12   | DEBUG_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | debug时间  |
|  13   | CPU |   int   | 10 |   0    |    Y     |  N   |   16    | CPU  |
|  14   | MEMORY |   varchar   | 64 |   0    |    Y     |  N   |   32768M    | 内存  |
|  15   | DISK |   varchar   | 64 |   0    |    Y     |  N   |   100G    | 磁盘  |

**表名：** <a id="T_DISPATCH_KUBERNETES_BUILD_CONTAINER_POOL_NO">T_DISPATCH_KUBERNETES_BUILD_CONTAINER_POOL_NO</a>

**说明：** buildId和containerName,poolNo的映射关系

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUILD_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 构建ID  |
|  2   | VM_SEQ_ID |   varchar   | 64 |   0    |    N     |  Y   |       | VmSeqID  |
|  3   | CONTAINER_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 容器名称  |
|  4   | POOL_NO |   varchar   | 128 |   0    |    Y     |  N   |       | 构建机池编号  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  Y   |   1    | 流水线重试次数  |

**表名：** <a id="T_DISPATCH_KUBERNETES_BUILD_HIS">T_DISPATCH_KUBERNETES_BUILD_HIS</a>

**说明：** dispatchkubernetes构建历史记录

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | pipelineid  |
|  3   | BUIDLD_ID |   varchar   | 64 |   0    |    N     |  N   |       | buildid  |
|  4   | VM_SEQ_ID |   varchar   | 64 |   0    |    N     |  N   |       | vmseqid  |
|  5   | CONTAINER_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 容器名称  |
|  6   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | GMT_MODIFIED |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  8   | CPU |   int   | 10 |   0    |    Y     |  N   |   16    | CPU  |
|  9   | MEMORY |   varchar   | 64 |   0    |    Y     |  N   |   32768M    | 内存  |
|  10   | DISK |   varchar   | 64 |   0    |    Y     |  N   |   100G    | 磁盘  |
|  11   | SECRET_KEY |   varchar   | 64 |   0    |    Y     |  N   |       | 构建密钥  |
|  12   | POOL_NO |   varchar   | 64 |   0    |    Y     |  N   |       | 并发构建池  |
|  13   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   1    | 流水线重试次数  |

**表名：** <a id="T_DISPATCH_PIPELINE_BUILD">T_DISPATCH_PIPELINE_BUILD</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  5   | VM_SEQ_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建序列号  |
|  6   | VM_ID |   bigint   | 20 |   0    |    N     |  N   |       | 虚拟机ID  |
|  7   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_BUILD">T_DISPATCH_PIPELINE_DOCKER_BUILD</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建ID  |
|  3   | VM_SEQ_ID |   int   | 10 |   0    |    N     |  N   |       | 构建序列号  |
|  4   | SECRET_KEY |   varchar   | 64 |   0    |    N     |  N   |       | 密钥  |
|  5   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  6   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | ZONE |   varchar   | 128 |   0    |    Y     |  N   |       | 构建机地域  |
|  9   | PROJECT_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 项目ID  |
|  10   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线ID  |
|  11   | DISPATCH_MESSAGE |   varchar   | 4096 |   0    |    Y     |  N   |       | 发送信息  |
|  12   | STARTUP_MESSAGE |   text   | 65535 |   0    |    Y     |  N   |       | 启动信息  |
|  13   | ROUTE_KEY |   varchar   | 64 |   0    |    Y     |  N   |       | 消息队列的路由KEY  |
|  14   | DOCKER_INST_ID |   bigint   | 20 |   0    |    Y     |  N   |       |   |
|  15   | VERSION_ID |   int   | 10 |   0    |    Y     |  N   |       | 版本ID  |
|  16   | TEMPLATE_ID |   int   | 10 |   0    |    Y     |  N   |       | 模板ID  |
|  17   | NAMESPACE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 命名空间ID  |
|  18   | DOCKER_IP |   varchar   | 64 |   0    |    Y     |  N   |       | 构建机IP  |
|  19   | CONTAINER_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 构建容器ID  |
|  20   | POOL_NO |   int   | 10 |   0    |    Y     |  N   |   0    | 构建容器池序号  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_DEBUG">T_DISPATCH_PIPELINE_DOCKER_DEBUG</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | VM_SEQ_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建序列号  |
|  5   | POOL_NO |   int   | 10 |   0    |    N     |  N   |   0    | 构建池序号  |
|  6   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  7   | TOKEN |   varchar   | 128 |   0    |    Y     |  N   |       | TOKEN  |
|  8   | IMAGE_NAME |   varchar   | 1024 |   0    |    N     |  N   |       | 镜像名称  |
|  9   | HOST_TAG |   varchar   | 128 |   0    |    Y     |  N   |       | 主机标签  |
|  10   | CONTAINER_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 构建容器ID  |
|  11   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 修改时间  |
|  13   | ZONE |   varchar   | 128 |   0    |    Y     |  N   |       | 构建机地域  |
|  14   | BUILD_ENV |   varchar   | 4096 |   0    |    Y     |  N   |       | 构建机环境变量  |
|  15   | REGISTRY_USER |   varchar   | 128 |   0    |    Y     |  N   |       | 注册用户名  |
|  16   | REGISTRY_PWD |   varchar   | 128 |   0    |    Y     |  N   |       | 注册用户密码  |
|  17   | IMAGE_TYPE |   varchar   | 128 |   0    |    Y     |  N   |       | 镜像类型  |
|  18   | IMAGE_PUBLIC_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 镜像是否为公共镜像：0否1是  |
|  19   | IMAGE_RD_TYPE |   bit   | 1 |   0    |    Y     |  N   |       | 镜像研发来源：0自研1第三方  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_ENABLE">T_DISPATCH_PIPELINE_DOCKER_ENABLE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 流水线ID  |
|  2   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否启用  |
|  3   | VM_SEQ_ID |   int   | 10 |   0    |    N     |  Y   |   -1    | 构建序列号  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_HOST">T_DISPATCH_PIPELINE_DOCKER_HOST</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_CODE |   varchar   | 128 |   0    |    N     |  Y   |       | 用户组所属项目  |
|  2   | HOST_IP |   varchar   | 128 |   0    |    N     |  Y   |       | 主机ip  |
|  3   | REMARK |   varchar   | 1024 |   0    |    Y     |  N   |       | 评论  |
|  4   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  5   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  6   | TYPE |   int   | 10 |   0    |    N     |  N   |   0    | 类型  |
|  7   | ROUTE_KEY |   varchar   | 45 |   0    |    Y     |  N   |       | 消息队列的路由KEY  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE">T_DISPATCH_PIPELINE_DOCKER_HOST_ZONE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | HOST_IP |   varchar   | 128 |   0    |    N     |  Y   |       | 主机ip  |
|  2   | ZONE |   varchar   | 128 |   0    |    N     |  N   |       | 构建机地域  |
|  3   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |   1    | 是否启用  |
|  4   | REMARK |   varchar   | 1024 |   0    |    Y     |  N   |       | 评论  |
|  5   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  7   | TYPE |   int   | 10 |   0    |    N     |  N   |   0    | 类型  |
|  8   | ROUTE_KEY |   varchar   | 45 |   0    |    Y     |  N   |       | 消息队列的路由KEY  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_IP_INFO">T_DISPATCH_PIPELINE_DOCKER_IP_INFO</a>

**说明：** DOCKER构建机负载表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键  |
|  2   | DOCKER_IP |   varchar   | 64 |   0    |    N     |  N   |       | DOCKERIP  |
|  3   | DOCKER_HOST_PORT |   int   | 10 |   0    |    N     |  N   |   80    | DOCKERPORT  |
|  4   | CAPACITY |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器总容量  |
|  5   | USED_NUM |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器已使用容量  |
|  6   | CPU_LOAD |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器CPU负载  |
|  7   | MEM_LOAD |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器MEM负载  |
|  8   | DISK_LOAD |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器DISK负载  |
|  9   | DISK_IO_LOAD |   int   | 10 |   0    |    N     |  N   |   0    | 节点容器DISKIO负载  |
|  10   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 节点是否可用  |
|  11   | SPECIAL_ON |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 节点是否作为专用机  |
|  12   | GRAY_ENV |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否为灰度节点  |
|  13   | CLUSTER_NAME |   varchar   | 64 |   0    |    Y     |  N   |   COMMON    | 构建集群类型  |
|  14   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  15   | GMT_MODIFIED |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_POOL">T_DISPATCH_PIPELINE_DOCKER_POOL</a>

**说明：** DOCKER并发构建池状态表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | VM_SEQ |   varchar   | 64 |   0    |    N     |  N   |       | 构建机序号  |
|  4   | POOL_NO |   int   | 10 |   0    |    N     |  N   |   0    | 构建池序号  |
|  5   | STATUS |   int   | 10 |   0    |    N     |  N   |   0    | 构建池状态  |
|  6   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | GMT_MODIFIED |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_TASK">T_DISPATCH_PIPELINE_DOCKER_TASK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | AGENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建机ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  6   | VM_SEQ_ID |   int   | 10 |   0    |    N     |  N   |       | 构建序列号  |
|  7   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  8   | SECRET_KEY |   varchar   | 128 |   0    |    N     |  N   |       | 密钥  |
|  9   | IMAGE_NAME |   varchar   | 1024 |   0    |    N     |  N   |       | 镜像名称  |
|  10   | CHANNEL_CODE |   varchar   | 128 |   0    |    Y     |  N   |       | 渠道号，默认为DS  |
|  11   | HOST_TAG |   varchar   | 128 |   0    |    Y     |  N   |       | 主机标签  |
|  12   | CONTAINER_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 构建容器ID  |
|  13   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  14   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  15   | ZONE |   varchar   | 128 |   0    |    Y     |  N   |       | 构建机地域  |
|  16   | REGISTRY_USER |   varchar   | 128 |   0    |    Y     |  N   |       | 注册用户名  |
|  17   | REGISTRY_PWD |   varchar   | 128 |   0    |    Y     |  N   |       | 注册用户密码  |
|  18   | IMAGE_TYPE |   varchar   | 128 |   0    |    Y     |  N   |       | 镜像类型  |
|  19   | CONTAINER_HASH_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 构建Job唯一标识  |
|  20   | IMAGE_PUBLIC_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 镜像是否为公共镜像：0否1是  |
|  21   | IMAGE_RD_TYPE |   bit   | 1 |   0    |    Y     |  N   |       | 镜像研发来源：0自研1第三方  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT">T_DISPATCH_PIPELINE_DOCKER_TASK_DRIFT</a>

**说明：** DOCKER构建任务漂移记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建ID  |
|  4   | VM_SEQ |   varchar   | 64 |   0    |    N     |  N   |       | 构建机序号  |
|  5   | OLD_DOCKER_IP |   varchar   | 64 |   0    |    N     |  N   |       | 旧构建容器IP  |
|  6   | NEW_DOCKER_IP |   varchar   | 64 |   0    |    N     |  N   |       | 新构建容器IP  |
|  7   | OLD_DOCKER_IP_INFO |   varchar   | 1024 |   0    |    N     |  N   |       | 旧容器IP负载  |
|  8   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | GMT_MODIFIED |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE">T_DISPATCH_PIPELINE_DOCKER_TASK_SIMPLE</a>

**说明：** DOCKER构建任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | VM_SEQ |   varchar   | 64 |   0    |    N     |  N   |       | 构建机序号  |
|  4   | DOCKER_IP |   varchar   | 64 |   0    |    N     |  N   |       | 构建容器IP  |
|  5   | DOCKER_RESOURCE_OPTION |   int   | 10 |   0    |    N     |  N   |   0    | 构建资源配置  |
|  6   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | GMT_MODIFIED |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_DISPATCH_PROJECT_RUN_TIME">T_DISPATCH_PROJECT_RUN_TIME</a>

**说明：** 项目当月已使用额度

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | VM_TYPE |   varchar   | 128 |   0    |    N     |  Y   |       | VM类型  |
|  3   | RUN_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 运行时长  |
|  4   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_DISPATCH_QUOTA_JOB_SYSTEM">T_DISPATCH_QUOTA_JOB_SYSTEM</a>

**说明：** 流水线JOB配额系统表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 自增ID  |
|  2   | VM_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 构建机类型  |
|  3   | CHANNEL_CODE |   varchar   | 128 |   0    |    N     |  N   |       | 构建来源，包含：BS,CODECC,AM,GIT等  |
|  4   | RUNNING_JOBS_MAX_SYSTEM |   int   | 10 |   0    |    N     |  N   |   1000    | 系统最大并发JOB数  |
|  5   | RUNNING_JOBS_MAX_PROJECT |   int   | 10 |   0    |    N     |  N   |   100    | 单项目默认最大并发JOB数  |
|  6   | RUNNING_TIME_JOB_MAX |   int   | 10 |   0    |    N     |  N   |   24    | 系统默认所有单个JOB最大执行时间  |
|  7   | RUNNING_TIME_JOB_MAX_PROJECT |   int   | 10 |   0    |    N     |  N   |   1000    | 默认单项目所有JOB最大执行时间  |
|  8   | PROJECT_RUNNING_JOB_THRESHOLD |   int   | 10 |   0    |    N     |  N   |   80    | 项目执行job数量告警阈值  |
|  9   | PROJECT_RUNNING_TIME_THRESHOLD |   int   | 10 |   0    |    N     |  N   |   80    | 项目执行job时间告警阈值  |
|  10   | SYSTEM_RUNNING_JOB_THRESHOLD |   int   | 10 |   0    |    N     |  N   |   80    | 系统执行job数量告警阈值  |
|  11   | OPERATOR |   varchar   | 128 |   0    |    N     |  N   |       | 操作人  |
|  12   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_DISPATCH_QUOTA_PROJECT">T_DISPATCH_QUOTA_PROJECT</a>

**说明：** 项目配额

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | VM_TYPE |   varchar   | 128 |   0    |    N     |  Y   |       | VM类型  |
|  3   | CHANNEL_CODE |   varchar   | 128 |   0    |    N     |  Y   |   BS    | 构建来源，包含：BS,CODECC,AM,GIT等  |
|  4   | RUNNING_JOBS_MAX |   int   | 10 |   0    |    N     |  N   |       | 项目最大并发JOB数  |
|  5   | RUNNING_TIME_JOB_MAX |   int   | 10 |   0    |    N     |  N   |       | 项目单JOB最大执行时间  |
|  6   | RUNNING_TIME_PROJECT_MAX |   int   | 10 |   0    |    N     |  N   |       | 项目所有JOB最大执行时间  |
|  7   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | OPERATOR |   varchar   | 128 |   0    |    N     |  N   |       | 操作人  |

**表名：** <a id="T_DISPATCH_QUOTA_SYSTEM">T_DISPATCH_QUOTA_SYSTEM</a>

**说明：** 系统配额

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | VM_TYPE |   varchar   | 128 |   0    |    N     |  Y   |       | 构建机类型  |
|  2   | RUNNING_JOBS_MAX_SYSTEM |   int   | 10 |   0    |    N     |  N   |       | 蓝盾系统最大并发JOB数  |
|  3   | RUNNING_JOBS_MAX_PROJECT |   int   | 10 |   0    |    N     |  N   |       | 单项目默认最大并发JOB数  |
|  4   | RUNNING_TIME_JOB_MAX |   int   | 10 |   0    |    N     |  N   |       | 系统默认所有单个JOB最大执行时间  |
|  5   | RUNNING_TIME_JOB_MAX_PROJECT |   int   | 10 |   0    |    N     |  N   |       | 默认单项目所有JOB最大执行时间  |
|  6   | RUNNING_JOBS_MAX_GITCI_SYSTEM |   int   | 10 |   0    |    N     |  N   |       | 工蜂CI系统总最大并发JOB数量  |
|  7   | RUNNING_JOBS_MAX_GITCI_PROJECT |   int   | 10 |   0    |    N     |  N   |       | 工蜂CI单项目最大并发JOB数量  |
|  8   | RUNNING_TIME_JOB_MAX_GITCI |   int   | 10 |   0    |    N     |  N   |       | 工蜂CI单JOB最大执行时间  |
|  9   | RUNNING_TIME_JOB_MAX_PROJECT_GITCI |   int   | 10 |   0    |    N     |  N   |       | 工蜂CI单项目最大执行时间  |
|  10   | PROJECT_RUNNING_JOB_THRESHOLD |   int   | 10 |   0    |    N     |  N   |       | 项目执行job数量告警阈值  |
|  11   | PROJECT_RUNNING_TIME_THRESHOLD |   int   | 10 |   0    |    N     |  N   |       | 项目执行job时间告警阈值  |
|  12   | SYSTEM_RUNNING_JOB_THRESHOLD |   int   | 10 |   0    |    N     |  N   |       | 系统执行job数量告警阈值  |
|  13   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  14   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  15   | OPERATOR |   varchar   | 128 |   0    |    N     |  N   |       | 操作人  |

**表名：** <a id="T_DISPATCH_RUNNING_JOBS">T_DISPATCH_RUNNING_JOBS</a>

**说明：** 运行中的JOB

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 128 |   0    |    N     |  N   |       | 项目ID  |
|  3   | VM_TYPE |   varchar   | 128 |   0    |    N     |  N   |       | VM类型  |
|  4   | CHANNEL_CODE |   varchar   | 128 |   0    |    N     |  N   |   BS    | 构建来源，包含：BS,CODECC,AM,GIT等  |
|  5   | BUILD_ID |   varchar   | 128 |   0    |    N     |  N   |       | 构建ID  |
|  6   | VM_SEQ_ID |   varchar   | 128 |   0    |    N     |  N   |       | 构建序列号  |
|  7   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  N   |       | 执行次数  |
|  8   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  9   | AGENT_START_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 构建机启动时间  |

**表名：** <a id="T_DISPATCH_THIRDPARTY_AGENT_BUILD">T_DISPATCH_THIRDPARTY_AGENT_BUILD</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | AGENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建机ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  6   | VM_SEQ_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建序列号  |
|  7   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  8   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  9   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  10   | WORKSPACE |   varchar   | 4096 |   0    |    Y     |  N   |       | 工作空间  |
|  11   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建次数  |
|  12   | PIPELINE_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 流水线名称  |
|  13   | TASK_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 任务名称  |
|  14   | AGENT_IP |   varchar   | 128 |   0    |    Y     |  N   |       | 节点IP  |
|  15   | NODE_ID |   bigint   | 20 |   0    |    Y     |  N   |   0    | 第三方构建机NODE_ID  |
|  16   | DOCKER_INFO |   json   | 1073741824 |   0    |    Y     |  N   |       | 第三方构建机docker构建信息  |
|  17   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 流水线执行次数  |
|  18   | CONTAINER_HASH_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 容器ID日志使用  |

**表名：** <a id="T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG">T_DISPATCH_THIRDPARTY_AGENT_DOCKER_DEBUG</a>

**说明：** 第三方构建机Docker登录调试

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | AGENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建机ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  6   | VM_SEQ_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建序列号  |
|  7   | USER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 调试用户  |
|  8   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 状态  |
|  9   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 修改时间  |
|  11   | WORKSPACE |   varchar   | 4096 |   0    |    Y     |  N   |       | 工作空间  |
|  12   | DOCKER_INFO |   json   | 1073741824 |   0    |    Y     |  N   |       | 第三方构建机docker构建信息  |
|  13   | ERR_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 启动构建时的错误信息  |
|  14   | DEBUG_URL |   varchar   | 4096 |   0    |    Y     |  N   |       | debug链接  |

**表名：** <a id="T_DOCKER_RESOURCE_OPTIONS">T_DOCKER_RESOURCE_OPTIONS</a>

**说明：** docker基础配额表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键  |
|  2   | CPU_PERIOD |   int   | 10 |   0    |    N     |  N   |   10000    | CPU配置  |
|  3   | CPU_QUOTA |   int   | 10 |   0    |    N     |  N   |   160000    | CPU配置  |
|  4   | MEMORY_LIMIT_BYTES |   bigint   | 20 |   0    |    N     |  N   |   34359738368    | 内存：32G  |
|  5   | DISK |   int   | 10 |   0    |    N     |  N   |   100    | 磁盘：100G  |
|  6   | BLKIO_DEVICE_WRITE_BPS |   bigint   | 20 |   0    |    N     |  N   |   125829120    | 磁盘写入速率，120m/s  |
|  7   | BLKIO_DEVICE_READ_BPS |   bigint   | 20 |   0    |    N     |  N   |   125829120    | 磁盘读入速率，120m/s  |
|  8   | DESCRIPTION |   varchar   | 128 |   0    |    N     |  N   |       | 描述  |
|  9   | GMT_CREATE |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | GMT_MODIFIED |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
