# 数据库设计文档

**数据库名：** devops_ci_ticket

**文档版本：** 1.0.1

**文档描述：** devops_ci_ticket的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_CERT](#T_CERT) | 凭证信息表 |
| [T_CERT_ENTERPRISE](#T_CERT_ENTERPRISE) | 企业证书表 |
| [T_CERT_TLS](#T_CERT_TLS) | TLS证书表 |
| [T_CREDENTIAL](#T_CREDENTIAL) | 凭证表 |

**表名：** <a id="T_CERT">T_CERT</a>

**说明：** 凭证信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | CERT_ID |   varchar   | 128 |   0    |    N     |  Y   |       | 证书ID  |
|  3   | CERT_USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 证书用户ID  |
|  4   | CERT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 证书类型  |
|  5   | CERT_REMARK |   varchar   | 128 |   0    |    N     |  N   |       | 证书备注  |
|  6   | CERT_P12_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书p12文件名称  |
|  7   | CERT_P12_FILE_CONTENT |   blob   | 65535 |   0    |    N     |  N   |       | 证书p12文件内容  |
|  8   | CERT_MP_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书mp文件名称  |
|  9   | CERT_MP_FILE_CONTENT |   blob   | 65535 |   0    |    N     |  N   |       | 证书mp文件内容  |
|  10   | CERT_JKS_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书jks文件名称  |
|  11   | CERT_JKS_FILE_CONTENT |   blob   | 65535 |   0    |    N     |  N   |       | 证书jsk文件内容  |
|  12   | CERT_JKS_ALIAS |   varchar   | 128 |   0    |    Y     |  N   |       | 证书jsk别名  |
|  13   | CERT_JKS_ALIAS_CREDENTIAL_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 证书jks凭据ID  |
|  14   | CERT_DEVELOPER_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书开发者名称  |
|  15   | CERT_TEAM_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书团队名称  |
|  16   | CERT_UUID |   varchar   | 64 |   0    |    N     |  N   |       | 证书uuid  |
|  17   | CERT_EXPIRE_DATE |   datetime   | 19 |   0    |    Y     |  N   |       | 证书过期时间  |
|  18   | CERT_CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 证书创建时间  |
|  19   | CERT_UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 证书更新时间  |
|  20   | CREDENTIAL_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 凭证ID  |

**表名：** <a id="T_CERT_ENTERPRISE">T_CERT_ENTERPRISE</a>

**说明：** 企业证书表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | CERT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 证书ID  |
|  3   | CERT_MP_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书mp文件名称  |
|  4   | CERT_MP_FILE_CONTENT |   blob   | 65535 |   0    |    N     |  N   |       | 证书mp文件内容  |
|  5   | CERT_DEVELOPER_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书开发者名称  |
|  6   | CERT_TEAM_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 证书团队名称  |
|  7   | CERT_UUID |   varchar   | 64 |   0    |    N     |  N   |       | 证书uuid  |
|  8   | CERT_UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  9   | CERT_EXPIRE_DATE |   datetime   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 证书过期时间  |
|  10   | CERT_CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 证书创建时间  |

**表名：** <a id="T_CERT_TLS">T_CERT_TLS</a>

**说明：** TLS证书表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | CERT_ID |   varchar   | 32 |   0    |    N     |  Y   |       | 证书ID  |
|  3   | CERT_SERVER_CRT_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 服务器crt证书名  |
|  4   | CERT_SERVER_CRT_FILE |   blob   | 65535 |   0    |    N     |  N   |       | Base64编码的加密后的证书内容  |
|  5   | CERT_SERVER_KEY_FILE_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 服务器key证书名  |
|  6   | CERT_SERVER_KEY_FILE |   blob   | 65535 |   0    |    N     |  N   |       | Base64编码的加密后的证书内容  |
|  7   | CERT_CLIENT_CRT_FILE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 客户端crt证书名  |
|  8   | CERT_CLIENT_CRT_FILE |   blob   | 65535 |   0    |    Y     |  N   |       | Base64编码的加密后的证书内容  |
|  9   | CERT_CLIENT_KEY_FILE_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 客户端key证书名  |
|  10   | CERT_CLIENT_KEY_FILE |   blob   | 65535 |   0    |    Y     |  N   |       | Base64编码的加密后的证书内容  |
|  11   | CERT_CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 证书创建时间  |
|  12   | CERT_UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 证书更新时间  |

**表名：** <a id="T_CREDENTIAL">T_CREDENTIAL</a>

**说明：** 凭证表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 项目ID  |
|  2   | CREDENTIAL_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 凭据ID  |
|  3   | CREDENTIAL_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 凭据名称  |
|  4   | CREDENTIAL_USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 凭据用户ID  |
|  5   | CREDENTIAL_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 凭据类型  |
|  6   | CREDENTIAL_REMARK |   text   | 65535 |   0    |    Y     |  N   |       | 凭据备注  |
|  7   | CREDENTIAL_V1 |   text   | 65535 |   0    |    N     |  N   |       | 凭据内容  |
|  8   | CREDENTIAL_V2 |   text   | 65535 |   0    |    Y     |  N   |       | 凭据内容  |
|  9   | CREDENTIAL_V3 |   text   | 65535 |   0    |    Y     |  N   |       | 凭据内容  |
|  10   | CREDENTIAL_V4 |   text   | 65535 |   0    |    Y     |  N   |       | 凭据内容  |
|  11   | CREATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | UPDATED_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  13   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  14   | ALLOW_ACROSS_PROJECT |   bit   | 1 |   0    |    N     |  N   |   b'0'    |   |
