# 数据库设计文档

**数据库名：** devops_ci_op

**文档版本：** 1.0.1

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
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  3   | dept_id |   int   | 10 |   0    |    N     |  N   |       | é¡¹ç›®æ‰€å±žäºŒçº§æœºæž„ID  |
|  4   | dept_name |   varchar   | 100 |   0    |    N     |  N   |       | é¡¹ç›®æ‰€å±žäºŒçº§æœºæž„åç§°  |
|  5   | level |   int   | 10 |   0    |    N     |  N   |       | å±‚çº§ID  |
|  6   | parent_dept_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | æ›´æ–°æ—¶é—´  |

**表名：** <a id="project_info">project_info</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | approval_status |   int   | 10 |   0    |    Y     |  N   |       | å®¡æ ¸çŠ¶æ€  |
|  3   | approval_time |   datetime   | 19 |   0    |    Y     |  N   |       | æ‰¹å‡†æ—¶é—´  |
|  4   | approver |   varchar   | 100 |   0    |    Y     |  N   |       | æ‰¹å‡†äºº  |
|  5   | cc_app_id |   int   | 10 |   0    |    Y     |  N   |       | åº”ç”¨ID  |
|  6   | created_at |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  7   | creator |   varchar   | 100 |   0    |    Y     |  N   |       | åˆ›å»ºè€…  |
|  8   | creator_bg_name |   varchar   | 100 |   0    |    Y     |  N   |       | åˆ›å»ºè€…äº‹ä¸šç¾¤åç§°  |
|  9   | creator_center_name |   varchar   | 100 |   0    |    Y     |  N   |       | åˆ›å»ºè€…ä¸­å¿ƒåå­—  |
|  10   | creator_dept_name |   varchar   | 100 |   0    |    Y     |  N   |       | åˆ›å»ºè€…é¡¹ç›®æ‰€å±žäºŒçº§æœºæž„åç§°  |
|  11   | english_name |   varchar   | 255 |   0    |    Y     |  N   |       | è‹±æ–‡åç§°  |
|  12   | is_offlined |   bit   | 1 |   0    |    Y     |  N   |       | æ˜¯å¦åœç”¨  |
|  13   | is_secrecy |   bit   | 1 |   0    |    Y     |  N   |       | æ˜¯å¦ä¿å¯†  |
|  14   | project_bg_id |   int   | 10 |   0    |    Y     |  N   |       | äº‹ä¸šç¾¤ID  |
|  15   | project_bg_name |   varchar   | 100 |   0    |    Y     |  N   |       | äº‹ä¸šç¾¤åç§°  |
|  16   | project_center_id |   varchar   | 50 |   0    |    Y     |  N   |       | ä¸­å¿ƒID  |
|  17   | project_center_name |   varchar   | 100 |   0    |    Y     |  N   |       | ä¸­å¿ƒåå­—  |
|  18   | project_dept_id |   int   | 10 |   0    |    Y     |  N   |       | æœºæž„ID  |
|  19   | project_dept_name |   varchar   | 100 |   0    |    Y     |  N   |       | é¡¹ç›®æ‰€å±žäºŒçº§æœºæž„åç§°  |
|  20   | project_id |   varchar   | 100 |   0    |    Y     |  N   |       | é¡¹ç›®ID  |
|  21   | project_name |   varchar   | 100 |   0    |    Y     |  N   |       | é¡¹ç›®åç§°  |
|  22   | project_type |   int   | 10 |   0    |    Y     |  N   |       | é¡¹ç›®ç±»åž‹  |
|  23   | use_bk |   bit   | 1 |   0    |    Y     |  N   |       | æ˜¯å¦ç”¨è“é²¸  |

**表名：** <a id="role">role</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | description |   varchar   | 255 |   0    |    Y     |  N   |       | æè¿°  |
|  3   | name |   varchar   | 255 |   0    |    N     |  N   |       | åç§°  |
|  4   | ch_name |   varchar   | 255 |   0    |    Y     |  N   |       | åˆ†æ”¯å  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | ä¿®æ”¹æ—¶é—´  |

**表名：** <a id="role_permission">role_permission</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | è¿‡æœŸæ—¶é—´  |
|  3   | role_id |   int   | 10 |   0    |    Y     |  N   |       | è§’è‰²ID  |
|  4   | url_action_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | ä¿®æ”¹æ—¶é—´  |

