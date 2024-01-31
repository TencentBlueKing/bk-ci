# 数据库设计文档

**数据库名：** devops_ci_quality

**文档版本：** 1.0.1

**文档描述：** devops_ci_quality的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_CONTROL_POINT](#T_CONTROL_POINT) |  |
| [T_CONTROL_POINT_METADATA](#T_CONTROL_POINT_METADATA) |  |
| [T_CONTROL_POINT_TASK](#T_CONTROL_POINT_TASK) |  |
| [T_COUNT_INTERCEPT](#T_COUNT_INTERCEPT) |  |
| [T_COUNT_PIPELINE](#T_COUNT_PIPELINE) |  |
| [T_COUNT_RULE](#T_COUNT_RULE) |  |
| [T_GROUP](#T_GROUP) |  |
| [T_HISTORY](#T_HISTORY) |  |
| [T_QUALITY_CONTROL_POINT](#T_QUALITY_CONTROL_POINT) | 质量红线控制点表 |
| [T_QUALITY_HIS_DETAIL_METADATA](#T_QUALITY_HIS_DETAIL_METADATA) | 执行结果详细基础数据表 |
| [T_QUALITY_HIS_ORIGIN_METADATA](#T_QUALITY_HIS_ORIGIN_METADATA) | 执行结果基础数据表 |
| [T_QUALITY_INDICATOR](#T_QUALITY_INDICATOR) | 质量红线指标表 |
| [T_QUALITY_METADATA](#T_QUALITY_METADATA) | 质量红线基础数据表 |
| [T_QUALITY_RULE](#T_QUALITY_RULE) |  |
| [T_QUALITY_RULE_BUILD_HIS](#T_QUALITY_RULE_BUILD_HIS) |  |
| [T_QUALITY_RULE_BUILD_HIS_OPERATION](#T_QUALITY_RULE_BUILD_HIS_OPERATION) |  |
| [T_QUALITY_RULE_MAP](#T_QUALITY_RULE_MAP) |  |
| [T_QUALITY_RULE_OPERATION](#T_QUALITY_RULE_OPERATION) |  |
| [T_QUALITY_RULE_REVIEWER](#T_QUALITY_RULE_REVIEWER) | 红线审核人 |
| [T_QUALITY_RULE_TEMPLATE](#T_QUALITY_RULE_TEMPLATE) | 质量红线模板表 |
| [T_QUALITY_TEMPLATE_INDICATOR_MAP](#T_QUALITY_TEMPLATE_INDICATOR_MAP) | 模板-指标关系表 |
| [T_RULE](#T_RULE) |  |
| [T_TASK](#T_TASK) |  |

**表名：** <a id="T_CONTROL_POINT">T_CONTROL_POINT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  3   | TASK_LIST |   text   | 65535 |   0    |    N     |  N   |       | 任务信息列表  |
|  4   | ONLINE |   bit   | 1 |   0    |    N     |  N   |       | 是否在线  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_CONTROL_POINT_METADATA">T_CONTROL_POINT_METADATA</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | METADATA_ID |   varchar   | 128 |   0    |    N     |  Y   |       | 元数据ID  |
|  2   | METADATA_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 元数据类型  |
|  3   | METADATA_NAME |   text   | 65535 |   0    |    N     |  N   |       | 元数据名称  |
|  4   | TASK_ID |   varchar   | 64 |   0    |    N     |  N   |       | 任务ID  |
|  5   | ONLINE |   bit   | 1 |   0    |    N     |  N   |       | 是否在线  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_CONTROL_POINT_TASK">T_CONTROL_POINT_TASK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CONTROL_STAGE |   varchar   | 32 |   0    |    N     |  N   |       | 原子控制阶段  |
|  3   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  4   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_COUNT_INTERCEPT">T_COUNT_INTERCEPT</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | DATE |   date   | 10 |   0    |    N     |  N   |       | 日期  |
|  4   | COUNT |   int   | 10 |   0    |    N     |  N   |       | 计数  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  7   | INTERCEPT_COUNT |   int   | 10 |   0    |    N     |  N   |   0    | 拦截数  |
|  8   | RULE_INTERCEPT_COUNT |   int   | 10 |   0    |    N     |  N   |   0    | RULE_INTERCEPT_COUNT+count)  |

**表名：** <a id="T_COUNT_PIPELINE">T_COUNT_PIPELINE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | DATE |   date   | 10 |   0    |    N     |  N   |       | 日期  |
|  5   | COUNT |   int   | 10 |   0    |    N     |  N   |       | 计数  |
|  6   | LAST_INTERCEPT_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 上次拦截时间  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
|  9   | INTERCEPT_COUNT |   int   | 10 |   0    |    N     |  N   |   0    | 拦截数  |

**表名：** <a id="T_COUNT_RULE">T_COUNT_RULE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | RULE_ID |   bigint   | 20 |   0    |    N     |  N   |       | 规则ID  |
|  4   | DATE |   date   | 10 |   0    |    N     |  N   |       | 日期  |
|  5   | COUNT |   int   | 10 |   0    |    N     |  N   |       | 计数  |
|  6   | INTERCEPT_COUNT |   int   | 10 |   0    |    N     |  N   |   0    | 拦截数  |
|  7   | LAST_INTERCEPT_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 上次拦截时间  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_GROUP">T_GROUP</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  4   | INNER_USERS |   text   | 65535 |   0    |    N     |  N   |       | 内部人员  |
|  5   | INNER_USERS_COUNT |   int   | 10 |   0    |    N     |  N   |       | 内部人员计数  |
|  6   | OUTER_USERS |   text   | 65535 |   0    |    N     |  N   |       | 外部人员  |
|  7   | OUTER_USERS_COUNT |   int   | 10 |   0    |    N     |  N   |       | 外部人员计数  |
|  8   | REMARK |   text   | 65535 |   0    |    Y     |  N   |       | 评论  |
|  9   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 创建者  |
|  10   | UPDATOR |   varchar   | 64 |   0    |    N     |  N   |       | 更新人  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |

**表名：** <a id="T_HISTORY">T_HISTORY</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | RULE_ID |   bigint   | 20 |   0    |    N     |  N   |       | 规则ID  |
|  4   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  6   | RESULT |   varchar   | 34 |   0    |    N     |  N   |       |   |
|  7   | INTERCEPT_LIST |   text   | 65535 |   0    |    N     |  N   |       | 拦截列表  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  10   | PROJECT_NUM |   bigint   | 20 |   0    |    N     |  N   |   0    | 项目数量  |
|  11   | CHECK_TIMES |   int   | 10 |   0    |    Y     |  N   |   1    | 第几次检查  |

**表名：** <a id="T_QUALITY_CONTROL_POINT">T_QUALITY_CONTROL_POINT</a>

**说明：** 质量红线控制点表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ELEMENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 原子的ClassType  |
|  3   | NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 控制点名称(原子名称)  |
|  4   | STAGE |   varchar   | 64 |   0    |    Y     |  N   |       | 研发阶段  |
|  5   | AVAILABLE_POSITION |   varchar   | 64 |   0    |    Y     |  N   |       | 支持红线位置(准入-BEFORE,准出-AFTER)  |
|  6   | DEFAULT_POSITION |   varchar   | 64 |   0    |    Y     |  N   |       | 默认红线位置  |
|  7   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |       | 是否启用  |
|  8   | CREATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 创建用户  |
|  9   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 更新用户  |
|  10   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  12   | ATOM_VERSION |   varchar   | 30 |   0    |    Y     |  N   |   1.0.0    | 插件版本  |
|  13   | TEST_PROJECT |   varchar   | 64 |   0    |    N     |  N   |       | 测试的项目  |
|  14   | CONTROL_POINT_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 哈希ID  |
|  15   | TAG |   varchar   | 64 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_QUALITY_HIS_DETAIL_METADATA">T_QUALITY_HIS_DETAIL_METADATA</a>

**说明：** 执行结果详细基础数据表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | DATA_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 数据ID  |
|  3   | DATA_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 数据名称  |
|  4   | DATA_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 数据类型  |
|  5   | DATA_DESC |   varchar   | 128 |   0    |    Y     |  N   |       | 数据描述  |
|  6   | DATA_VALUE |   varchar   | 256 |   0    |    Y     |  N   |       | 数据值  |
|  7   | ELEMENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 原子的ClassType  |
|  8   | ELEMENT_DETAIL |   varchar   | 64 |   0    |    Y     |  N   |       | 工具/原子子类  |
|  9   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  10   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |
|  11   | BUILD_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建ID  |
|  12   | BUILD_NO |   varchar   | 64 |   0    |    Y     |  N   |       | 构建号  |
|  13   | CREATE_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 创建时间  |
|  14   | EXTRA |   text   | 65535 |   0    |    Y     |  N   |       | 额外信息  |
|  15   | TASK_ID |   varchar   | 34 |   0    |    Y     |  N   |       | 任务节点id  |
|  16   | TASK_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 任务节点名  |

**表名：** <a id="T_QUALITY_HIS_ORIGIN_METADATA">T_QUALITY_HIS_ORIGIN_METADATA</a>

**说明：** 执行结果基础数据表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 构建ID  |
|  5   | BUILD_NO |   varchar   | 64 |   0    |    Y     |  N   |       | 构建号  |
|  6   | RESULT_DATA |   text   | 65535 |   0    |    Y     |  N   |       | 返回数据  |
|  7   | CREATE_TIME |   bigint   | 20 |   0    |    Y     |  N   |       | 创建时间  |

**表名：** <a id="T_QUALITY_INDICATOR">T_QUALITY_INDICATOR</a>

**说明：** 质量红线指标表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ELEMENT_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 原子的ClassType  |
|  3   | ELEMENT_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 产出原子  |
|  4   | ELEMENT_DETAIL |   varchar   | 64 |   0    |    Y     |  N   |       | 工具/原子子类  |
|  5   | EN_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 指标英文名  |
|  6   | CN_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 指标中文名  |
|  7   | METADATA_IDS |   text   | 65535 |   0    |    Y     |  N   |       | 指标所包含基础数据  |
|  8   | DEFAULT_OPERATION |   varchar   | 32 |   0    |    Y     |  N   |       | 默认操作  |
|  9   | OPERATION_AVAILABLE |   text   | 65535 |   0    |    Y     |  N   |       | 可用操作  |
|  10   | THRESHOLD |   varchar   | 64 |   0    |    Y     |  N   |       | 默认阈值  |
|  11   | THRESHOLD_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | 阈值类型  |
|  12   | DESC |   varchar   | 256 |   0    |    Y     |  N   |       | 描述  |
|  13   | INDICATOR_READ_ONLY |   bit   | 1 |   0    |    Y     |  N   |       | 是否只读  |
|  14   | STAGE |   varchar   | 32 |   0    |    Y     |  N   |       | 阶段  |
|  15   | INDICATOR_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 指标范围  |
|  16   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |       | 是否启用  |
|  17   | TYPE |   varchar   | 32 |   0    |    Y     |  N   |   SYSTEM    | 指标类型  |
|  18   | TAG |   varchar   | 32 |   0    |    Y     |  N   |       | 指标标签，用于前端区分控制  |
|  19   | CREATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 创建用户  |
|  20   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 更新用户  |
|  21   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  22   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  23   | ATOM_VERSION |   varchar   | 30 |   0    |    N     |  N   |   1.0.0    | 插件版本号  |
|  24   | LOG_PROMPT |   varchar   | 1024 |   0    |    N     |  N   |       | 日志提示  |

**表名：** <a id="T_QUALITY_METADATA">T_QUALITY_METADATA</a>

**说明：** 质量红线基础数据表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | DATA_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 数据ID  |
|  3   | DATA_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 数据名称  |
|  4   | ELEMENT_TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 原子的ClassType  |
|  5   | ELEMENT_NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 产出原子  |
|  6   | ELEMENT_DETAIL |   varchar   | 64 |   0    |    Y     |  N   |       | 工具/原子子类  |
|  7   | VALUE_TYPE |   varchar   | 32 |   0    |    Y     |  N   |       | value值前端组件类型  |
|  8   | DESC |   varchar   | 256 |   0    |    Y     |  N   |       | 描述  |
|  9   | EXTRA |   text   | 65535 |   0    |    Y     |  N   |       | 额外信息  |
|  10   | CREATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  11   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  12   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |

**表名：** <a id="T_QUALITY_RULE">T_QUALITY_RULE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 规则名称  |
|  3   | DESC |   varchar   | 256 |   0    |    Y     |  N   |       | 规则描述  |
|  4   | INDICATOR_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 指标范围  |
|  5   | CONTROL_POINT |   varchar   | 64 |   0    |    Y     |  N   |       | 控制点原子类型  |
|  6   | CONTROL_POINT_POSITION |   varchar   | 64 |   0    |    Y     |  N   |       | 控制点红线位置  |
|  7   | CREATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 创建用户  |
|  8   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 更新用户  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  11   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否启用  |
|  12   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目id  |
|  13   | INTERCEPT_TIMES |   int   | 10 |   0    |    Y     |  N   |   0    | 拦截次数  |
|  14   | EXECUTE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 生效流水线执行数  |
|  15   | PIPELINE_TEMPLATE_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 流水线模板生效范围  |
|  16   | QUALITY_RULE_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 质量规则哈希ID  |
|  17   | GATEWAY_ID |   varchar   | 128 |   0    |    N     |  N   |       | 红线匹配的id  |

**表名：** <a id="T_QUALITY_RULE_BUILD_HIS">T_QUALITY_RULE_BUILD_HIS</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 项目ID  |
|  3   | PIPELINE_ID |   varchar   | 40 |   0    |    Y     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 40 |   0    |    Y     |  N   |       | 构建ID  |
|  5   | RULE_POS |   varchar   | 8 |   0    |    Y     |  N   |       | 控制点位置  |
|  6   | RULE_NAME |   varchar   | 123 |   0    |    Y     |  N   |       | 规则名称  |
|  7   | RULE_DESC |   varchar   | 256 |   0    |    Y     |  N   |       | 规则描述  |
|  8   | GATEWAY_ID |   varchar   | 128 |   0    |    Y     |  N   |       | 红线匹配的id  |
|  9   | PIPELINE_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 生效的流水线id集合  |
|  10   | TEMPLATE_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 生效的流水线模板id集合  |
|  11   | INDICATOR_IDS |   text   | 65535 |   0    |    Y     |  N   |       | 指标类型  |
|  12   | INDICATOR_OPERATIONS |   text   | 65535 |   0    |    Y     |  N   |       | 指标操作  |
|  13   | INDICATOR_THRESHOLDS |   text   | 65535 |   0    |    Y     |  N   |       | 指标阈值  |
|  14   | OPERATION_LIST |   text   | 65535 |   0    |    Y     |  N   |       | 操作清单  |
|  15   | QUALITY_RULE_HIS_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 质量规则构建历史哈希ID  |
|  16   | CREATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  17   | CREATE_USER |   varchar   | 32 |   0    |    Y     |  N   |       | 创建人  |
|  18   | STAGE_ID |   varchar   | 40 |   0    |    N     |  N   |   1    | stage_id  |
|  19   | STATUS |   varchar   | 20 |   0    |    Y     |  N   |       | 红线状态  |
|  20   | GATE_KEEPERS |   varchar   | 1024 |   0    |    Y     |  N   |       | 红线把关人  |
|  21   | TASK_STEPS |   text   | 65535 |   0    |    Y     |  N   |       | 红线指定的任务节点  |

**表名：** <a id="T_QUALITY_RULE_BUILD_HIS_OPERATION">T_QUALITY_RULE_BUILD_HIS_OPERATION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | RULE_ID |   bigint   | 20 |   0    |    N     |  N   |       | 规则id  |
|  3   | STAGE_ID |   varchar   | 40 |   0    |    N     |  N   |       |   |
|  4   | GATE_OPT_USER |   varchar   | 32 |   0    |    Y     |  N   |       |   |
|  5   | GATE_OPT_TIME |   datetime   | 19 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_QUALITY_RULE_MAP">T_QUALITY_RULE_MAP</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | RULE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 规则ID  |
|  3   | INDICATOR_IDS |   text   | 65535 |   0    |    Y     |  N   |       | 指标类型  |
|  4   | INDICATOR_OPERATIONS |   text   | 65535 |   0    |    Y     |  N   |       | 指标操作  |
|  5   | INDICATOR_THRESHOLDS |   text   | 65535 |   0    |    Y     |  N   |       | 指标阈值  |

**表名：** <a id="T_QUALITY_RULE_OPERATION">T_QUALITY_RULE_OPERATION</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | RULE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 规则ID  |
|  3   | TYPE |   varchar   | 16 |   0    |    Y     |  N   |       | 类型  |
|  4   | NOTIFY_USER |   text   | 65535 |   0    |    Y     |  N   |       | 通知人员  |
|  5   | NOTIFY_GROUP_ID |   text   | 65535 |   0    |    Y     |  N   |       | 用户组ID  |
|  6   | NOTIFY_TYPES |   varchar   | 64 |   0    |    Y     |  N   |       | 通知类型  |
|  7   | AUDIT_USER |   text   | 65535 |   0    |    Y     |  N   |       | 审核人员  |
|  8   | AUDIT_TIMEOUT |   int   | 10 |   0    |    Y     |  N   |       | 审核超时时间  |

**表名：** <a id="T_QUALITY_RULE_REVIEWER">T_QUALITY_RULE_REVIEWER</a>

**说明：** 红线审核人

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 64 |   0    |    N     |  N   |       | 项目ID  |
|  3   | RULE_ID |   bigint   | 20 |   0    |    N     |  N   |       | 规则ID  |
|  4   | PIPELINE_ID |   varchar   | 64 |   0    |    N     |  N   |       | 流水线ID  |
|  5   | BUILD_ID |   varchar   | 64 |   0    |    N     |  N   |       | 构建ID  |
|  6   | REVIEWER |   varchar   | 32 |   0    |    N     |  N   |       | 实际审核人  |
|  7   | REVIEW_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 审核时间  |

**表名：** <a id="T_QUALITY_RULE_TEMPLATE">T_QUALITY_RULE_TEMPLATE</a>

**说明：** 质量红线模板表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 64 |   0    |    Y     |  N   |       | 名称  |
|  3   | TYPE |   varchar   | 16 |   0    |    Y     |  N   |       | 类型  |
|  4   | DESC |   varchar   | 256 |   0    |    Y     |  N   |       | 描述  |
|  5   | STAGE |   varchar   | 64 |   0    |    Y     |  N   |       | 阶段  |
|  6   | CONTROL_POINT |   varchar   | 64 |   0    |    Y     |  N   |       | 控制点原子类型  |
|  7   | CONTROL_POINT_POSITION |   varchar   | 64 |   0    |    Y     |  N   |       | 控制点红线位置  |
|  8   | CREATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 创建者  |
|  9   | UPDATE_USER |   varchar   | 64 |   0    |    Y     |  N   |       | 修改人  |
|  10   | create_time |   datetime   | 19 |   0    |    Y     |  N   |       | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 更新时间  |
|  12   | ENABLE |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否启用  |

**表名：** <a id="T_QUALITY_TEMPLATE_INDICATOR_MAP">T_QUALITY_TEMPLATE_INDICATOR_MAP</a>

**说明：** 模板-指标关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TEMPLATE_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 模板ID  |
|  3   | INDICATOR_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 指标ID  |
|  4   | OPERATION |   varchar   | 32 |   0    |    Y     |  N   |       | 可选操作  |
|  5   | THRESHOLD |   varchar   | 64 |   0    |    Y     |  N   |       | 默认阈值  |

**表名：** <a id="T_RULE">T_RULE</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   bigint   | 20 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PROJECT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 项目ID  |
|  3   | NAME |   varchar   | 128 |   0    |    N     |  N   |       | 名称  |
|  4   | REMARK |   text   | 65535 |   0    |    Y     |  N   |       | 评论  |
|  5   | TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 类型  |
|  6   | CONTROL_POINT |   varchar   | 32 |   0    |    N     |  N   |       | 控制点原子类型  |
|  7   | TASK_ID |   varchar   | 64 |   0    |    N     |  N   |       | 任务ID  |
|  8   | THRESHOLD |   text   | 65535 |   0    |    N     |  N   |       | 默认阈值  |
|  9   | INDICATOR_RANGE |   text   | 65535 |   0    |    Y     |  N   |       | 指标范围  |
|  10   | RANGE_IDENTIFICATION |   text   | 65535 |   0    |    N     |  N   |       | ANY-项目ID集合,PART_BY_NAME-空集合  |
|  11   | OPERATION |   varchar   | 32 |   0    |    N     |  N   |       | 可选操作  |
|  12   | OPERATION_END_NOTIFY_TYPE |   varchar   | 128 |   0    |    Y     |  N   |       | 操作结束通知类型  |
|  13   | OPERATION_END_NOTIFY_GROUP |   text   | 65535 |   0    |    Y     |  N   |       | 操作结束通知用户组  |
|  14   | OPERATION_END_NOTIFY_USER |   text   | 65535 |   0    |    Y     |  N   |       | 操作结束通知用户  |
|  15   | OPERATION_AUDIT_NOTIFY_USER |   text   | 65535 |   0    |    Y     |  N   |       | 操作审核通知用户  |
|  16   | INTERCEPT_TIMES |   int   | 10 |   0    |    N     |  N   |   0    | 拦截次数  |
|  17   | ENABLE |   bit   | 1 |   0    |    N     |  N   |       | 是否启用  |
|  18   | CREATOR |   varchar   | 32 |   0    |    N     |  N   |       | 创建者  |
|  19   | UPDATOR |   varchar   | 32 |   0    |    N     |  N   |       | 更新人  |
|  20   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  21   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  22   | IS_DELETED |   bit   | 1 |   0    |    N     |  N   |       | 是否删除0可用1删除  |
|  23   | OPERATION_AUDIT_TIMEOUT_MINUTES |   int   | 10 |   0    |    Y     |  N   |       | 审核超时时间  |

**表名：** <a id="T_TASK">T_TASK</a>

**说明：** 

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 64 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 255 |   0    |    N     |  N   |       | 名称  |
|  3   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 创建时间  |
|  4   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 更新时间  |
