# 数据库设计文档

**数据库名：** devops_ci_auth

**文档版本：** 1.0.9

**文档描述：** devops_ci_auth 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_AUTH_ACTION | 权限操作表 |
| T_AUTH_GROUP_INFO | 用户组信息表 |
| T_AUTH_GROUP_PERSSION |  |
| T_AUTH_GROUP_USER |  |
| T_AUTH_HANDOVER_DETAIL | 权限交接详细表 |
| T_AUTH_HANDOVER_OVERVIEW | 权限交接总览表 |
| T_AUTH_IAM_CALLBACK | IAM 回调地址 |
| T_AUTH_ITSM_CALLBACK | 权限 itsm 回调表 |
| T_AUTH_MANAGER | 管理员策略表 |
| T_AUTH_MANAGER_APPROVAL | 蓝盾超级管理员权限续期审核表 |
| T_AUTH_MANAGER_USER | 管理员用户表(只存有效期内的用户) |
| T_AUTH_MANAGER_USER_HISTORY | 管理员用户历史表 |
| T_AUTH_MANAGER_WHITELIST | 管理员自助申请表名单表 |
| T_AUTH_MIGRATION | 权限迁移 |
| T_AUTH_MONITOR_SPACE | 蓝盾监控空间权限表 |
| T_AUTH_OAUTH2_ACCESS_TOKEN | ACCESS_TOKEN 表 |
| T_AUTH_OAUTH2_CLIENT_DETAILS | 客户端信息表 |
| T_AUTH_OAUTH2_CODE | 授权码表 |
| T_AUTH_OAUTH2_REFRESH_TOKEN | REFRESH_TOKEN 表 |
| T_AUTH_OAUTH2_SCOPE | 授权范围表 |
| T_AUTH_OAUTH2_SCOPE_OPERATION | 授权操作信息表 |
| T_AUTH_RESOURCE | 资源表 |
| T_AUTH_RESOURCE_AUTHORIZATION | 资源授权管理表 |
| T_AUTH_RESOURCE_GROUP | 资源关联用户组表 |
| T_AUTH_RESOURCE_GROUP_APPLY | 用户组申请记录表 |
| T_AUTH_RESOURCE_GROUP_CONFIG | 资源用户组配置表 |
| T_AUTH_RESOURCE_GROUP_MEMBER | 资源组成员 |
| T_AUTH_RESOURCE_GROUP_PERMISSION | 资源组权限表 |
| T_AUTH_RESOURCE_SYNC | 同步 IAM 资源 |
| T_AUTH_RESOURCE_TYPE | 权限资源类型表 |
| T_AUTH_STRATEGY | 权限策略表 |
| T_AUTH_TEMPORARY_VERIFY_RECORD | 迁移-鉴权记录表 |
| T_AUTH_USER_BLACKLIST |  |
| T_AUTH_USER_INFO | 账号信息表 |

**表名：** <a>T_AUTH_ACTION</a>

**说明：** 权限操作表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ACTION |   varchar   | 64 |   0    |    N     |  Y   |       | 操作 ID  |
|  2   | RESOURCE_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 蓝盾-关联资源类型  |
|  3   | RELATED_RESOURCE_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | IAM-关联资源类型  |
|  4   | ACTION_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 操作名称  |
|  5   | ENGLISH_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 动作英文名称  |
|  6   | CREATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 创建者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  9   | DELETE |   bit   | 1 |   0    |    Y     |  N   |       | 是否删除  |
|  10   | ACTION_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 操作类型  |

**表名：** <a>T_AUTH_GROUP_INFO</a>

**说明：** 用户组信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主健 ID  |
|  2   | GROUP_NAME |   varchar   | 32 |   0    |    N     |  N   |   ""    | 用户组名称  |
|  3   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识默认用户组标识一致  |
|  4   | GROUP_TYPE |   bit   | 1 |   0    |    N     |  N   |       | 用户组类型 0 默认分组  |
|  5   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组所属项目  |
|  6   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否删除 0 可用 1 删除  |
|  7   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 添加人  |
|  8   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  9   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 修改时间  |
|  11   | DISPLAY_NAME |   varchar   | 32 |   0    |    Y     |  N   |       | 用户组别名  |
|  12   | RELATION_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 关联系统 ID  |

**表名：** <a>T_AUTH_GROUP_PERSSION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主健 ID  |
|  2   | AUTH_ACTION |   varchar   | 64 |   0    |    N     |  N   |   ""    | 权限动作  |
|  3   | GROUP_CODE |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组编号默认 7 个内置组编号固定自定义组编码随机  |
|  4   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 创建人  |
|  5   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  6   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a>T_AUTH_GROUP_USER</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户 ID  |
|  3   | GROUP_ID |   varchar   | 64 |   0    |    N     |  N   |   ""    | 用户组 ID  |
|  4   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |   ""    | 添加用户  |
|  5   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 添加时间  |

