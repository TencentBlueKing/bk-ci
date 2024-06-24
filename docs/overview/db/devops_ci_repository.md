# 数据库设计文档

**数据库名：** devops_ci_repository

**文档版本：** 1.0.1

**文档描述：** devops_ci_repository 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_REPOSITORY | 代码库表 |
| T_REPOSITORY_CODE_GIT | 工蜂代码库明细表 |
| T_REPOSITORY_CODE_GITLAB | gitlab 代码库明细表 |
| T_REPOSITORY_CODE_P4 |  |
| T_REPOSITORY_CODE_SVN | svn 代码库明细表 |
| T_REPOSITORY_COMMIT | 代码库变更记录 |
| T_REPOSITORY_GITHUB | github 代码库明细表 |
| T_REPOSITORY_GITHUB_TOKEN | githuboauthtoken 表 |
| T_REPOSITORY_GIT_CHECK | 工蜂 oauthtoken 表 |
| T_REPOSITORY_GIT_TOKEN | 工蜂 commitchecker 表 |
| T_REPOSITORY_PIPELINE_REF | 流水线引用代码库表 |
| T_REPOSITORY_TGIT_TOKEN | 外网工蜂 OAUTHtoken 表 |
| T_REPOSITORY_WEBHOOK_REQUEST | 代码库 WEBHOOK 请求表 |

**表名：** <a>T_REPOSITORY</a>

**说明：** 代码库表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目 ID  |
|  3   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  4   | ALIAS_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 别名  |
|  5   | URL |   varchar   | 255 |   0    |    N     |  N   |       | url 地址  |
|  6   | TYPE |   varchar   | 20 |   0    |    N     |  N   |       | 类型  |
|  7   | REPOSITORY_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 哈希 ID  |
|  8   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 创建时间  |
|  9   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 修改时间  |
|  10   | IS_DELETED |   bit   | 1 |   0    |    N     |  N   |       | 是否删除 0 可用 1 删除  |
|  11   | UPDATED_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库最近修改人  |
|  12   | ATOM |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否为插件库(插件库不得修改和删除)  |
|  13   | ENABLE_PAC |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否开启 pac  |
|  14   | YAML_SYNC_STATUS |   varchar   | 10 |   0    |    Y     |  N   |       | pac 同步状态  |

**表名：** <a>T_REPOSITORY_CODE_GIT</a>

**说明：** 工蜂代码库明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 仓库 ID  |
|  2   | PROJECT_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 项目名称  |
|  3   | USER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 用户名称  |
|  4   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 创建时间  |
|  5   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 修改时间  |
|  6   | CREDENTIAL_ID |   varchar   | 64 |   0    |    N     |  N   |       | 凭据 ID  |
|  7   | AUTH_TYPE |   varchar   | 8 |   0    |    Y     |  N   |       | 认证方式  |
|  8   | GIT_PROJECT_ID |   bigint   | 20 |   0    |    Y     |  N   |   0    | GIT 项目 ID  |

**表名：** <a>T_REPOSITORY_CODE_GITLAB</a>

**说明：** gitlab 代码库明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 仓库 ID  |
|  2   | PROJECT_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 项目名称  |
|  3   | CREDENTIAL_ID |   varchar   | 64 |   0    |    N     |  N   |       | 凭据 ID  |
|  4   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 创建时间  |
|  5   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 修改时间  |
|  6   | USER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 用户名称  |
|  7   | AUTH_TYPE |   varchar   | 8 |   0    |    Y     |  N   |       | 凭证类型  |
|  8   | GIT_PROJECT_ID |   bigint   | 20 |   0    |    Y     |  N   |   0    | GIT 项目 ID  |

**表名：** <a>T_REPOSITORY_CODE_P4</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_NAME |   varchar   | 255 |   0    |    N     |  N   |       |   |
|  3   | USER_NAME |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  4   | CREDENTIAL_ID |   varchar   | 64 |   0    |    N     |  N   |       |   |
|  5   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |
|  6   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    |   |

**表名：** <a>T_REPOSITORY_CODE_SVN</a>

**说明：** svn 代码库明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 仓库 ID  |
|  2   | REGION |   varchar   | 255 |   0    |    N     |  N   |       | 地区  |
|  3   | PROJECT_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 项目名称  |
|  4   | USER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 用户名称  |
|  5   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 创建时间  |
|  6   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   2019-08-0100:00:00    | 修改时间  |
|  7   | CREDENTIAL_ID |   varchar   | 64 |   0    |    N     |  N   |       | 凭据 ID  |
|  8   | SVN_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 仓库类型  |

**表名：** <a>T_REPOSITORY_COMMIT</a>

**说明：** 代码库变更记录

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建 ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线 ID  |
|  4   | REPO_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 代码库 ID  |
|  5   | TYPE |   smallint   | 6 |   0    |    Y     |  N   |       | 1-svn,2-git,3-gitlab  |
|  6   | COMMIT |   varchar   | 64 |   0    |    Y     |  N   |       | 提交  |
|  7   | COMMITTER |   varchar   | 32 |   0    |    Y     |  N   |       | 提交者  |
|  8   | COMMIT_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 提交时间  |
|  9   | COMMENT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 评论  |
|  10   | ELEMENT_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 原子 ID  |
|  11   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  12   | URL |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库 URL  |

**表名：** <a>T_REPOSITORY_GITHUB</a>

**说明：** github 代码库明细表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  Y   |       | 仓库 ID  |
|  2   | CREDENTIAL_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 凭据 ID  |
|  3   | PROJECT_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 项目名称  |
|  4   | USER_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 用户名称  |
|  5   | CREATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATED_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  7   | GIT_PROJECT_ID |   bigint   | 20 |   0    |    Y     |  N   |   0    | GIT 项目 ID  |

