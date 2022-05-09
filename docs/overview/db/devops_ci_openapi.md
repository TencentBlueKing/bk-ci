# 数据库设计文档

**数据库名：** devops_ci_openapi

**文档版本：** 1.0.0

**文档描述：** devops_ci_openapi的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_APP_CODE_GROUP](#T_APP_CODE_GROUP) | app_code对应的组织架构 |
| [T_APP_CODE_PROJECT](#T_APP_CODE_PROJECT) | app_code对应的蓝盾项目 |
| [T_APP_USER_INFO](#T_APP_USER_INFO) | app_code对应的管理员 |

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