**表名：** <a>T_AUTH_HANDOVER_DETAIL</a>

**说明：** 权限交接详细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | FLOW_NO |   varchar   | 64 |   0    |    N     |  N   |       | 流程单号  |
|  4   | ITEM_ID |   varchar   | 255 |   0    |    N     |  N   |       | 组/授权资源 ID  |
|  5   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 组/授权资源关联的资源类型  |
|  6   | HANDOVER_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 交接类型-group/authorization  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_HANDOVER_OVERVIEW</a>

**说明：** 权限交接总览表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | PROJECT_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 项目名称  |
|  4   | FLOW_NO |   varchar   | 64 |   0    |    N     |  N   |       | 流程单号  |
|  5   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 标题  |
|  6   | APPLICANT |   varchar   | 32 |   0    |    N     |  N   |       | 申请人  |
|  7   | APPROVER |   varchar   | 32 |   0    |    Y     |  N   |       | 审批人  |
|  8   | STATUS |   int   | 10 |   0    |    Y     |  N   |   0    | 审批结果，0-审批中，1-审批成功，2-审批拒绝，3-撤销  |
|  9   | GROUP_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 用户组数  |
|  10   | AUTHORIZATION_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 授权个数  |
|  11   | REMARK |   varchar   | 256 |   0    |    Y     |  N   |       | 备注  |
|  12   | LAST_OPERATOR |   varchar   | 32 |   0    |    Y     |  N   |       | 最后操作人  |
|  13   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  14   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_IAM_CALLBACK</a>

**说明：** IAM 回调地址

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | GATEWAY |   varchar   | 255 |   0    |    N     |  N   |   ""    | 目标服务网关  |
|  3   | PATH |   varchar   | 1024 |   0    |    N     |  N   |   ""    | 目标接口路径  |
|  4   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除 true-是 false-否  |
|  5   | RESOURCE |   varchar   | 32 |   0    |    N     |  N   |   ""    | 资源类型  |
|  6   | SYSTEM |   varchar   | 32 |   0    |    N     |  N   |   ""    | 接入系统  |

**表名：** <a>T_AUTH_ITSM_CALLBACK</a>

**说明：** 权限 itsm 回调表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 自增 ID  |
|  2   | APPLY_ID |   int   | 10 |   0    |    N     |  N   |       | 权限中心申请单 ID  |
|  3   | SN |   varchar   | 64 |   0    |    N     |  N   |       | ITSM 申请单号  |
|  4   | ENGLISH_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 项目英文名  |
|  5   | CALLBACK_ID |   varchar   | 32 |   0    |    N     |  N   |       | 权限中心审批单 ID  |
|  6   | APPLICANT |   varchar   | 32 |   0    |    N     |  N   |       | 申请人  |
|  7   | APPROVER |   varchar   | 32 |   0    |    Y     |  N   |       | 最后审批人  |
|  8   | APPROVE_RESULT |   bit   | 1 |   0    |    Y     |  N   |       | 审批结果，0-审批拒绝，1-审批成功  |
|  9   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  10   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a>T_AUTH_MANAGER</a>

**说明：** 管理员策略表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | NAME |   varchar   | 32 |   0    |    N     |  N   |       | 名称  |
|  3   | ORGANIZATION_ID |   int   | 10 |   0    |    N     |  N   |       | 组织 ID  |
|  4   | LEVEL |   int   | 10 |   0    |    N     |  N   |       | 层级 ID  |
|  5   | STRATEGYID |   int   | 10 |   0    |    N     |  N   |       | 权限策略 ID  |
|  6   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否删除  |
|  7   | CREATE_USER |   varchar   | 11 |   0    |    N     |  N   |   ""    | 创建用户  |
|  8   | UPDATE_USER |   varchar   | 11 |   0    |    Y     |  N   |   ""    | 修改用户  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a>T_AUTH_MANAGER_APPROVAL</a>

**说明：** 蓝盾超级管理员权限续期审核表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 自增 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理员权限 ID  |
|  4   | EXPIRED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限过期时间  |
|  5   | START_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 审批单生效时间  |
|  6   | END_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 审批单失效时间  |
|  7   | STATUS |   int   | 10 |   0    |    N     |  N   |       | 发送状态 0-审核流程中,1-用户拒绝续期,2-用户同意续期,3-审批人拒绝续期，4-审批人同意续期  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a>T_AUTH_MANAGER_USER</a>

