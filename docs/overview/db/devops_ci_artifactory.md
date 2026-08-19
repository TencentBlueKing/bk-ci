# 数据库设计文档

**数据库名：** devops_ci_artifactory

**文档版本：** 1.0.24

**文档描述：** devops_ci_artifactory 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_FILE_INFO | 文件信息表 |
| T_FILE_PROPS_INFO | 文件元数据信息表 |
| T_FILE_TASK | 文件托管任务表 |
| T_PIPELINE_ARTIFACT_INFO | 流水线产出物基础元数据信息表 |
| T_TOKEN |  |

**表名：** <a>T_FILE_INFO</a>

**说明：** 文件信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    Y     |  N   |       | 用户组所属项目  |
|  3   | FILE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 文件类型  |
|  4   | FILE_PATH |   varchar   | 1024 |   0    |    Y     |  N   |       | 文件路径  |
|  5   | FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 文件名字  |
|  6   | FILE_SIZE |   bigint   | 20 |   0    |    N     |  N   |       | 文件大小  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_FILE_PROPS_INFO</a>

**说明：** 文件元数据信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROPS_KEY |   varchar   | 64 |   0    |    Y     |  N   |       | 属性字段 key  |
|  3   | PROPS_VALUE |   varchar   | 256 |   0    |    Y     |  N   |       | 属性字段 value  |
|  4   | FILE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 文件 ID  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_FILE_TASK</a>

**说明：** 文件托管任务表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | TASK_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 任务 ID  |
|  2   | FILE_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 文件类型  |
|  3   | FILE_PATH |   text   | 65535 |   0    |    Y     |  N   |       | 文件路径  |
|  4   | MACHINE_IP |   varchar   | 32 |   0    |    Y     |  N   |       | 机器 ip 地址  |
|  5   | LOCAL_PATH |   text   | 65535 |   0    |    Y     |  N   |       | 本地路径  |
|  6   | STATUS |   smallint   | 6 |   0    |    Y     |  N   |       | 状态  |
|  7   | USER_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 用户 ID  |
|  8   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目 ID  |
|  9   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线 ID  |
|  10   | BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建 ID  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a>T_PIPELINE_ARTIFACT_INFO</a>

**说明：** 流水线产出物基础元数据信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 蓝盾项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | PIPELINE_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 流水线名称  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建 ID  |
|  6   | BUILD_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 构建号  |
|  7   | STAGE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 阶段 ID  |
|  8   | CONTAINER_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建容器 ID  |
|  9   | TASK_ID |   varchar   | 34 |   0    |    N     |  N   |       | 任务 ID  |
|  10   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  N   |   1    | 执行次数  |
|  11   | ARTIFACT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 产出物类型：FILE/IMAGE/REPORT/PACKAGE 等  |
|  12   | ARTIFACT_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 产出物名称，如文件名、镜像名  |
|  13   | ARTIFACT_VERSION |   varchar   | 128 |   0    |    N     |  N   |       | 产出物版本，如镜像 Tag、包版本  |
|  14   | ARTIFACT_URI |   varchar   | 1024 |   0    |    Y     |  N   |       | 产出物唯一资源标识，如文件路径、镜像完整地址  |
|  15   | ARTIFACT_REPO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | 产出物仓库地址，如制品库地址、镜像 Registry  |
|  16   | ARTIFACT_DIGEST |   varchar   | 128 |   0    |    Y     |  N   |       | 产出物摘要，如 sha256、镜像 digest  |
|  17   | ARTIFACT_SIZE |   bigint   | 20 |   0    |    Y     |  N   |       | 产出物大小，单位字节  |
|  18   | CODE_REPO_URL |   varchar   | 512 |   0    |    Y     |  N   |       | 代码库地址  |
|  19   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交 ID  |
|  20   | EXTRA_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 扩展元数据，JSON 格式  |
|  21   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  22   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改人  |
|  23   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  24   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_TOKEN</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  4   | ARTIFACTORY_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 归档仓库类型  |
|  5   | PATH |   text   | 65535 |   0    |    N     |  N   |       | 路径  |
|  6   | TOKEN |   varchar   | 64 |   0    |    N     |  N   |       | TOKEN  |
|  7   | EXPIRE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 过期时间  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
