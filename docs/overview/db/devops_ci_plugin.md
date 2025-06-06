# 数据库设计文档

**数据库名：** devops_ci_plugin

**文档版本：** 1.0.9

**文档描述：** devops_ci_plugin 的数据库文档
| 表名                  | 说明       |
| :---: | :---: |
| T_AI_SCORE | 脚本执行报错 AI 分析-评分 |
| T_PLUGIN_GITHUB_CHECK |  |
| T_PLUGIN_GIT_CHECK |  |

**表名：** <a>T_AI_SCORE</a>

**说明：** 脚本执行报错 AI 分析-评分

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | LABEL |   varchar   | 256 |   0    |    N     |  N   |       | 任务 ID  |
|  3   | ARCHIVE |   bit   | 1 |   0    |    N     |  N   |   0    | 是否已归档  |
|  4   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  5   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  6   | GOOD_USERS |   text   | 65535 |   0    |    Y     |  N   |       | 赞的人  |
|  7   | BAD_USERS |   text   | 65535 |   0    |    Y     |  N   |       | 踩的人  |
|  8   | AI_MSG |   text   | 65535 |   0    |    Y     |  N   |       | 大模型生成的内容  |
|  9   | SYSTEM_MSG |   text   | 65535 |   0    |    Y     |  N   |       | Promptforsystem  |
|  10   | USER_MSG |   text   | 65535 |   0    |    Y     |  N   |       | Promptforuser  |

**表名：** <a>T_PLUGIN_GITHUB_CHECK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | BUILD_NUMBER |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  4   | REPO_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库 ID  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交 ID  |
|  6   | CHECK_RUN_ID |   bigint   | 20 |   0    |    N     |  N   |       |   |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  10   | CHECK_RUN_NAME |   varchar   | 64 |   0    |    Y     |  N   |       |   |

**表名：** <a>T_PLUGIN_GIT_CHECK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键 ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线 ID  |
|  3   | BUILD_NUMBER |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  4   | REPO_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库 ID  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交 ID  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  9   | CONTEXT |   varchar   | 255 |   0    |    Y     |  N   |       | 内容  |
|  10   | TARGET_BRANCH |   varchar   | 255 |   0    |    Y     |  N   |       | 目标分支  |