**说明：** 管理员用户表(只存有效期内的用户)

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理员权限 ID  |
|  4   | START_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效起始时间  |
|  5   | END_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效结束时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建用户  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改用户  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a>T_AUTH_MANAGER_USER_HISTORY</a>

**说明：** 管理员用户历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理员权限 ID  |
|  4   | START_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效起始时间  |
|  5   | END_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 权限生效结束时间  |
|  6   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建用户  |
|  7   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改用户  |
|  8   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a>T_AUTH_MANAGER_WHITELIST</a>

**说明：** 管理员自助申请表名单表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | MANAGER_ID |   int   | 10 |   0    |    N     |  N   |       | 管理策略 ID  |
|  3   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |

**表名：** <a>T_AUTH_MIGRATION</a>

**说明：** 权限迁移

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | STATUS |   int   | 10 |   0    |    Y     |  N   |   0    | 迁移状态,0-迁移中,1-迁移成功,2-迁移失败  |
|  3   | BEFORE_GROUP_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 迁移前用户组数  |
|  4   | AFTER_GROUP_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 迁移后用户组数  |
|  5   | RESOURCE_COUNT |   text   | 65535 |   0    |    Y     |  N   |       | 迁移后资源数和资源用户组数  |
|  6   | START_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 开始时间  |
|  7   | END_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  8   | TOTAL_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 总耗时  |
|  9   | ERROR_MESSAGE |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  10   | ROUTER_TAG |   varchar   | 32 |   0    |    Y     |  N   |       | 迁移项目的网关路由 tags  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_MONITOR_SPACE</a>

**说明：** 蓝盾监控空间权限表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | SPACE_BIZ_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 监控空间业务 ID  |
|  3   | SPACE_UID |   varchar   | 64 |   0    |    N     |  N   |       | 监控空间 ID  |
|  4   | CREATOR |   varchar   | 32 |   0    |    N     |  N   |       | 创建人  |
|  5   | UPDATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 更新者  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_OAUTH2_ACCESS_TOKEN</a>

**说明：** ACCESS_TOKEN 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ACCESS_TOKEN |   varchar   | 64 |   0    |    N     |  N   |       | ACCESS_TOKEN  |
|  2   | CLIENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 客户端 ID  |
|  3   | USER_NAME |   varchar   | 32 |   0    |    Y     |  N   |       | 登录的用户名，客户端模式该值为空  |
|  4   | PASS_WORD |   varchar   | 64 |   0    |    Y     |  N   |       | 用于密码模式  |
|  5   | GRANT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 授权模式  |
|  6   | EXPIRED_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 过期时间  |
|  7   | REFRESH_TOKEN |   varchar   | 64 |   0    |    Y     |  N   |       | REFRESH_TOKEN，客户端模式该值为空  |
|  8   | SCOPE_ID |   int   | 10 |   0    |    N     |  N   |       | 授权范围 ID  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_OAUTH2_CLIENT_DETAILS</a>

**说明：** 客户端信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | CLIENT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 客户端标识  |
|  2   | CLIENT_SECRET |   varchar   | 64 |   0    |    N     |  N   |       | 客户端秘钥  |
|  3   | CLIENT_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 客户端名称  |
|  4   | SCOPE |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 授权操作范围  |
|  5   | ICON |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 图标  |
|  6   | AUTHORIZED_GRANT_TYPES |   varchar   | 64 |   0    |    N     |  N   |       | 授权模式  |
|  7   | WEB_SERVER_REDIRECT_URI |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | 跳转链接  |
|  8   | ACCESS_TOKEN_VALIDITY |   bigint   | 20 |   0    |    N     |  N   |       | access_token 有效时间  |
|  9   | REFRESH_TOKEN_VALIDITY |   bigint   | 20 |   0    |    Y     |  N   |       | refresh_token 有效时间  |
|  10   | CREATE_USER |   varchar   | 32 |   0    |    N     |  N   |   ""    | 创建人  |
|  11   | UPDATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 修改人  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_OAUTH2_CODE</a>

**说明：** 授权码表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | CLIENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 客户端标识  |
|  2   | CODE |   varchar   | 64 |   0    |    N     |  N   |       | 授权码  |
|  3   | USER_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 用户名  |
|  4   | EXPIRED_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 过期时间  |
|  5   | SCOPE_ID |   int   | 10 |   0    |    N     |  N   |       | 授权范围 ID  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_OAUTH2_REFRESH_TOKEN</a>

