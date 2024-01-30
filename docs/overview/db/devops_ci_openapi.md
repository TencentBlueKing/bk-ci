# 数据库设计文档

**数据库名：** devops_ci_openapi

**文档版本：** 1.0.1

**文档描述：** devops_ci_openapi的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_APP_CODE_GROUP](#T_APP_CODE_GROUP) | app_code对应的组织架构 |
| [T_APP_CODE_PROJECT](#T_APP_CODE_PROJECT) | app_code对应的蓝盾项目 |
| [T_APP_USER_INFO](#T_APP_USER_INFO) | app_code对应的管理员 |
| [T_OPENAPI_METRICS_FOR_API](#T_OPENAPI_METRICS_FOR_API) | 接口维度度量表 |
| [T_OPENAPI_METRICS_FOR_PROJECT](#T_OPENAPI_METRICS_FOR_PROJECT) | 项目维度度量表 |

**表名：** <a id="T_APP_CODE_GROUP">T_APP_CODE_GROUP</a>

**说明：** app_code对应的组织架构

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | APP_CODE |   varchar   | 255 |   0    |    N     |  N   |       | APP编码  |
|  3   | BG_ID |   int   | 10 |   0    |    Y     |  N   |       | 事业群ID  |
|  4   | BG_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 事业群名称  |
|  5   | DEPT_ID |   int   | 10 |   0    |    Y     |  N   |       | 项目所属二级机构ID  |
|  6   | DEPT_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  7   | CENTER_ID |   int   | 10 |   0    |    Y     |  N   |       | 中心ID  |
|  8   | CENTER_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 中心名字  |
|  9   | CREATOR |   varchar   | 255 |   0    |    Y     |  N   |       | 创建者  |
|  10   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  11   | UPDATER |   varchar   | 255 |   0    |    Y     |  N   |       | 跟新人  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="T_APP_CODE_PROJECT">T_APP_CODE_PROJECT</a>

**说明：** app_code对应的蓝盾项目

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | APP_CODE |   varchar   | 255 |   0    |    N     |  N   |       | APP编码  |
|  3   | PROJECT_ID |   varchar   | 255 |   0    |    N     |  N   |       | 项目ID  |
|  4   | CREATOR |   varchar   | 255 |   0    |    Y     |  N   |       | 创建者  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |

**表名：** <a id="T_APP_USER_INFO">T_APP_USER_INFO</a>

**说明：** app_code对应的管理员

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | APP_CODE |   varchar   | 64 |   0    |    N     |  N   |       | APP编码  |
|  3   | MANAGER_ID |   varchar   | 64 |   0    |    N     |  N   |       | APP管理员ID  |
|  4   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |       | 是否删除  |
|  5   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 添加人员  |
|  6   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 添加时间  |

**表名：** <a id="T_OPENAPI_METRICS_FOR_API">T_OPENAPI_METRICS_FOR_API</a>

**说明：** 接口维度度量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | API |   varchar   | 64 |   0    |    N     |  N   |       | api接口代码  |
|  2   | KEY |   varchar   | 64 |   0    |    N     |  N   |       | APP编码/api请求用户  |
|  3   | SECOND_LEVEL_CONCURRENCY |   int   | 10 |   0    |    N     |  N   |       | 秒级并发量  |
|  4   | PEAK_CONCURRENCY |   int   | 10 |   0    |    N     |  N   |       | 峰值并发量  |
|  5   | CALL_5M |   int   | 10 |   0    |    N     |  N   |       | 5min调用量  |
|  6   | CALL_1H |   int   | 10 |   0    |    N     |  N   |       | 1h调用量  |
|  7   | CALL_24H |   int   | 10 |   0    |    N     |  N   |       | 24h调用量  |
|  8   | CALL_7D |   int   | 10 |   0    |    N     |  N   |       | 7d调用量  |

**表名：** <a id="T_OPENAPI_METRICS_FOR_PROJECT">T_OPENAPI_METRICS_FOR_PROJECT</a>

**说明：** 项目维度度量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT |   varchar   | 64 |   0    |    N     |  N   |       | 项目id  |
|  2   | API |   varchar   | 64 |   0    |    N     |  N   |       | api接口代码  |
|  3   | KEY |   varchar   | 64 |   0    |    N     |  N   |       | APP编码/api请求用户  |
|  4   | CALL_HISTORY |   int   | 10 |   0    |    N     |  N   |       | 历史累计调用  |
