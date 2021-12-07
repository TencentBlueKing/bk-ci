# 数据库设计文档

**数据库名：** devops_ci_op

**文档版本：** 1.0.0

**文档描述：** devops_ci_op的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [dept_info](#dept_info) |  |
| [project_info](#project_info) |  |
| [role](#role) |  |
| [role_permission](#role_permission) |  |
| [schema_version](#schema_version) |  |
| [spring_session](#spring_session) |  |
| [SPRING_SESSION_ATTRIBUTES](#SPRING_SESSION_ATTRIBUTES) |  |
| [t_user_token](#t_user_token) |  |
| [url_action](#url_action) |  |
| [user](#user) |  |
| [user_permission](#user_permission) |  |
| [user_role](#user_role) |  |

**表名：** <a id="dept_info">dept_info</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  3   | dept_id |   int   | 10 |   0    |    N     |  N   |       | 项目所属二级机构ID  |
|  4   | dept_name |   varchar   | 100 |   0    |    N     |  N   |       | 项目所属二级机构名称  |
|  5   | level |   int   | 10 |   0    |    N     |  N   |       | 层级ID  |
|  6   | parent_dept_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="project_info">project_info</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | approval_status |   int   | 10 |   0    |    Y     |  N   |       | 审核状态  |
|  3   | approval_time |   datetime   | 19 |   0    |    Y     |  N   |       | 批准时间  |
|  4   | approver |   varchar   | 100 |   0    |    Y     |  N   |       | 批准人  |
|  5   | cc_app_id |   int   | 10 |   0    |    Y     |  N   |       | 应用ID  |
|  6   | created_at |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  7   | creator |   varchar   | 100 |   0    |    Y     |  N   |       | 创建者  |
|  8   | creator_bg_name |   varchar   | 100 |   0    |    Y     |  N   |       | 创建者事业群名称  |
|  9   | creator_center_name |   varchar   | 100 |   0    |    Y     |  N   |       | 创建者中心名字  |
|  10   | creator_dept_name |   varchar   | 100 |   0    |    Y     |  N   |       | 创建者项目所属二级机构名称  |
|  11   | english_name |   varchar   | 255 |   0    |    Y     |  N   |       | 英文名称  |
|  12   | is_offlined |   bit   | 1 |   0    |    Y     |  N   |       | 是否停用  |
|  13   | is_secrecy |   bit   | 1 |   0    |    Y     |  N   |       | 是否保密  |
|  14   | project_bg_id |   int   | 10 |   0    |    Y     |  N   |       | 事业群ID  |
|  15   | project_bg_name |   varchar   | 100 |   0    |    Y     |  N   |       | 事业群名称  |
|  16   | project_center_id |   varchar   | 50 |   0    |    Y     |  N   |       | 中心ID  |
|  17   | project_center_name |   varchar   | 100 |   0    |    Y     |  N   |       | 中心名字  |
|  18   | project_dept_id |   int   | 10 |   0    |    Y     |  N   |       | 机构ID  |
|  19   | project_dept_name |   varchar   | 100 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  20   | project_id |   varchar   | 100 |   0    |    Y     |  N   |       | 项目ID  |
|  21   | project_name |   varchar   | 100 |   0    |    Y     |  N   |       | 项目名称  |
|  22   | project_type |   int   | 10 |   0    |    Y     |  N   |       | 项目类型  |
|  23   | use_bk |   bit   | 1 |   0    |    Y     |  N   |       | 是否用蓝鲸  |

**表名：** <a id="role">role</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | description |   varchar   | 255 |   0    |    Y     |  N   |       | 描述  |
|  3   | name |   varchar   | 255 |   0    |    N     |  N   |       | 名称  |
|  4   | ch_name |   varchar   | 255 |   0    |    Y     |  N   |       | 分支名  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="role_permission">role_permission</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | 过期时间  |
|  3   | role_id |   int   | 10 |   0    |    Y     |  N   |       | 角色ID  |
|  4   | url_action_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="schema_version">schema_version</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | installed_rank |   int   | 10 |   0    |    N     |  Y   |       |   |
|  2   | version |   varchar   | 50 |   0    |    Y     |  N   |       | 版本号  |
|  3   | description |   varchar   | 200 |   0    |    N     |  N   |       | 描述  |
|  4   | type |   varchar   | 20 |   0    |    N     |  N   |       | 类型  |
|  5   | script |   varchar   | 1000 |   0    |    N     |  N   |       | 打包脚本  |
|  6   | checksum |   int   | 10 |   0    |    Y     |  N   |       | 校验和  |
|  7   | installed_by |   varchar   | 100 |   0    |    N     |  N   |       | 安装者  |
|  8   | installed_on |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 安装时间  |
|  9   | execution_time |   int   | 10 |   0    |    N     |  N   |       | 执行时间  |
|  10   | success |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |

**表名：** <a id="spring_session">spring_session</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | SESSION_ID |   char   | 36 |   0    |    N     |  Y   |       | SESSIONID  |
|  2   | CREATION_TIME |   bigint   | 20 |   0    |    N     |  N   |       | 创建时间  |
|  3   | LAST_ACCESS_TIME |   bigint   | 20 |   0    |    N     |  N   |       |   |
|  4   | MAX_INACTIVE_INTERVAL |   int   | 10 |   0    |    N     |  N   |       |   |
|  5   | PRINCIPAL_NAME |   varchar   | 100 |   0    |    Y     |  N   |       |   |

**表名：** <a id="SPRING_SESSION_ATTRIBUTES">SPRING_SESSION_ATTRIBUTES</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | SESSION_ID |   char   | 36 |   0    |    N     |  Y   |       | SESSIONID  |
|  2   | ATTRIBUTE_NAME |   varchar   | 200 |   0    |    N     |  Y   |       | 属性名称  |
|  3   | ATTRIBUTE_BYTES |   blob   | 65535 |   0    |    Y     |  N   |       | 属性字节  |

**表名：** <a id="t_user_token">t_user_token</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | user_Id |   varchar   | 255 |   0    |    N     |  Y   |       | 用户ID  |
|  2   | access_Token |   varchar   | 255 |   0    |    Y     |  N   |       | 权限Token  |
|  3   | expire_Time_Mills |   bigint   | 20 |   0    |    N     |  N   |       | 过期时间  |
|  4   | last_Access_Time_Mills |   bigint   | 20 |   0    |    N     |  N   |       | 最近鉴权时间  |
|  5   | refresh_Token |   varchar   | 255 |   0    |    Y     |  N   |       | 刷新token  |
|  6   | user_Type |   varchar   | 255 |   0    |    Y     |  N   |       | 用户类型  |

**表名：** <a id="url_action">url_action</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | action |   varchar   | 255 |   0    |    N     |  N   |       | 操作  |
|  3   | description |   varchar   | 255 |   0    |    Y     |  N   |       | 描述  |
|  4   | url |   varchar   | 255 |   0    |    N     |  N   |       | url地址  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="user">user</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | chname |   varchar   | 255 |   0    |    Y     |  N   |       |   |
|  3   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  4   | email |   varchar   | 255 |   0    |    Y     |  N   |       | email  |
|  5   | lang |   varchar   | 255 |   0    |    Y     |  N   |       | 语言  |
|  6   | last_login_time |   datetime   | 19 |   0    |    Y     |  N   |       | 最近登录时间  |
|  7   | phone |   varchar   | 255 |   0    |    Y     |  N   |       | 电话  |
|  8   | username |   varchar   | 255 |   0    |    N     |  N   |       | 用户名称  |

**表名：** <a id="user_permission">user_permission</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | 过期时间  |
|  3   | url_action_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  4   | user_id |   int   | 10 |   0    |    Y     |  N   |       | 用户ID  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |

**表名：** <a id="user_role">user_role</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | role_id |   int   | 10 |   0    |    Y     |  N   |       | 角色ID  |
|  3   | user_id |   int   | 10 |   0    |    Y     |  N   |       | 用户ID  |
|  4   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | 过期时间  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | 修改时间  |