**表名：** <a>T_REPOSITORY_GITHUB_TOKEN</a>

**说明：** githuboauthtoken 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 用户 ID  |
|  3   | ACCESS_TOKEN |   varchar   | 96 |   0    |    N     |  N   |       | 权限 Token  |
|  4   | TOKEN_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | token 类型  |
|  5   | SCOPE |   text   | 65535 |   0    |    N     |  N   |       | 生效范围  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | TYPE |   varchar   | 32 |   0    |    Y     |  N   |   GITHUB_APP    | GitHubtoken 类型（GITHUB_APP、OAUTH_APP）  |

**表名：** <a>T_REPOSITORY_GIT_CHECK</a>

**说明：** 工蜂 oauthtoken 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | BUILD_NUMBER |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  4   | REPO_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库 ID  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交 ID  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  9   | CONTEXT |   varchar   | 255 |   0    |    Y     |  N   |       | 内容  |
|  10   | SOURCE |   varchar   | 64 |   0    |    N     |  N   |       | 事件来源  |
|  11   | TARGET_BRANCH |   varchar   | 1024 |   0    |    N     |  N   |       | 目标分支  |

**表名：** <a>T_REPOSITORY_GIT_TOKEN</a>

**说明：** 工蜂 commitchecker 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 用户 ID  |
|  3   | ACCESS_TOKEN |   varchar   | 96 |   0    |    Y     |  N   |       | 权限 Token  |
|  4   | REFRESH_TOKEN |   varchar   | 96 |   0    |    Y     |  N   |       | 刷新 token  |
|  5   | TOKEN_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | token 类型  |
|  6   | EXPIRES_IN |   bigint   | 20 |   0    |    Y     |  N   |       | 过期时间  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | token 的创建时间  |

**表名：** <a>T_REPOSITORY_PIPELINE_REF</a>

**说明：** 流水线引用代码库表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       |   |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 蓝盾项目 ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  4   | PIPELINE_NAME |   varchar   | 255 |   0    |    N     |  N   |       | 流水线名称  |
|  5   | REPOSITORY_ID |   bigint   | 20 |   0    |    N     |  N   |       | 代码库 ID  |
|  6   | TASK_ID |   varchar   | 64 |   0    |    N     |  N   |       | 任务 ID  |
|  7   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 原子名称，用户是可以修改  |
|  8   | ATOM_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  9   | ATOM_CATEGORY |   varchar   | 10 |   0    |    N     |  N   |       | 插件类别  |
|  10   | TASK_PARAMS |   text   | 65535 |   0    |    N     |  N   |       | 插件参数  |
|  11   | TASK_REPO_TYPE |   varchar   | 10 |   0    |    N     |  N   |       | 插件代码库类型配置  |
|  12   | TASK_REPO_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 插件代码库 hashId 配置  |
|  13   | TASK_REPO_NAME |   varchar   | 255 |   0    |    Y     |  N   |       | 插件代码库别名配置  |
|  14   | TRIGGER_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 触发类型  |
|  15   | EVENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 事件类型  |
|  16   | TRIGGER_CONDITION |   text   | 65535 |   0    |    Y     |  N   |       | 触发条件  |
|  17   | TRIGGER_CONDITION_MD5 |   varchar   | 64 |   0    |    Y     |  N   |       | 触发条件 md5  |
|  18   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  19   | UPDATE_TIME |   timestamp   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  20   | CHANNEL |   varchar   | 32 |   0    |    Y     |  N   |   BS    | 流水线渠道  |

**表名：** <a>T_REPOSITORY_TGIT_TOKEN</a>

**说明：** 外网工蜂 OAUTHtoken 表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | USER_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 用户 ID  |
|  3   | ACCESS_TOKEN |   varchar   | 96 |   0    |    Y     |  N   |       | 权限 Token  |
|  4   | REFRESH_TOKEN |   varchar   | 96 |   0    |    Y     |  N   |       | 刷新 token  |
|  5   | TOKEN_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | token 类型  |
|  6   | EXPIRES_IN |   bigint   | 20 |   0    |    Y     |  N   |       | 过期时间  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |   CURRENT_TIMESTAMP    | token 的创建时间  |
|  8   | OAUTH_USER_ID |   varchar   | 64 |   0    |    N     |  N   |       | 账户实际名称  |

**表名：** <a>T_REPOSITORY_WEBHOOK_REQUEST</a>

**说明：** 代码库 WEBHOOK 请求表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | REQUEST_ID |   varchar   | 64 |   0    |    N     |  Y   |       | 请求 ID  |
|  2   | EXTERNAL_ID |   varchar   | 255 |   0    |    Y     |  N   |       | 代码库平台 ID  |
|  3   | REPOSITORY_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 触发类型  |
|  4   | EVENT_TYPE |   varchar   | 255 |   0    |    Y     |  N   |       | 事件类型  |
|  5   | TRIGGER_USER |   varchar   | 100 |   0    |    N     |  N   |       | 触发用户  |
|  6   | EVENT_MESSAGE |   text   | 65535 |   0    |    N     |  N   |       | 事件信息  |
|  7   | REQUEST_HEADER |   text   | 65535 |   0    |    Y     |  N   |       | 事件请求头  |
|  8   | REQUEST_PARAM |   text   | 65535 |   0    |    Y     |  N   |       | 事件请求参数  |
|  9   | REQUEST_BODY |   text   | 65535 |   0    |    Y     |  N   |       | 事件请求体  |
|  10   | CREATE_TIME |   timestamp   | 19 |   0    |    N     |  Y   |   CURRENT_TIMESTAMP    | 创建时间  |
