# 数据库设计文档

**数据库名：** devops_ci_project

**文档版本：** 1.0.1

**文档描述：** devops_ci_project的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_ACTIVITY](#T_ACTIVITY) |  |
| [T_DATA_SOURCE](#T_DATA_SOURCE) | 模块数据源配置 |
| [T_FAVORITE](#T_FAVORITE) | 关注收藏表 |
| [T_GRAY_TEST](#T_GRAY_TEST) |  |
| [T_I18N_MESSAGE](#T_I18N_MESSAGE) | 国际化信息表 |
| [T_LEAF_ALLOC](#T_LEAF_ALLOC) | ID管理数据表 |
| [T_MESSAGE_CODE_DETAIL](#T_MESSAGE_CODE_DETAIL) | code码详情表 |
| [T_NOTICE](#T_NOTICE) |  |
| [T_PROJECT](#T_PROJECT) | 项目信息表 |
| [T_PROJECT_APPROVAL](#T_PROJECT_APPROVAL) | 项目审批表 |
| [T_PROJECT_DATA_MIGRATE_HISTORY](#T_PROJECT_DATA_MIGRATE_HISTORY) | 项目数据迁移历史表 |
| [T_PROJECT_LABEL](#T_PROJECT_LABEL) |  |
| [T_PROJECT_LABEL_REL](#T_PROJECT_LABEL_REL) |  |
| [T_SERVICE](#T_SERVICE) | 服务信息表 |
| [T_SERVICE_TYPE](#T_SERVICE_TYPE) | 服务类型表 |
| [T_SHARDING_ROUTING_RULE](#T_SHARDING_ROUTING_RULE) | DB分片路由规则 |
| [T_TABLE_SHARDING_CONFIG](#T_TABLE_SHARDING_CONFIG) | 数据库表分片配置 |
| [T_USER](#T_USER) | 用户表 |
| [T_USER_DAILY_FIRST_AND_LAST_LOGIN](#T_USER_DAILY_FIRST_AND_LAST_LOGIN) |  |
| [T_USER_DAILY_LOGIN](#T_USER_DAILY_LOGIN) |  |
| [T_USER_LOCALE](#T_USER_LOCALE) | 用户设置的国际化信息 |

**表名：** <a id="T_ACTIVITY">T_ACTIVITY</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 类型  |
|  3   | NAME |   varchar   | 128 |   0    |    N     |  N   |       | 名称  |
|  4   | ENGLISH_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 英文名称  |
|  5   | LINK |   varchar   | 1024 |   0    |    N     |  N   |       | 跳转链接  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | STATUS |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  8   | CREATOR |   varchar   | 32 |   0    |    N     |  N   |       | 创建者  |

**表名：** <a id="T_DATA_SOURCE">T_DATA_SOURCE</a>

**说明：** 模块数据源配置

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 集群名称  |
|  3   | MODULE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 模块标识  |
|  4   | DATA_SOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 数据源名称  |
|  5   | FULL_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 容量是否满标识true：是，false：否  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  8   | DS_URL |   varchar   | 1024 |   0    |    Y     |  N   |       | 数据源URL地址  |
|  9   | TAG |   varchar   | 128 |   0    |    Y     |  N   |       | 数据源标签  |
|  10   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   DB    | 数据库类型，DB:普通数据库，ARCHIVE_DB:归档数据库  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_FAVORITE">T_FAVORITE</a>

**说明：** 关注收藏表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   bigint   | 20 |   0    |    N     |  Y   |       | 主键id  |
|  2   | service_id |   bigint   | 20 |   0    |    Y     |  N   |       | 服务id  |
|  3   | username |   varchar   | 64 |   0    |    Y     |  N   |       | 用户  |

**表名：** <a id="T_GRAY_TEST">T_GRAY_TEST</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   bigint   | 20 |   0    |    N     |  Y   |       | 主键id  |
|  2   | service_id |   bigint   | 20 |   0    |    Y     |  N   |       | 服务id  |
|  3   | username |   varchar   | 64 |   0    |    Y     |  N   |       | 用户  |
|  4   | status |   varchar   | 64 |   0    |    Y     |  N   |       | 服务状态  |

**表名：** <a id="T_I18N_MESSAGE">T_I18N_MESSAGE</a>

**说明：** 国际化信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | MODULE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 模块标识  |
|  3   | LANGUAGE |   varchar   | 64 |   0    |    N     |  N   |       | 国际化语言信息  |
|  4   | KEY |   varchar   | 256 |   0    |    N     |  N   |       | 国际化变量名  |
|  5   | VALUE |   text   | 65535 |   0    |    N     |  N   |       | 国际化变量值  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  8   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  9   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_LEAF_ALLOC">T_LEAF_ALLOC</a>

**说明：** ID管理数据表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BIZ_TAG |   varchar   | 128 |   0    |    N     |  Y   |       | 业务标签  |
|  2   | MAX_ID |   bigint   | 20 |   0    |    N     |  N   |   1    | 当前最大ID值  |
|  3   | STEP |   int   | 10 |   0    |    N     |  N   |       | 步长,每一次请求获取的ID个数  |
|  4   | DESCRIPTION |   varchar   | 256 |   0    |    Y     |  N   |       | 说明  |
|  5   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_MESSAGE_CODE_DETAIL">T_MESSAGE_CODE_DETAIL</a>

**说明：** code码详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | MESSAGE_CODE |   varchar   | 128 |   0    |    N     |  N   |       | code码  |
|  3   | MODULE_CODE |   char   | 2 |   0    |    N     |  N   |       | 模块代码  |
|  4   | MESSAGE_DETAIL_ZH_CN |   varchar   | 500 |   0    |    N     |  N   |       | 中文简体描述信息  |
|  5   | MESSAGE_DETAIL_ZH_TW |   varchar   | 500 |   0    |    Y     |  N   |       | 中文繁体描述信息  |
|  6   | MESSAGE_DETAIL_EN |   varchar   | 500 |   0    |    Y     |  N   |       | 英文描述信息  |

**表名：** <a id="T_NOTICE">T_NOTICE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NOTICE_TITLE |   varchar   | 100 |   0    |    N     |  N   |       | 公告标题  |
|  3   | EFFECT_DATE |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 生效日期  |
|  4   | INVALID_DATE |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 失效日期  |
|  5   | CREATE_DATE |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建日期  |
|  6   | UPDATE_DATE |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新日期  |
|  7   | NOTICE_CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 公告内容  |
|  8   | REDIRECT_URL |   varchar   | 200 |   0    |    Y     |  N   |       | 跳转地址  |
|  9   | NOTICE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 消息类型:0.弹框1.跑马灯  |
|  10   | SERVICE_NAME |   varchar   | 1024 |   0    |    Y     |  N   |       | 服务名称  |

**表名：** <a id="T_PROJECT">T_PROJECT</a>

**说明：** 项目信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | created_at |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  3   | updated_at |   timestamp   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  4   | deleted_at |   timestamp   | 19 |   0    |    Y     |  N   |       | 删除时间  |
|  5   | extra |   text   | 65535 |   0    |    Y     |  N   |       | 额外信息  |
|  6   | creator |   varchar   | 32 |   0    |    Y     |  N   |       | 创建者  |
|  7   | description |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  8   | kind |   int   | 10 |   0    |    Y     |  N   |       | 容器类型  |
|  9   | cc_app_id |   bigint   | 20 |   0    |    Y     |  N   |       | 应用ID  |
|  10   | cc_app_name |   varchar   | 64 |   0    |    Y     |  N   |       | 应用名称  |
|  11   | is_offlined |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否停用  |
|  12   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  13   | project_name |   varchar   | 64 |   0    |    N     |  N   |       | 项目名称  |
|  14   | english_name |   varchar   | 64 |   0    |    N     |  N   |       | 英文名称  |
|  15   | updator |   varchar   | 32 |   0    |    Y     |  N   |       | 更新人  |
|  16   | project_type |   int   | 10 |   0    |    Y     |  N   |       | 项目类型  |
|  17   | use_bk |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否用蓝鲸  |
|  18   | deploy_type |   text   | 65535 |   0    |    Y     |  N   |       | 部署类型  |
|  19   | bg_id |   bigint   | 20 |   0    |    Y     |  N   |       | 事业群ID  |
|  20   | bg_name |   varchar   | 255 |   0    |    Y     |  N   |       | 事业群名称  |
|  21   | business_line_id |   bigint   | 20 |   0    |    Y     |  N   |       | 业务线ID  |
|  22   | business_line_name |   varchar   | 255 |   0    |    Y     |  N   |       | 业务线名称  |
|  23   | dept_id |   bigint   | 20 |   0    |    Y     |  N   |       | 项目所属二级机构ID  |
|  24   | dept_name |   varchar   | 255 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  25   | center_id |   bigint   | 20 |   0    |    Y     |  N   |       | 中心ID  |
|  26   | center_name |   varchar   | 255 |   0    |    Y     |  N   |       | 中心名字  |
|  27   | data_id |   bigint   | 20 |   0    |    Y     |  N   |       | 数据ID  |
|  28   | is_secrecy |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否保密  |
|  29   | is_helm_chart_enabled |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否启用图表激活  |
|  30   | approval_status |   int   | 10 |   0    |    Y     |  N   |   1    | 审核状态  |
|  31   | logo_addr |   text   | 65535 |   0    |    Y     |  N   |       | logo地址  |
|  32   | approver |   varchar   | 32 |   0    |    Y     |  N   |       | 批准人  |
|  33   | remark |   text   | 65535 |   0    |    Y     |  N   |       | 评论  |
|  34   | approval_time |   timestamp   | 19 |   0    |    Y     |  N   |       | 批准时间  |
|  35   | creator_bg_name |   varchar   | 128 |   0    |    Y     |  N   |       | 创建者事业群名称  |
|  36   | creator_dept_name |   varchar   | 128 |   0    |    Y     |  N   |       | 创建者项目所属二级机构名称  |
|  37   | creator_center_name |   varchar   | 128 |   0    |    Y     |  N   |       | 创建者中心名字  |
|  38   | hybrid_cc_app_id |   bigint   | 20 |   0    |    Y     |  N   |       | 应用ID  |
|  39   | enable_external |   bit   | 1 |   0    |    Y     |  N   |       | 是否支持构建机访问外网  |
|  40   | enable_idc |   bit   | 1 |   0    |    Y     |  N   |       | 是否支持IDC构建机  |
|  41   | enabled |   bit   | 1 |   0    |    Y     |  N   |       | 是否启用  |
|  42   | CHANNEL |   varchar   | 32 |   0    |    N     |  N   |   BS    | 项目渠道  |
|  43   | pipeline_limit |   int   | 10 |   0    |    Y     |  N   |   500    | 流水线数量上限  |
|  44   | router_tag |   varchar   | 32 |   0    |    Y     |  N   |       | 网关路由tags  |
|  45   | relation_id |   varchar   | 32 |   0    |    Y     |  N   |       | 扩展系统关联ID  |
|  46   | other_router_tags |   varchar   | 128 |   0    |    Y     |  N   |       | 其他系统网关路由tags  |
|  47   | properties |   text   | 65535 |   0    |    Y     |  N   |       | 项目其他配置  |
|  48   | SUBJECT_SCOPES |   text   | 65535 |   0    |    Y     |  N   |       | 最大可授权人员范围  |
|  49   | AUTH_SECRECY |   int   | 10 |   0    |    Y     |  N   |   0    | 项目性质,0-公开，1-保密,2-机密  |
|  50   | product_id |   int   | 10 |   0    |    Y     |  N   |       | 运营产品ID  |

**表名：** <a id="T_PROJECT_APPROVAL">T_PROJECT_APPROVAL</a>

**说明：** 项目审批表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 项目名称  |
|  3   | ENGLISH_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 英文名称  |
|  4   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  5   | CREATED_AT |   timestamp   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | UPDATED_AT |   timestamp   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  7   | CREATOR |   varchar   | 32 |   0    |    Y     |  N   |       | 创建者  |
|  8   | UPDATOR |   varchar   | 32 |   0    |    Y     |  N   |       | 更新人  |
|  9   | BG_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 事业群ID  |
|  10   | BG_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 事业群名称  |
|  11   | BUSINESS_LINE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 业务线ID  |
|  12   | BUSINESS_LINE_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 业务线名称  |
|  13   | DEPT_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 项目所属二级机构ID  |
|  14   | DEPT_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  15   | CENTER_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 中心ID  |
|  16   | CENTER_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 中心名字  |
|  17   | LOGO_ADDR |   text   | 65535 |   0    |    Y     |  N   |       | logo地址  |
|  18   | APPROVER |   varchar   | 32 |   0    |    Y     |  N   |       | 批准人  |
|  19   | APPROVAL_STATUS |   int   | 10 |   0    |    Y     |  N   |   1    | 审核状态  |
|  20   | APPROVAL_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 批准时间  |
|  21   | RELATION_ID |   varchar   | 32 |   0    |    Y     |  N   |       | 扩展系统关联ID  |
|  22   | SUBJECT_SCOPES |   text   | 65535 |   0    |    Y     |  N   |       | 最大可授权人员范围  |
|  23   | AUTH_SECRECY |   int   | 10 |   0    |    Y     |  N   |   0    | 项目性质,0-公开,1-保密,2-机密  |
|  24   | TIPS_STATUS |   int   | 10 |   0    |    Y     |  N   |   0    | 提示状态,0-不展示,1-展示创建成功,2-展示更新成功  |
|  25   | PROJECT_TYPE |   int   | 10 |   0    |    Y     |  N   |       | 项目类型  |
|  26   | PRODUCT_ID |   int   | 10 |   0    |    Y     |  N   |       | 运营产品ID  |
|  27   | PRODUCT_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 运营产品名称  |

**表名：** <a id="T_PROJECT_DATA_MIGRATE_HISTORY">T_PROJECT_DATA_MIGRATE_HISTORY</a>

**说明：** 项目数据迁移历史表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | MODULE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 模块标识  |
|  4   | SOURCE_CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 被迁移集群名称  |
|  5   | SOURCE_DATA_SOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 被迁移数据源名称  |
|  6   | TARGET_CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 迁移集群名称  |
|  7   | TARGET_DATA_SOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 迁移数据源名称  |
|  8   | TARGET_DATA_TAG |   varchar   | 128 |   0    |    Y     |  N   |       | 迁移数据源标签  |
|  9   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线ID  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  12   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  13   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_PROJECT_LABEL">T_PROJECT_LABEL</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LABEL_NAME |   varchar   | 45 |   0    |    N     |  N   |       | 标签名称  |
|  3   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  4   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_PROJECT_LABEL_REL">T_PROJECT_LABEL_REL</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LABEL_ID |   varchar   | 32 |   0    |    N     |  N   |       | 标签ID  |
|  3   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |

**表名：** <a id="T_SERVICE">T_SERVICE</a>

**说明：** 服务信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   bigint   | 20 |   0    |    N     |  Y   |       | id  |
|  2   | name |   varchar   | 64 |   0    |    Y     |  N   |       | 名称  |
|  3   | english_name |   varchar   | 64 |   0    |    Y     |  N   |       | 英文名称  |
|  4   | service_type_id |   bigint   | 20 |   0    |    Y     |  N   |       | 服务类型ID  |
|  5   | link |   varchar   | 255 |   0    |    Y     |  N   |       | 跳转链接  |
|  6   | link_new |   varchar   | 255 |   0    |    Y     |  N   |       | 新跳转链接  |
|  7   | inject_type |   varchar   | 64 |   0    |    Y     |  N   |       | 注入类型  |
|  8   | iframe_url |   varchar   | 255 |   0    |    Y     |  N   |       | iframeUrl地址  |
|  9   | css_url |   varchar   | 255 |   0    |    Y     |  N   |       | cssUrl地址  |
|  10   | js_url |   varchar   | 255 |   0    |    Y     |  N   |       | jsUrl地址  |
|  11   | show_project_list |   bit   | 1 |   0    |    Y     |  N   |       | 是否在页面显示  |
|  12   | show_nav |   bit   | 1 |   0    |    Y     |  N   |       | showNav  |
|  13   | project_id_type |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID类型  |
|  14   | status |   varchar   | 64 |   0    |    Y     |  N   |       | 状态  |
|  15   | created_user |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  16   | created_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  17   | updated_user |   varchar   | 64 |   0    |    Y     |  N   |       | 修改者  |
|  18   | updated_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |
|  19   | deleted |   bit   | 1 |   0    |    Y     |  N   |       | 是否删除  |
|  20   | gray_css_url |   varchar   | 255 |   0    |    Y     |  N   |       | 灰度cssUrl地址  |
|  21   | gray_js_url |   varchar   | 255 |   0    |    Y     |  N   |       | 灰度jsUrl地址  |
|  22   | logo_url |   varchar   | 256 |   0    |    Y     |  N   |       | logo地址  |
|  23   | web_socket |   text   | 65535 |   0    |    Y     |  N   |       | 支持webSocket的页面  |
|  24   | weight |   int   | 10 |   0    |    Y     |  N   |       | 权值  |
|  25   | gray_iframe_url |   varchar   | 255 |   0    |    Y     |  N   |       | 灰度iframeUrl地址  |
|  26   | new_window |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否打开新标签页  |
|  27   | new_windowUrl |   varchar   | 200 |   0    |    Y     |  N   |       | 新标签页地址  |
|  28   | cluster_type |   varchar   | 32 |   0    |    N     |  N   |       | 集群类型  |

**表名：** <a id="T_SERVICE_TYPE">T_SERVICE_TYPE</a>

**说明：** 服务类型表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | title |   varchar   | 64 |   0    |    Y     |  N   |       | 邮件标题  |
|  3   | english_title |   varchar   | 64 |   0    |    Y     |  N   |       | 英文邮件标题  |
|  4   | created_user |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  5   | created_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | updated_user |   varchar   | 64 |   0    |    Y     |  N   |       | 修改者  |
|  7   | updated_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |
|  8   | deleted |   bit   | 1 |   0    |    Y     |  N   |       | 是否删除  |
|  9   | weight |   int   | 10 |   0    |    Y     |  N   |       | 权值  |

**表名：** <a id="T_SHARDING_ROUTING_RULE">T_SHARDING_ROUTING_RULE</a>

**说明：** DB分片路由规则

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ROUTING_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 路由名称  |
|  3   | ROUTING_RULE |   varchar   | 256 |   0    |    N     |  N   |       | 路由规则  |
|  4   | CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |   prod    | 集群名称  |
|  5   | MODULE_CODE |   varchar   | 64 |   0    |    N     |  N   |   PROCESS    | 模块标识  |
|  6   | TYPE |   varchar   | 32 |   0    |    N     |  N   |   DB    | 路由类型，DB:数据库，TABLE:数据库表  |
|  7   | DATA_SOURCE_NAME |   varchar   | 128 |   0    |    N     |  N   |   ds_0    | 数据源名称  |
|  8   | TABLE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 数据库表名称  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_TABLE_SHARDING_CONFIG">T_TABLE_SHARDING_CONFIG</a>

**说明：** 数据库表分片配置

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CLUSTER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 集群名称  |
|  3   | MODULE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 模块标识  |
|  4   | TABLE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 数据库表名称  |
|  5   | SHARDING_NUM |   int   | 10 |   0    |    N     |  N   |   5    | 分表数量  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  8   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  9   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_USER">T_USER</a>

**说明：** 用户表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 用户ID  |
|  2   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  3   | BG_ID |   int   | 10 |   0    |    N     |  N   |       | 事业群ID  |
|  4   | BG_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 事业群名称  |
|  5   | DEPT_ID |   int   | 10 |   0    |    Y     |  N   |       | 项目所属二级机构ID  |
|  6   | DEPT_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  7   | CENTER_ID |   int   | 10 |   0    |    Y     |  N   |       | 中心ID  |
|  8   | CENTER_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 中心名字  |
|  9   | GROYP_ID |   int   | 10 |   0    |    Y     |  N   |       | 用户组ID  |
|  10   | GROUP_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 用户组名称  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  13   | USER_TYPE |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 用户类型0普通用户1公共账号  |

**表名：** <a id="T_USER_DAILY_FIRST_AND_LAST_LOGIN">T_USER_DAILY_FIRST_AND_LAST_LOGIN</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | DATE |   date   | 10 |   0    |    N     |  N   |       | 日期  |
|  4   | FIRST_LOGIN_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 首次登录时间  |
|  5   | LAST_LOGIN_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 最近登录时间  |

**表名：** <a id="T_USER_DAILY_LOGIN">T_USER_DAILY_LOGIN</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | DATE |   date   | 10 |   0    |    N     |  N   |       | 日期  |
|  4   | LOGIN_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 登录时间  |
|  5   | OS |   varchar   | 32 |   0    |    N     |  N   |       | 操作系统  |
|  6   | IP |   varchar   | 32 |   0    |    N     |  N   |       | ip地址  |

**表名：** <a id="T_USER_LOCALE">T_USER_LOCALE</a>

**说明：** 用户设置的国际化信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | USER_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 用户ID  |
|  2   | LANGUAGE |   varchar   | 64 |   0    |    N     |  N   |       | 国际化语言信息  |
|  3   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  4   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
