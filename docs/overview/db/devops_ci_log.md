# 数据库设计文档

**数据库名：** devops_ci_log

**文档版本：** 1.0.9

**文档描述：** devops_ci_log 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_LOG_INDICES_V2 | 构建日志已关联 ES 索引表 |
| T_LOG_STATUS | 构建日志打印状态表 |
| T_LOG_SUBTAGS | 构建日志子标签表 |

**表名：** <a>T_LOG_INDICES_V2</a>

**说明：** 构建日志已关联 ES 索引表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建 ID  |
|  3   | INDEX_NAME |   varchar   | 20 |   0    |    N     |  N   |       |   |
|  4   | LAST_LINE_NUM |   bigint   | 20 |   0    |    N     |  N   |   1    | 最后行号  |
|  5   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-11-1100:00:00    | 修改时间  |
|  7   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | buildisenablev2ornot  |
|  8   | LOG_CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | multieslogclustername  |
|  9   | USE_CLUSTER |   bit   | 1 |   0    |    N     |  N   |   b'0'    | usemultieslogclusterornot  |

**表名：** <a>T_LOG_STATUS</a>

**说明：** 构建日志打印状态表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建 ID  |
|  3   | TAG |   varchar   | 64 |   0    |    Y     |  N   |       | 标签  |
|  4   | SUB_TAG |   varchar   | 256 |   0    |    Y     |  N   |       | 子标签  |
|  5   | JOB_ID |   varchar   | 64 |   0    |    Y     |  N   |       | JOBID  |
|  6   | USER_JOB_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 真正的 jobId，已经存在的 JOB_ID 字段其实是 containerhashid  |
|  7   | STEP_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 用户填写的插件 id  |
|  8   | MODE |   varchar   | 32 |   0    |    Y     |  N   |       | LogStorageMode  |
|  9   | EXECUTE_COUNT |   int   | 10 |   0    |    N     |  N   |       | 执行次数  |
|  10   | FINISHED |   bit   | 1 |   0    |    N     |  N   |   b'0'    | buildisfinishedornot  |
|  11   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_LOG_SUBTAGS</a>

**说明：** 构建日志子标签表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建 ID  |
|  3   | TAG |   varchar   | 64 |   0    |    N     |  N   |       | 插件标签  |
|  4   | SUB_TAGS |   text   | 65535 |   0    |    N     |  N   |       | 插件子标签  |
|  5   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