**表名：** <a id="schema_version">schema_version</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | installed_rank |   int   | 10 |   0    |    N     |  Y   |       |   |
|  2   | version |   varchar   | 50 |   0    |    Y     |  N   |       | ç‰ˆæœ¬å·  |
|  3   | description |   varchar   | 200 |   0    |    N     |  N   |       | æè¿°  |
|  4   | type |   varchar   | 20 |   0    |    N     |  N   |       | ç±»åž‹  |
|  5   | script |   varchar   | 1000 |   0    |    N     |  N   |       | æ‰“åŒ…è„šæœ¬  |
|  6   | checksum |   int   | 10 |   0    |    Y     |  N   |       | æ ¡éªŒå’Œ  |
|  7   | installed_by |   varchar   | 100 |   0    |    N     |  N   |       | å®‰è£…è€…  |
|  8   | installed_on |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | å®‰è£…æ—¶é—´  |
|  9   | execution_time |   int   | 10 |   0    |    N     |  N   |       | æ‰§è¡Œæ—¶é—´  |
|  10   | success |   bit   | 1 |   0    |    N     |  N   |       | æ˜¯å¦æˆåŠŸ  |

**表名：** <a id="spring_session">spring_session</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | SESSION_ID |   char   | 36 |   0    |    N     |  Y   |       | SESSIONID  |
|  2   | CREATION_TIME |   bigint   | 20 |   0    |    N     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  3   | LAST_ACCESS_TIME |   bigint   | 20 |   0    |    N     |  N   |       |   |
|  4   | MAX_INACTIVE_INTERVAL |   int   | 10 |   0    |    N     |  N   |       |   |
|  5   | PRINCIPAL_NAME |   varchar   | 100 |   0    |    Y     |  N   |       |   |

**表名：** <a id="SPRING_SESSION_ATTRIBUTES">SPRING_SESSION_ATTRIBUTES</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | SESSION_ID |   char   | 36 |   0    |    N     |  Y   |       | SESSIONID  |
|  2   | ATTRIBUTE_NAME |   varchar   | 200 |   0    |    N     |  Y   |       | å±žæ€§åç§°  |
|  3   | ATTRIBUTE_BYTES |   blob   | 65535 |   0    |    Y     |  N   |       | å±žæ€§å­—èŠ‚  |

**表名：** <a id="t_user_token">t_user_token</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | user_Id |   varchar   | 255 |   0    |    N     |  Y   |       | ç”¨æˆ·ID  |
|  2   | access_Token |   varchar   | 255 |   0    |    Y     |  N   |       | æƒé™Token  |
|  3   | expire_Time_Mills |   bigint   | 20 |   0    |    N     |  N   |       | è¿‡æœŸæ—¶é—´  |
|  4   | last_Access_Time_Mills |   bigint   | 20 |   0    |    N     |  N   |       | æœ€è¿‘é‰´æƒæ—¶é—´  |
|  5   | refresh_Token |   varchar   | 255 |   0    |    Y     |  N   |       | åˆ·æ–°token  |
|  6   | user_Type |   varchar   | 255 |   0    |    Y     |  N   |       | ç”¨æˆ·ç±»åž‹  |

**表名：** <a id="url_action">url_action</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | action |   varchar   | 255 |   0    |    N     |  N   |       | æ“ä½œ  |
|  3   | description |   varchar   | 255 |   0    |    Y     |  N   |       | æè¿°  |
|  4   | url |   varchar   | 255 |   0    |    N     |  N   |       | urlåœ°å€  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | ä¿®æ”¹æ—¶é—´  |

**表名：** <a id="user">user</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | chname |   varchar   | 255 |   0    |    Y     |  N   |       |   |
|  3   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  4   | email |   varchar   | 255 |   0    |    Y     |  N   |       | email  |
|  5   | lang |   varchar   | 255 |   0    |    Y     |  N   |       | è¯­è¨€  |
|  6   | last_login_time |   datetime   | 19 |   0    |    Y     |  N   |       | æœ€è¿‘ç™»å½•æ—¶é—´  |
|  7   | phone |   varchar   | 255 |   0    |    Y     |  N   |       | ç”µè¯  |
|  8   | username |   varchar   | 255 |   0    |    N     |  N   |       | ç”¨æˆ·åç§°  |

**表名：** <a id="user_permission">user_permission</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | è¿‡æœŸæ—¶é—´  |
|  3   | url_action_id |   int   | 10 |   0    |    Y     |  N   |       |   |
|  4   | user_id |   int   | 10 |   0    |    Y     |  N   |       | ç”¨æˆ·ID  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | ä¿®æ”¹æ—¶é—´  |

**表名：** <a id="user_role">user_role</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | ä¸»é”®ID  |
|  2   | role_id |   int   | 10 |   0    |    Y     |  N   |       | è§’è‰²ID  |
|  3   | user_id |   int   | 10 |   0    |    Y     |  N   |       | ç”¨æˆ·ID  |
|  4   | expire_time |   datetime   | 19 |   0    |    Y     |  N   |       | è¿‡æœŸæ—¶é—´  |
|  5   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | åˆ›å»ºæ—¶é—´  |
|  6   | modify_time |   datetime   | 19 |   0    |    Y     |  N   |       | ä¿®æ”¹æ—¶é—´  |
