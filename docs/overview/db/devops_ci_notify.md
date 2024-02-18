# 数据库设计文档

**数据库名：** devops_ci_notify

**文档版本：** 1.0.1

**文档描述：** devops_ci_notify的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_COMMON_NOTIFY_MESSAGE_TEMPLATE](#T_COMMON_NOTIFY_MESSAGE_TEMPLATE) | 基础模板表 |
| [T_EMAILS_NOTIFY_MESSAGE_TEMPLATE](#T_EMAILS_NOTIFY_MESSAGE_TEMPLATE) | email模板表 |
| [T_NOTIFY_EMAIL](#T_NOTIFY_EMAIL) |  |
| [T_NOTIFY_RTX](#T_NOTIFY_RTX) | rtx流水表 |
| [T_NOTIFY_SMS](#T_NOTIFY_SMS) |  |
| [T_NOTIFY_VOICE](#T_NOTIFY_VOICE) | 语音流水表 |
| [T_NOTIFY_WECHAT](#T_NOTIFY_WECHAT) | 微信流水表 |
| [T_NOTIFY_WEWORK](#T_NOTIFY_WEWORK) | 企业微信流水表 |
| [T_RTX_NOTIFY_MESSAGE_TEMPLATE](#T_RTX_NOTIFY_MESSAGE_TEMPLATE) | rtx模板表 |
| [T_VOICE_NOTIFY_MESSAGE_TEMPLATE](#T_VOICE_NOTIFY_MESSAGE_TEMPLATE) | 语音模板表 |
| [T_WECHAT_NOTIFY_MESSAGE_TEMPLATE](#T_WECHAT_NOTIFY_MESSAGE_TEMPLATE) | wechat模板表 |
| [T_WEWORK_GROUP_NOTIFY_MESSAGE_TEMPLATE](#T_WEWORK_GROUP_NOTIFY_MESSAGE_TEMPLATE) | 企业微信群模板表 |
| [T_WEWORK_NOTIFY_MESSAGE_TEMPLATE](#T_WEWORK_NOTIFY_MESSAGE_TEMPLATE) | wework模板表 |

**表名：** <a id="T_COMMON_NOTIFY_MESSAGE_TEMPLATE">T_COMMON_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** 基础模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TEMPLATE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 模板代码  |
|  3   | TEMPLATE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 模板名称  |
|  4   | NOTIFY_TYPE_SCOPE |   varchar   | 64 |   0    |    N     |  N   |       | 适用的通知类型（EMAIL:邮件RTX:企业微信WECHAT:微信SMS:短信）  |
|  5   | PRIORITY |   tinyint   | 4 |   0    |    N     |  N   |       | 优先级  |
|  6   | SOURCE |   tinyint   | 4 |   0    |    N     |  N   |       | 邮件来源  |

**表名：** <a id="T_EMAILS_NOTIFY_MESSAGE_TEMPLATE">T_EMAILS_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** email模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | SENDER |   varchar   | 128 |   0    |    N     |  N   |   DevOps    | 邮件发送者  |
|  6   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 邮件标题  |
|  7   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 邮件内容  |
|  8   | BODY_FORMAT |   tinyint   | 4 |   0    |    N     |  N   |       | 邮件格式（0:文本1:html网页）  |
|  9   | EMAIL_TYPE |   tinyint   | 4 |   0    |    N     |  N   |       | 邮件类型（0:外部邮件1:内部邮件）  |
|  10   | TENCENT_CLOUD_TEMPLATE_ID |   int   | 10 |   0    |    Y     |  N   |       | 腾讯云邮件模板id  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_NOTIFY_EMAIL">T_NOTIFY_EMAIL</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  3   | SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件来源  |
|  4   | SENDER |   varchar   | 255 |   0    |    N     |  N   |       | 邮件发送者  |
|  5   | TO |   text   | 65535 |   0    |    N     |  N   |       | 邮件接收者  |
|  6   | TITLE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件标题  |
|  7   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 邮件内容  |
|  8   | PRIORITY |   int   | 10 |   0    |    N     |  N   |       | 优先级  |
|  9   | RETRY_COUNT |   int   | 10 |   0    |    N     |  N   |       | 重试次数  |
|  10   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  11   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  13   | CC |   text   | 65535 |   0    |    Y     |  N   |       | 邮件抄送接收者  |
|  14   | BCC |   text   | 65535 |   0    |    Y     |  N   |       | 邮件密送接收者  |
|  15   | FORMAT |   int   | 10 |   0    |    N     |  N   |       | 格式  |
|  16   | TYPE |   int   | 10 |   0    |    N     |  N   |       | 类型  |
|  17   | CONTENT_MD5 |   varchar   | 32 |   0    |    N     |  N   |       | 内容md5值，由title和body计算得，频率限制时使用  |
|  18   | FREQUENCY_LIMIT |   int   | 10 |   0    |    Y     |  N   |       | 频率限制时长，单位分钟，即n分钟内不重发成功的消息  |
|  19   | TOF_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | tof系统id  |
|  20   | FROM_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | 发送消息的系统id  |
|  21   | DelaySeconds |   int   | 10 |   0    |    Y     |  N   |       | 延迟发送的时间，秒  |

**表名：** <a id="T_NOTIFY_RTX">T_NOTIFY_RTX</a>

**说明：** rtx流水表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | BATCH_ID |   varchar   | 32 |   0    |    N     |  N   |       | RTX通知批次ID  |
|  3   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  4   | SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件来源  |
|  5   | SENDER |   varchar   | 255 |   0    |    N     |  N   |       | 邮件发送者  |
|  6   | RECEIVERS |   text   | 65535 |   0    |    N     |  N   |       | 通知接收者  |
|  7   | TITLE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件标题  |
|  8   | BODY |   text   | 65535 |   0    |    N     |  N   |       | 邮件内容  |
|  9   | PRIORITY |   int   | 10 |   0    |    N     |  N   |       | 优先级  |
|  10   | RETRY_COUNT |   int   | 10 |   0    |    N     |  N   |       | 重试次数  |
|  11   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  12   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  13   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  14   | CONTENT_MD5 |   varchar   | 32 |   0    |    N     |  N   |       | 内容md5值，由title和body计算得，频率限制时使用  |
|  15   | FREQUENCY_LIMIT |   int   | 10 |   0    |    Y     |  N   |       | 频率限制时长，单位分钟，即n分钟内不重发成功的消息  |
|  16   | TOF_SYS_id |   varchar   | 20 |   0    |    Y     |  N   |       | tof系统id  |
|  17   | FROM_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | 发送消息的系统id  |
|  18   | DelaySeconds |   int   | 10 |   0    |    Y     |  N   |       | 延迟发送的时间，秒  |

**表名：** <a id="T_NOTIFY_SMS">T_NOTIFY_SMS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  3   | SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件来源  |
|  4   | SENDER |   varchar   | 255 |   0    |    N     |  N   |       | 邮件发送者  |
|  5   | RECEIVERS |   text   | 65535 |   0    |    N     |  N   |       | 通知接收者  |
|  6   | BODY |   text   | 65535 |   0    |    N     |  N   |       | 邮件内容  |
|  7   | PRIORITY |   int   | 10 |   0    |    N     |  N   |       | 优先级  |
|  8   | RETRY_COUNT |   int   | 10 |   0    |    N     |  N   |       | 重试次数  |
|  9   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  10   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  11   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  12   | BATCH_ID |   varchar   | 32 |   0    |    N     |  N   |       | 通知批次ID  |
|  13   | T_NOTIFY_SMScol |   varchar   | 45 |   0    |    Y     |  N   |       |   |
|  14   | CONTENT_MD5 |   varchar   | 32 |   0    |    N     |  N   |       | 内容md5值，由title和body计算得，频率限制时使用  |
|  15   | FREQUENCY_LIMIT |   int   | 10 |   0    |    Y     |  N   |       | 频率限制时长，单位分钟，即n分钟内不重发成功的消息  |
|  16   | TOF_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | tof系统id  |
|  17   | FROM_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | 发送消息的系统id  |
|  18   | DelaySeconds |   int   | 10 |   0    |    Y     |  N   |       | 延迟发送的时间，秒  |

**表名：** <a id="T_NOTIFY_VOICE">T_NOTIFY_VOICE</a>

**说明：** 语音流水表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  3   | RECEIVERS |   text   | 65535 |   0    |    N     |  N   |       | 语音接收者  |
|  4   | TASK_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 任务名称  |
|  5   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 呼叫内容  |
|  6   | TRANSFER_RECEIVER |   varchar   | 50 |   0    |    N     |  N   |       | 转接责任人  |
|  7   | RETRY_COUNT |   int   | 10 |   0    |    N     |  N   |       | 重试次数  |
|  8   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  9   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  10   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  11   | TOF_SYS_id |   varchar   | 20 |   0    |    Y     |  N   |       | tof系统id  |
|  12   | FROM_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | 发送消息的系统id  |

**表名：** <a id="T_NOTIFY_WECHAT">T_NOTIFY_WECHAT</a>

**说明：** 微信流水表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  3   | SOURCE |   varchar   | 255 |   0    |    N     |  N   |       | 邮件来源  |
|  4   | SENDER |   varchar   | 255 |   0    |    N     |  N   |       | 邮件发送者  |
|  5   | RECEIVERS |   text   | 65535 |   0    |    N     |  N   |       | 通知接收者  |
|  6   | BODY |   text   | 65535 |   0    |    N     |  N   |       | 邮件内容  |
|  7   | PRIORITY |   int   | 10 |   0    |    N     |  N   |       | 优先级  |
|  8   | RETRY_COUNT |   int   | 10 |   0    |    N     |  N   |       | 重试次数  |
|  9   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  10   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  11   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  12   | CONTENT_MD5 |   varchar   | 32 |   0    |    N     |  N   |       | 内容md5值，由title和body计算得，频率限制时使用  |
|  13   | FREQUENCY_LIMIT |   int   | 10 |   0    |    Y     |  N   |       | 频率限制时长，单位分钟，即n分钟内不重发成功的消息  |
|  14   | TOF_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | tof系统id  |
|  15   | FROM_SYS_ID |   varchar   | 20 |   0    |    Y     |  N   |       | 发送消息的系统id  |
|  16   | DelaySeconds |   int   | 10 |   0    |    Y     |  N   |       | 延迟发送的时间，秒  |

**表名：** <a id="T_NOTIFY_WEWORK">T_NOTIFY_WEWORK</a>

**说明：** 企业微信流水表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | SUCCESS |   bit   | 1 |   0    |    N     |  N   |       | 是否成功  |
|  3   | RECEIVERS |   text   | 65535 |   0    |    N     |  N   |       | 通知接收者  |
|  4   | BODY |   text   | 65535 |   0    |    N     |  N   |       | 邮件内容  |
|  5   | LAST_ERROR |   text   | 65535 |   0    |    Y     |  N   |       | 最后错误内容  |
|  6   | CREATED_TIME |   datetime   | 26 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP(6)    | 创建时间  |
|  7   | UPDATED_TIME |   datetime   | 26 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP(6)    | 更新时间  |

**表名：** <a id="T_RTX_NOTIFY_MESSAGE_TEMPLATE">T_RTX_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** rtx模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | SENDER |   varchar   | 128 |   0    |    N     |  N   |   DevOps    | 邮件发送者  |
|  6   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 邮件标题  |
|  7   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 邮件内容  |
|  8   | BODY_MD |   mediumtext   | 16777215 |   0    |    Y     |  N   |       | markdown格式内容  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_VOICE_NOTIFY_MESSAGE_TEMPLATE">T_VOICE_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** 语音模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | TASK_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 任务名称  |
|  6   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 语音内容  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_WECHAT_NOTIFY_MESSAGE_TEMPLATE">T_WECHAT_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** wechat模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | SENDER |   varchar   | 128 |   0    |    N     |  N   |   DevOps    | 邮件发送者  |
|  6   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 邮件标题  |
|  7   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 邮件内容  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_WEWORK_GROUP_NOTIFY_MESSAGE_TEMPLATE">T_WEWORK_GROUP_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** 企业微信群模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 邮件标题  |
|  6   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 内容  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_WEWORK_NOTIFY_MESSAGE_TEMPLATE">T_WEWORK_NOTIFY_MESSAGE_TEMPLATE</a>

**说明：** wework模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMON_TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  4   | MODIFIOR |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  5   | SENDER |   varchar   | 128 |   0    |    N     |  N   |   DevOps    | 邮件发送者  |
|  6   | TITLE |   varchar   | 256 |   0    |    Y     |  N   |       | 邮件标题  |
|  7   | BODY |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 邮件内容  |
|  8   | CREATE_TIME |   datetime   | 26 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(6)    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 26 |   0    |    Y     |  N   |       | 更新时间  |