**说明：** REFRESH_TOKEN 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REFRESH_TOKEN |   varchar   | 64 |   0    |    N     |  N   |       | REFRESH_TOKEN  |
|  2   | CLIENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 客户端 ID  |
|  3   | EXPIRED_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 过期时间  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_OAUTH2_SCOPE</a>

**说明：** 授权范围表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主健 ID  |
|  2   | SCOPE |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 授权范围  |
|  3   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_OAUTH2_SCOPE_OPERATION</a>

**说明：** 授权操作信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主健 ID  |
|  2   | OPERATION_ID |   varchar   | 64 |   0    |    N     |  N   |       | 授权操作 ID  |
|  3   | OPERATION_NAME_CN |   varchar   | 64 |   0    |    N     |  N   |       | 授权操作中文名称  |
|  4   | OPERATION_NAME_EN |   varchar   | 64 |   0    |    N     |  N   |       | 授权操作英文名称  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a>T_AUTH_RESOURCE</a>

**说明：** 资源表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 资源 ID  |
|  5   | RESOURCE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 资源名  |
|  6   | IAM_RESOURCE_CODE |   varchar   | 32 |   0    |    N     |  N   |       | IAM 资源 ID  |
|  7   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 开启权限管理,0-不启用,1-启用  |
|  8   | RELATION_ID |   varchar   | 32 |   0    |    N     |  N   |       | 关联的 IAM 分级管理员 ID  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  11   | CREATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  12   | UPDATE_USER |   varchar   | 64 |   0    |    N     |  N   |       | 修改人  |

**表名：** <a>T_AUTH_RESOURCE_AUTHORIZATION</a>

**说明：** 资源授权管理表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 资源 ID  |
|  5   | RESOURCE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 资源名  |
|  6   | HANDOVER_FROM |   varchar   | 64 |   0    |    N     |  N   |       | 授予人  |
|  7   | HANDOVER_FROM_CN_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 授予人中文名称  |
|  8   | HANDOVER_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 授予时间  |
|  9   | CREATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_GROUP</a>

**说明：** 资源关联用户组表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 资源 ID  |
|  5   | RESOURCE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 资源名  |
|  6   | IAM_RESOURCE_CODE |   varchar   | 32 |   0    |    N     |  N   |       | IAM 资源 ID  |
|  7   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识  |
|  8   | GROUP_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 用户组名称  |
|  9   | DEFAULT_GROUP |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否为默认组,0-非默认组,1-默认组  |
|  10   | RELATION_ID |   varchar   | 32 |   0    |    N     |  N   |       | 关联的 IAM 组 ID  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  13   | DESCRIPTION |   varchar   | 512 |   0    |    Y     |  N   |       | 用户组描述  |
|  14   | IAM_TEMPLATE_ID |   int   | 10 |   0    |    Y     |  N   |       | 人员模板 ID  |

**表名：** <a>T_AUTH_RESOURCE_GROUP_APPLY</a>

**说明：** 用户组申请记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | MEMBER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 成员 ID  |
|  4   | IAM_GROUP_ID |   int   | 10 |   0    |    N     |  N   |       | IAM 组 ID  |
|  5   | STATUS |   int   | 10 |   0    |    Y     |  N   |   0    | 状态,0-审批中,1-审批成功,2-审批超时  |
|  6   | NUMBER_OF_CHECKS |   int   | 10 |   0    |    Y     |  N   |   0    | 检查次数，用于同步组数据  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_GROUP_CONFIG</a>

**说明：** 资源用户组配置表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  3   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识  |
|  4   | GROUP_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 用户组名称  |
|  5   | CREATE_MODE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 创建模式,0-开启时创建,1-启用权限管理时创建  |
|  6   | GROUP_TYPE |   int   | 10 |   0    |    N     |  N   |   0    | 用户组类型 0-默认组 1-自定义组  |
|  7   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 用户组描述  |
|  8   | AUTHORIZATION_SCOPES |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 用户组授权范围  |
|  9   | ACTIONS |   text   | 65535 |   0    |    Y     |  N   |       | 用户组拥有的资源操作  |
|  10   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_GROUP_MEMBER</a>

**说明：** 资源组成员

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 资源 ID  |
|  5   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识  |
|  6   | IAM_GROUP_ID |   int   | 10 |   0    |    N     |  N   |       | IAM 组 ID  |
|  7   | MEMBER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 成员 ID  |
|  8   | MEMBER_NAME |   varchar   | 512 |   0    |    N     |  N   |       | 成员名  |
|  9   | MEMBER_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 成员类型,用户/组织/模板  |
|  10   | EXPIRED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 过期时间  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_GROUP_PERMISSION</a>

