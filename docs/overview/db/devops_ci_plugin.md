# 数据库设计文档

**数据库名：** devops_ci_plugin

**文档版本：** 1.0.0

**文档描述：** devops_ci_plugin的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_PLUGIN_CODECC](#T_PLUGIN_CODECC) |  |
| [T_PLUGIN_CODECC_ELEMENT](#T_PLUGIN_CODECC_ELEMENT) |  |
| [T_PLUGIN_GITHUB_CHECK](#T_PLUGIN_GITHUB_CHECK) |  |
| [T_PLUGIN_GIT_CHECK](#T_PLUGIN_GIT_CHECK) |  |

**表名：** <a id="T_PLUGIN_CODECC">T_PLUGIN_CODECC</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 构建ID  |
|  5   | TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 任务ID  |
|  6   | TOOL_SNAPSHOT_LIST |   longtext   | 2147483647 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_PLUGIN_CODECC_ELEMENT">T_PLUGIN_CODECC_ELEMENT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 流水线ID  |
|  4   | TASK_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 任务名称  |
|  5   | TASK_CN_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 任务中文名称  |
|  6   | TASK_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 任务ID  |
|  7   | IS_SYNC |   varchar   | 6 |   0    |    Y     |  N   |       | 是否是同步  |
|  8   | SCAN_TYPE |   varchar   | 6 |   0    |    Y     |  N   |       | 扫描类型（0：全量,1：增量）  |
|  9   | LANGUAGE |   varchar   | 1024 |   0    |    Y     |  N   |       | 工程语言  |
|  10   | PLATFORM |   varchar   | 16 |   0    |    Y     |  N   |       | codecc原子执行环境，例如WINDOWS，LINUX，MACOS等  |
|  11   | TOOLS |   varchar   | 1024 |   0    |    Y     |  N   |       | 扫描工具  |
|  12   | PY_VERSION |   varchar   | 16 |   0    |    Y     |  N   |       | 其中“py2”表示使用python2版本，“py3”表示使用python3版本  |
|  13   | ESLINT_RC |   varchar   | 16 |   0    |    Y     |  N   |       | js项目框架  |
|  14   | CODE_PATH |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 代码存放路径  |
|  15   | SCRIPT_TYPE |   varchar   | 16 |   0    |    Y     |  N   |       | 脚本类型  |
|  16   | SCRIPT |   longtext   | 2147483647 |   0    |    Y     |  N   |       | 打包脚本  |
|  17   | CHANNEL_CODE |   varchar   | 16 |   0    |    Y     |  N   |       | 渠道号，默认为DS  |
|  18   | UPDATE_USER_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 更新的用户id  |
|  19   | IS_DELETE |   varchar   | 6 |   0    |    Y     |  N   |       | 是否删除0可用1删除  |
|  20   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_PLUGIN_GITHUB_CHECK">T_PLUGIN_GITHUB_CHECK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | BUILD_NUMBER |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  4   | REPO_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库ID  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交ID  |
|  6   | CHECK_RUN_ID |   bigint   | 20 |   0    |    N     |  N   |       |   |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  10   | CHECK_RUN_NAME |   varchar   | 64 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_PLUGIN_GIT_CHECK">T_PLUGIN_GIT_CHECK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | BUILD_NUMBER |   int   | 10 |   0    |    N     |  N   |       | 构建编号  |
|  4   | REPO_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库ID  |
|  5   | COMMIT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 代码提交ID  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  8   | REPO_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 代码库别名  |
|  9   | CONTEXT |   varchar   | 255 |   0    |    Y     |  N   |       | 内容  |
