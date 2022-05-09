# 数据库设计文档

**数据库名：** devops_ci_measure

**文档版本：** 1.0.0

**文档描述：** devops_ci_measure的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_MEASURE_BUILD_ELEMENT](#T_MEASURE_BUILD_ELEMENT) |  |
| [T_MEASURE_DASHBOARD_VIEW](#T_MEASURE_DASHBOARD_VIEW) |  |
| [T_MEASURE_PIPELINE_BUILD](#T_MEASURE_PIPELINE_BUILD) |  |
| [T_MEASURE_PROJECT](#T_MEASURE_PROJECT) |  |
| [T_MEASURE_WETEST_INFO](#T_MEASURE_WETEST_INFO) |  |

**表名：** <a id="T_MEASURE_BUILD_ELEMENT">T_MEASURE_BUILD_ELEMENT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | elementName |   varchar   | 64 |   0    |    N     |  N   |       | 元素名称  |
|  2   | pipelineId |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  3   | buildId |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  4   | status |   varchar   | 32 |   0    |    N     |  N   |       | 状态  |
|  5   | beginTime |   datetime   | 19 |   0    |    N     |  N   |       | 开始时间  |
|  6   | endTime |   datetime   | 19 |   0    |    N     |  N   |       | 结束时间  |
|  7   | projectId |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  8   | extra |   text   | 65535 |   0    |    Y     |  N   |       | 额外信息  |
|  9   | type |   varchar   | 32 |   0    |    N     |  N   |       | 类型  |
|  10   | elementId |   varchar   | 64 |   0    |    N     |  N   |       | 插件elementId  |
|  11   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  12   | atomCode |   varchar   | 128 |   0    |    N     |  N   |       | 插件的唯一标识  |

**表名：** <a id="T_MEASURE_DASHBOARD_VIEW">T_MEASURE_DASHBOARD_VIEW</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | projectId |   varchar   | 36 |   0    |    N     |  N   |       | 项目ID  |
|  3   | user |   varchar   | 32 |   0    |    N     |  N   |       | 用户  |
|  4   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  5   | viewConfig |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 视图配置  |
|  6   | viewType |   varchar   | 32 |   0    |    N     |  N   |   SINGLE    | 视图类型  |

**表名：** <a id="T_MEASURE_PIPELINE_BUILD">T_MEASURE_PIPELINE_BUILD</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | pipelineId |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  2   | buildId |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  3   | beginTime |   datetime   | 19 |   0    |    N     |  N   |       | 流水线的启动时间  |
|  4   | endTime |   datetime   | 19 |   0    |    N     |  N   |       | 流水线的结束时间  |
|  5   | startType |   varchar   | 20 |   0    |    N     |  N   |       | 流水线的启动方式  |
|  6   | buildUser |   varchar   | 255 |   0    |    N     |  N   |       | 流水线的启动用户  |
|  7   | isParallel |   bit   | 1 |   0    |    N     |  N   |       | 流水线的是否并行  |
|  8   | buildResult |   varchar   | 20 |   0    |    N     |  N   |       | 流水线的构建结果  |
|  9   | projectId |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  10   | pipeline |   mediumtext   | 16777215 |   0    |    N     |  N   |       | 流水线  |
|  11   | buildNum |   int   | 10 |   0    |    N     |  N   |       | 构建版本号  |
|  12   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  13   | metaInfo |   text   | 65535 |   0    |    Y     |  N   |       | 元数据  |
|  14   | parentPipelineId |   varchar   | 34 |   0    |    Y     |  N   |       | 启动子流水线的流水线ID  |
|  15   | parentBuildId |   varchar   | 34 |   0    |    Y     |  N   |       | 启动子流水线的构建ID  |

**表名：** <a id="T_MEASURE_PROJECT">T_MEASURE_PROJECT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | id |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | approval_status |   int   | 10 |   0    |    Y     |  N   |       | 审核状态  |
|  3   | bg_id |   int   | 10 |   0    |    Y     |  N   |       | 事业群ID  |
|  4   | bg_name |   varchar   | 120 |   0    |    Y     |  N   |       | 事业群名称  |
|  5   | cc_app_id |   int   | 10 |   0    |    Y     |  N   |       | 应用ID  |
|  6   | center_id |   int   | 10 |   0    |    Y     |  N   |       | 中心ID  |
|  7   | center_name |   varchar   | 120 |   0    |    Y     |  N   |       | 中心名字  |
|  8   | created_at |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  9   | creator |   varchar   | 32 |   0    |    Y     |  N   |       | 创建者  |
|  10   | data_id |   int   | 10 |   0    |    Y     |  N   |       | 数据ID  |
|  11   | deploy_type |   varchar   | 256 |   0    |    Y     |  N   |       | 部署类型  |
|  12   | dept_id |   int   | 10 |   0    |    Y     |  N   |       | 项目所属二级机构ID  |
|  13   | dept_name |   varchar   | 120 |   0    |    Y     |  N   |       | 项目所属二级机构名称  |
|  14   | description |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  15   | project_code |   varchar   | 128 |   0    |    Y     |  N   |       | 用户组所属项目  |
|  16   | is_offlined |   bit   | 1 |   0    |    Y     |  N   |       | 是否停用  |
|  17   | is_secrecy |   bit   | 1 |   0    |    Y     |  N   |       | 是否保密  |
|  18   | kind |   int   | 10 |   0    |    Y     |  N   |       | 容器类型  |
|  19   | project_id |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  20   | project_name |   varchar   | 256 |   0    |    Y     |  N   |       | 项目名称  |
|  21   | project_type |   int   | 10 |   0    |    Y     |  N   |       | 项目类型  |
|  22   | updated_at |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  23   | use_bk |   bit   | 1 |   0    |    Y     |  N   |       | 是否用蓝鲸  |
|  24   | logo_addr |   varchar   | 1024 |   0    |    Y     |  N   |       | logo地址  |
|  25   | pipeline_count |   int   | 10 |   0    |    Y     |  N   |   0    | 流水线数量  |

**表名：** <a id="T_MEASURE_WETEST_INFO">T_MEASURE_WETEST_INFO</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | elementKeyId |   int   | 10 |   0    |    N     |  N   |       | 元素Keyid  |
|  3   | testid |   varchar   | 64 |   0    |    Y     |  N   |       |   |
|  4   | passrate |   int   | 10 |   0    |    Y     |  N   |   0    | 通过率  |
|  5   | failManuMap |   text   | 65535 |   0    |    Y     |  N   |       |   |
|  6   | failVersionMap |   text   | 65535 |   0    |    Y     |  N   |       | 失败版本map  |
|  7   | failResolutionMap |   text   | 65535 |   0    |    Y     |  N   |       | 失败解析map  |
|  8   | errCodeMap |   text   | 65535 |   0    |    Y     |  N   |       | 错误代码map  |
|  9   | errLevelMap |   text   | 65535 |   0    |    Y     |  N   |       | 错误等级map  |
|  10   | createTime |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
