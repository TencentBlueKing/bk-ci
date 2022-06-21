# 数据库设计文档

**数据库名：** devops_ci_auth

**文档版本：** 1.0.0

**文档描述：** devops_ci_auth的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_AUTH_GROUP_INFO](#T_AUTH_GROUP_INFO) | 用户组信息表 |
| [T_AUTH_GROUP_PERSSION](#T_AUTH_GROUP_PERSSION) |  |
| [T_AUTH_GROUP_USER](#T_AUTH_GROUP_USER) |  |
| [T_AUTH_IAM_CALLBACK](#T_AUTH_IAM_CALLBACK) | IAM回调地址 |
| [T_AUTH_MANAGER](#T_AUTH_MANAGER) | 管理员策略表 |
| [T_AUTH_MANAGER_USER](#T_AUTH_MANAGER_USER) | 管理员用户表(只存有效期内的用户) |
| [T_AUTH_MANAGER_USER_HISTORY](#T_AUTH_MANAGER_USER_HISTORY) | 管理员用户历史表 |
| [T_AUTH_MANAGER_WHITELIST](#T_AUTH_MANAGER_WHITELIST) | 管理员自助申请表名单表 |
| [T_AUTH_STRATEGY](#T_AUTH_STRATEGY) | 权限策略表 |

**表名：** <a id="T_AUTH_GROUP_INFO">T_AUTH_GROUP_INFO</a>

**说明：** 用户组信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主健ID  |
|  2   | GROUP_NAME |   varchar   | 32 |   0    |    N     |  N   |   ""    | 用户组名称  |
|  3   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识默认用户组标识一致  |
|  4   | GROUP_TYPE |   bit   | 1 |   0    |    N     |  N   |       | 用户组类型0默认分组  |
|  5   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组所属项目  |
|  6   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否删除0可用1删除  |
|  7   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 添加人  |
|  8   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  9   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 修改时间  |
|  11   | DISPLAY_NAME |   varchar   | 32 |   0    |    Y     |  N   |       | 用户组别名  |
|  12   | RELATION_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 关联系统ID  |

**表名：** <a id="T_AUTH_GROUP_PERSSION">T_AUTH_GROUP_PERSSION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主健ID  |
|  2   | AUTH_ACTION |   varchar   | 64 |   0    |    N     |  N   |   ""    | 权限动作  |
|  3   | GROUP_CODE |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组编号默认7个内置组编号固定自定义组编码随机  |
|  4   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 创建人  |
|  5   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  6   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="T_AUTH_GROUP_USER">T_AUTH_GROUP_USER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户ID  |
|  3   | GROUP_ID |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组ID  |
|  4   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 添加用户  |
|  5   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 添加时间  |

**表名：** <a id="T_AUTH_IAM_CALLBACK">T_AUTH_IAM_CALLBACK</a>

**说明：** IAM回调地址

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | GATEWAY |   varchar   | 255 |   0    |    N     |  N   |   ""    | 目标服务网关  |
|  3   | PATH |   varchar   | 1024 |   0    |    N     |  N   |   ""    | 目标接口路径  |
|  4   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除true-是false-否  |
|  5   | RESOURCE |   varchar   | 32 |   0    |    N     |  N   |   ""    | 资源类型  |
|  6   | SYSTEM |   varchar   | 32 |   0    |    N     |  N   |   ""    | 接入系统  |

**表名：** <a id="T_AUTH_MANAGER">T_AUTH_MANAGER</a>

**说明：** 管理员策略表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 32 |   0    |    N     |  N   |       | 名称  |
|  3   | ORGANIZATION_ID |   int   | 10 |   0    |    N     |  N   |       | 组织ID  |
|  4   | LEVEL |   int   | 10 |   0    |    N     |  N   |       | 层级ID  |
|  5   | STRATEGYID |   int   | 10 |   0    |    N     |  N   |       | 权限策略ID  |
|  6   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否删除  |
|  7   | CREATE_USER |   varchar   | 11 |   0    |    N     |  N   |   ""    | 创建用户  |
|  8   | UPDATE_USER |   varchar   | 11 |   0    |    Y     |  N   |   ""    | 修改用户  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_AUTH_MANAGER_USER">T_AUTH_MANAGER_USER</a>

**说明：** 管理员用户表(只存有效期内的用户)

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理员权限ID  |
|  4   | START_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效起始时间  |
|  5   | END_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效结束时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建用户  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改用户  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="T_AUTH_MANAGER_USER_HISTORY">T_AUTH_MANAGER_USER_HISTORY</a>

**说明：** 管理员用户历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理员权限ID  |
|  4   | START_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效起始时间  |
|  5   | END_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效结束时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建用户  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改用户  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_AUTH_MANAGER_WHITELIST">T_AUTH_MANAGER_WHITELIST</a>

**说明：** 管理员自助申请表名单表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理策略ID  |
|  3   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |

**表名：** <a id="T_AUTH_STRATEGY">T_AUTH_STRATEGY</a>

**说明：** 权限策略表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 策略主键ID  |
|  2   | STRATEGY_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 策略名称  |
|  3   | STRATEGY_BODY |   varchar   | 2000 |   0    |    N     |  N   |       | 策略内容  |
|  4   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否删除0未删除1删除  |
|  5   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  7   | CREATE_USER |   varchar   | 32 |   0    |    N     |  N   |       | 创建人  |
|  8   | UPDATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 修改人  |