**说明：** 资源组权限表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组关联的资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 用户组关联的资源 ID  |
|  5   | IAM_RESOURCE_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组关联的 IAM 资源 ID  |
|  6   | GROUP_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组标识  |
|  7   | IAM_GROUP_ID |   int   | 10 |   0    |    N     |  N   |       | 关联的 IAM 组 ID  |
|  8   | ACTION |   varchar   | 64 |   0    |    N     |  N   |       | 操作 ID  |
|  9   | ACTION_RELATED_RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 动作关联的资源类型  |
|  10   | RELATED_RESOURCE_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 组权限关联的资源类型  |
|  11   | RELATED_RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 组权限关联的资源 ID  |
|  12   | RELATED_IAM_RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  N   |       | 组权限关联的资源 ID  |
|  13   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  14   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_SYNC</a>

**说明：** 同步 IAM 资源

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  2   | STATUS |   int   | 10 |   0    |    Y     |  N   |   0    | 迁移状态,0-同步中,1-同步成功,2-同步失败  |
|  3   | ERROR_MESSAGE |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |
|  4   | TOTAL_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 总耗时  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a>T_AUTH_RESOURCE_TYPE</a>

**说明：** 权限资源类型表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  N   |       | ID  |
|  2   | RESOURCE_TYPE |   varchar   | 64 |   0    |    N     |  Y   |       | 资源类型  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 资源名称  |
|  4   | ENGLISH_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 资源英文名称  |
|  5   | DESC |   varchar   | 255 |   0    |    N     |  N   |       | 资源描述  |
|  6   | ENGLISH_DESC |   varchar   | 255 |   0    |    Y     |  N   |       | 资源英文描述  |
|  7   | PARENT |   varchar   | 255 |   0    |    Y     |  N   |       | 父类资源  |
|  8   | SYSTEM |   varchar   | 255 |   0    |    N     |  N   |       | 所属系统  |
|  9   | CREATE_USER |   varchar   | 32 |   0    |    N     |  N   |       | 创建者  |
|  10   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  11   | UPDATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 更新者  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  13   | DELETE |   bit   | 1 |   0    |    Y     |  N   |       | 是否删除  |

**表名：** <a>T_AUTH_STRATEGY</a>

**说明：** 权限策略表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 策略主键 ID  |
|  2   | STRATEGY_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 策略名称  |
|  3   | STRATEGY_BODY |   varchar   | 2000 |   0    |    N     |  N   |       | 策略内容  |
|  4   | IS_DELETE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否删除 0 未删除 1 删除  |
|  5   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATE_TIME |   timestamp   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  7   | CREATE_USER |   varchar   | 32 |   0    |    N     |  N   |       | 创建人  |
|  8   | UPDATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 修改人  |

**表名：** <a>T_AUTH_TEMPORARY_VERIFY_RECORD</a>

**说明：** 迁移-鉴权记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 用户 ID  |
|  2   | PROJECT_CODE |   varchar   | 64 |   0    |    N     |  Y   |       | 项目 ID  |
|  3   | RESOURCE_TYPE |   varchar   | 64 |   0    |    N     |  Y   |       | 资源类型  |
|  4   | RESOURCE_CODE |   varchar   | 255 |   0    |    N     |  Y   |       | 资源 ID  |
|  5   | ACTION |   varchar   | 64 |   0    |    N     |  Y   |       | 操作 ID  |
|  6   | VERIFY_RESULT |   bit   | 1 |   0    |    N     |  N   |       | 鉴权结果  |
|  7   | LAST_VERIFY_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 最后鉴权时间  |

**表名：** <a>T_AUTH_USER_BLACKLIST</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       |   |
|  2   | USER_ID |   varchar   | 32 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | REMARK |   varchar   | 255 |   0    |    N     |  N   |       | 拉黑原因  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 拉黑时间  |
|  5   | STATUS |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否生效 1 生效 0 不生效  |

**表名：** <a>T_AUTH_USER_INFO</a>

**说明：** 账号信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       |   |
|  2   | userId |   varchar   | 255 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | email |   varchar   | 255 |   0    |    Y     |  N   |       | 邮箱  |
|  4   | phone |   varchar   | 32 |   0    |    Y     |  N   |       | 手机号  |
|  5   | create_time |   datetime   | 19 |   0    |    N     |  N   |       | 注册时间  |
|  6   | user_type |   int   | 10 |   0    |    N     |  N   |       | 用户类型 0.页面注册 1.GitHub2.Gitlab  |
|  7   | last_login_time |   datetime   | 19 |   0    |    Y     |  N   |       | 最后登陆时间  |
|  8   | user_status |   int   | 10 |   0    |    N     |  N   |       | 用户状态,0--正常,1--冻结  |
