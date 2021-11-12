# 数据库设计文档

**数据库名：** devops_ci_sign

**文档版本：** 1.0.0

**文档描述：** devops_ci_sign的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_SIGN_HISTORY](#T_SIGN_HISTORY) | 签名历史记录表 |
| [T_SIGN_IPA_INFO](#T_SIGN_IPA_INFO) | 签名任务信息表 |
| [T_SIGN_IPA_UPLOAD](#T_SIGN_IPA_UPLOAD) | 签名包上传记录表 |

**表名：** <a id="T_SIGN_HISTORY">T_SIGN_HISTORY</a>

**说明：** 签名历史记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | RESIGN_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 签名ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |   system    | 用户ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  4   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建ID  |
|  6   | TASK_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 任务ID  |
|  7   | TASK_EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |       | 任务执行计数  |
|  8   | ARCHIVE_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 归档类型  |
|  9   | ARCHIVE_PATH |   text   | 65535 |   0    |    Y     |  N   |       | 归档路径  |
|  10   | FILE_MD5 |   varchar   | 64 |   0    |    Y     |  N   |       | 文件MD5  |
|  11   | STATUS |   varchar   | 32 |   0    |    Y     |  N   |       | 状态  |
|  12   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | END_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 结束时间  |
|  14   | RESULT_FILE_MD5 |   varchar   | 64 |   0    |    Y     |  N   |       | 文件MD5  |
|  15   | RESULT_FILE_NAME |   varchar   | 512 |   0    |    Y     |  N   |       | 文件名称  |
|  16   | UPLOAD_FINISH_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 上传完成时间  |
|  17   | UNZIP_FINISH_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 解压完成时间  |
|  18   | RESIGN_FINISH_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 签名完成时间  |
|  19   | ZIP_FINISH_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 打包完成时间  |
|  20   | ARCHIVE_FINISH_TIME |   timestamp   | 19 |   0    |    Y     |  N   |       | 归档完成时间  |
|  21   | ERROR_MESSAGE |   text   | 65535 |   0    |    Y     |  N   |       | 错误信息  |

**表名：** <a id="T_SIGN_IPA_INFO">T_SIGN_IPA_INFO</a>

**说明：** 签名任务信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | RESIGN_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 签名ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | WILDCARD |   bit   | 1 |   0    |    N     |  N   |       | 是否采用通配符重签  |
|  4   | CERT_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 证书ID  |
|  5   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  6   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |
|  7   | BUILD_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建ID  |
|  8   | TASK_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 任务ID  |
|  9   | ARCHIVE_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 归档类型  |
|  10   | ARCHIVE_PATH |   text   | 65535 |   0    |    Y     |  N   |       | 归档路径  |
|  11   | MOBILE_PROVISION_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 移动设备ID  |
|  12   | UNIVERSAL_LINKS |   text   | 65535 |   0    |    Y     |  N   |       | UniversalLink的设置  |
|  13   | KEYCHAIN_ACCESS_GROUPS |   text   | 65535 |   0    |    Y     |  N   |       | 钥匙串访问组  |
|  14   | REPLACE_BUNDLE |   bit   | 1 |   0    |    Y     |  N   |       | 是否替换bundleId  |
|  15   | APPEX_SIGN_INFO |   text   | 65535 |   0    |    Y     |  N   |       | 拓展应用名和对应的描述文件ID  |
|  16   | FILENAME |   text   | 65535 |   0    |    Y     |  N   |       | 文件名称  |
|  17   | FILE_SIZE |   bigint   | 20 |   0    |    Y     |  N   |       | 文件大小  |
|  18   | FILE_MD5 |   varchar   | 64 |   0    |    Y     |  N   |       | 文件MD5  |
|  19   | REQUEST_CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 事件内容  |
|  20   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_SIGN_IPA_UPLOAD">T_SIGN_IPA_UPLOAD</a>

**说明：** 签名包上传记录表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | UPLOAD_TOKEN |   varchar   | 64 |   0    |    N     |  Y   |       | token  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户ID  |
|  3   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  4   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建ID  |
|  6   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | RESIGN_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 签名ID  |
