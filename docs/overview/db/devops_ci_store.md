# 数据库设计文档

**数据库名：** devops_ci_store

**文档版本：** 1.0.1

**文档描述：** devops_ci_store的数据库文档

| 表名                  | 说明       |
| :---: | :---: |
| [T_APPS](#T_APPS) | 编译环境信息表 |
| [T_APP_ENV](#T_APP_ENV) | 编译环境变量表 |
| [T_APP_VERSION](#T_APP_VERSION) | 编译环境版本信息表 |
| [T_ATOM](#T_ATOM) | 流水线原子表 |
| [T_ATOM_APPROVE_REL](#T_ATOM_APPROVE_REL) | 插件审核关联关系表 |
| [T_ATOM_BUILD_APP_REL](#T_ATOM_BUILD_APP_REL) | 流水线原子构建与编译环境关联关系表 |
| [T_ATOM_BUILD_INFO](#T_ATOM_BUILD_INFO) | 流水线原子构建信息表 |
| [T_ATOM_DEV_LANGUAGE_ENV_VAR](#T_ATOM_DEV_LANGUAGE_ENV_VAR) | 插件环境变量信息表 |
| [T_ATOM_ENV_INFO](#T_ATOM_ENV_INFO) | 流水线原子执行环境信息表 |
| [T_ATOM_FEATURE](#T_ATOM_FEATURE) | 原子插件特性信息表 |
| [T_ATOM_LABEL_REL](#T_ATOM_LABEL_REL) | 原子与标签关联关系表 |
| [T_ATOM_OFFLINE](#T_ATOM_OFFLINE) | 原子下架表 |
| [T_ATOM_OPERATE_LOG](#T_ATOM_OPERATE_LOG) | 流水线原子操作日志表 |
| [T_ATOM_PIPELINE_BUILD_REL](#T_ATOM_PIPELINE_BUILD_REL) | 流水线原子构建关联关系表 |
| [T_ATOM_PIPELINE_REL](#T_ATOM_PIPELINE_REL) | 流水线原子与流水线关联关系表 |
| [T_ATOM_VERSION_LOG](#T_ATOM_VERSION_LOG) | 流水线原子版本日志表 |
| [T_BUILD_RESOURCE](#T_BUILD_RESOURCE) | 流水线构建资源信息表 |
| [T_BUSINESS_CONFIG](#T_BUSINESS_CONFIG) | store业务配置表 |
| [T_CATEGORY](#T_CATEGORY) | 范畴信息表 |
| [T_CLASSIFY](#T_CLASSIFY) | 流水线原子分类信息表 |
| [T_CONTAINER](#T_CONTAINER) | 流水线构建容器表（这里的容器与Docker不是同一个概念，而是流水线模型中的一个元素） |
| [T_CONTAINER_RESOURCE_REL](#T_CONTAINER_RESOURCE_REL) | 流水线容器与构建资源关联关系表 |
| [T_IMAGE](#T_IMAGE) | 镜像信息表 |
| [T_IMAGE_AGENT_TYPE](#T_IMAGE_AGENT_TYPE) | 镜像与机器类型关联表 |
| [T_IMAGE_CATEGORY_REL](#T_IMAGE_CATEGORY_REL) | 镜像与范畴关联关系表 |
| [T_IMAGE_FEATURE](#T_IMAGE_FEATURE) | 镜像特性表 |
| [T_IMAGE_LABEL_REL](#T_IMAGE_LABEL_REL) | 镜像与标签关联关系表 |
| [T_IMAGE_VERSION_LOG](#T_IMAGE_VERSION_LOG) | 镜像版本日志表 |
| [T_LABEL](#T_LABEL) | 标签信息表 |
| [T_LOGO](#T_LOGO) | 商店logo信息表 |
| [T_REASON](#T_REASON) | 原因定义表 |
| [T_REASON_REL](#T_REASON_REL) | 原因和组件关联关系 |
| [T_STORE_APPROVE](#T_STORE_APPROVE) | 审核表 |
| [T_STORE_BUILD_APP_REL](#T_STORE_BUILD_APP_REL) | store构建与编译环境关联关系表 |
| [T_STORE_BUILD_INFO](#T_STORE_BUILD_INFO) | store组件构建信息表 |
| [T_STORE_COMMENT](#T_STORE_COMMENT) | store组件评论信息表 |
| [T_STORE_COMMENT_PRAISE](#T_STORE_COMMENT_PRAISE) | store组件评论点赞信息表 |
| [T_STORE_COMMENT_REPLY](#T_STORE_COMMENT_REPLY) | store组件评论回复信息表 |
| [T_STORE_DEPT_REL](#T_STORE_DEPT_REL) | 商店组件与与机构关联关系表 |
| [T_STORE_DOCKING_PLATFORM](#T_STORE_DOCKING_PLATFORM) | 组件对接平台信息表 |
| [T_STORE_DOCKING_PLATFORM_ERROR_CODE](#T_STORE_DOCKING_PLATFORM_ERROR_CODE) | 平台所属错误码信息 |
| [T_STORE_DOCKING_PLATFORM_REL](#T_STORE_DOCKING_PLATFORM_REL) | 组件对接平台关联关系表 |
| [T_STORE_ENV_VAR](#T_STORE_ENV_VAR) | 环境变量信息表 |
| [T_STORE_ERROR_CODE_INFO](#T_STORE_ERROR_CODE_INFO) | store组件错误码信息 |
| [T_STORE_HONOR_INFO](#T_STORE_HONOR_INFO) | 研发商店荣誉信息表 |
| [T_STORE_HONOR_REL](#T_STORE_HONOR_REL) | 研发商店荣誉关联信息表 |
| [T_STORE_INDEX_BASE_INFO](#T_STORE_INDEX_BASE_INFO) | 研发商店指标基本信息表 |
| [T_STORE_INDEX_ELEMENT_DETAIL](#T_STORE_INDEX_ELEMENT_DETAIL) | 研发商店组件指标要素详情表 |
| [T_STORE_INDEX_LEVEL_INFO](#T_STORE_INDEX_LEVEL_INFO) | 研发商店指标等级信息表 |
| [T_STORE_INDEX_RESULT](#T_STORE_INDEX_RESULT) | 研发商店组件指标结果表 |
| [T_STORE_MEDIA_INFO](#T_STORE_MEDIA_INFO) | 媒体信息表 |
| [T_STORE_MEMBER](#T_STORE_MEMBER) | store组件成员信息表 |
| [T_STORE_OPT_LOG](#T_STORE_OPT_LOG) | store操作日志表 |
| [T_STORE_PIPELINE_BUILD_REL](#T_STORE_PIPELINE_BUILD_REL) | 商店组件构建关联关系表 |
| [T_STORE_PIPELINE_REL](#T_STORE_PIPELINE_REL) | 商店组件与与流水线关联关系表 |
| [T_STORE_PKG_RUN_ENV_INFO](#T_STORE_PKG_RUN_ENV_INFO) | 组件安装包运行时环境信息表 |
| [T_STORE_PROJECT_REL](#T_STORE_PROJECT_REL) | 商店组件与项目关联关系表 |
| [T_STORE_PUBLISHER_INFO](#T_STORE_PUBLISHER_INFO) | 发布者信息表 |
| [T_STORE_PUBLISHER_MEMBER_REL](#T_STORE_PUBLISHER_MEMBER_REL) | 发布者成员关联关系表 |
| [T_STORE_RELEASE](#T_STORE_RELEASE) | store组件发布升级信息表 |
| [T_STORE_SENSITIVE_API](#T_STORE_SENSITIVE_API) | 敏感API接口信息表 |
| [T_STORE_SENSITIVE_CONF](#T_STORE_SENSITIVE_CONF) | store私有配置表 |
| [T_STORE_STATISTICS](#T_STORE_STATISTICS) | store统计信息表 |
| [T_STORE_STATISTICS_DAILY](#T_STORE_STATISTICS_DAILY) | store每日统计信息表 |
| [T_STORE_STATISTICS_TOTAL](#T_STORE_STATISTICS_TOTAL) | store全量统计信息表 |
| [T_TEMPLATE](#T_TEMPLATE) | 模板信息表 |
| [T_TEMPLATE_CATEGORY_REL](#T_TEMPLATE_CATEGORY_REL) | 模板与范畴关联关系表 |
| [T_TEMPLATE_LABEL_REL](#T_TEMPLATE_LABEL_REL) | 模板与标签关联关系表 |

**表名：** <a id="T_APPS">T_APPS</a>

**说明：** 编译环境信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  3   | OS |   varchar   | 32 |   0    |    N     |  N   |       | 操作系统  |
|  4   | BIN_PATH |   varchar   | 64 |   0    |    Y     |  N   |       | 执行所在路径  |

**表名：** <a id="T_APP_ENV">T_APP_ENV</a>

**说明：** 编译环境变量表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | APP_ID |   int   | 10 |   0    |    N     |  N   |       | 编译环境ID  |
|  3   | PATH |   varchar   | 32 |   0    |    N     |  N   |       | 路径  |
|  4   | NAME |   varchar   | 32 |   0    |    N     |  N   |       | 名称  |
|  5   | DESCRIPTION |   varchar   | 64 |   0    |    N     |  N   |       | 描述  |

**表名：** <a id="T_APP_VERSION">T_APP_VERSION</a>

**说明：** 编译环境版本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | APP_ID |   int   | 10 |   0    |    N     |  N   |       | 编译环境ID  |
|  3   | VERSION |   varchar   | 32 |   0    |    Y     |  N   |       | 版本号  |

**表名：** <a id="T_ATOM">T_ATOM</a>

**说明：** 流水线原子表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 64 |   0    |    N     |  N   |       | 名称  |
|  3   | ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  4   | CLASS_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 插件大类  |
|  5   | SERVICE_SCOPE |   varchar   | 256 |   0    |    N     |  N   |       | 生效范围  |
|  6   | JOB_TYPE |   varchar   | 20 |   0    |    Y     |  N   |       | 适用Job类型，AGENT：编译环境，AGENT_LESS：无编译环境  |
|  7   | OS |   varchar   | 100 |   0    |    N     |  N   |       | 操作系统  |
|  8   | CLASSIFY_ID |   varchar   | 32 |   0    |    N     |  N   |       | 所属分类ID  |
|  9   | DOCS_LINK |   varchar   | 256 |   0    |    Y     |  N   |       | 文档跳转链接  |
|  10   | ATOM_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   1    | 原子类型  |
|  11   | ATOM_STATUS |   tinyint   | 4 |   0    |    N     |  N   |       | 原子状态  |
|  12   | ATOM_STATUS_MSG |   varchar   | 1024 |   0    |    Y     |  N   |       | 插件状态信息  |
|  13   | SUMMARY |   varchar   | 256 |   0    |    Y     |  N   |       | 简介  |
|  14   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  15   | CATEGROY |   tinyint   | 4 |   0    |    N     |  N   |   1    | 类别  |
|  16   | VERSION |   varchar   | 30 |   0    |    N     |  N   |       | 版本号  |
|  17   | LOGO_URL |   varchar   | 256 |   0    |    Y     |  N   |       | LOGOURL地址  |
|  18   | ICON |   text   | 65535 |   0    |    Y     |  N   |       | 插件图标  |
|  19   | DEFAULT_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否为默认原子  |
|  20   | LATEST_FLAG |   bit   | 1 |   0    |    N     |  N   |       | 是否为最新版本原子  |
|  21   | BUILD_LESS_RUN_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 无构建环境原子是否可以在有构建环境运行标识  |
|  22   | REPOSITORY_HASH_ID |   varchar   | 64 |   0    |    Y     |  N   |       | 代码库哈希ID  |
|  23   | CODE_SRC |   varchar   | 256 |   0    |    Y     |  N   |       | 代码库链接  |
|  24   | PAY_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否免费  |
|  25   | HTML_TEMPLATE_VERSION |   varchar   | 10 |   0    |    N     |  N   |   1.1    | 前端渲染模板版本  |
|  26   | PROPS |   text   | 65535 |   0    |    Y     |  N   |       | 自定义扩展容器前端表单属性字段的JSON串  |
|  27   | DATA |   text   | 65535 |   0    |    Y     |  N   |       | 预留字段  |
|  28   | PUBLISHER |   varchar   | 50 |   0    |    N     |  N   |   system    | 原子发布者  |
|  29   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |       | 权值  |
|  30   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  31   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  32   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  33   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  34   | VISIBILITY_LEVEL |   int   | 10 |   0    |    N     |  N   |   0    | 可见范围  |
|  35   | PUB_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 发布时间  |
|  36   | PRIVATE_REASON |   varchar   | 256 |   0    |    Y     |  N   |       | 插件代码库不开源原因  |
|  37   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除  |
|  38   | BRANCH |   varchar   | 128 |   0    |    Y     |  N   |   master    | 代码库分支  |
|  39   | BRANCH_TEST_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否是分支测试版本  |
|  40   | LATEST_TEST_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否为最新测试版本原子，TRUE：最新FALSE：非最新  |

**表名：** <a id="T_ATOM_APPROVE_REL">T_ATOM_APPROVE_REL</a>

**说明：** 插件审核关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  3   | TEST_PROJECT_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 调试项目编码  |
|  4   | APPROVE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 审批ID  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_BUILD_APP_REL">T_ATOM_BUILD_APP_REL</a>

**说明：** 流水线原子构建与编译环境关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | BUILD_INFO_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建信息Id  |
|  3   | APP_VERSION_ID |   int   | 10 |   0    |    Y     |  N   |       | 编译环境版本Id(对应T_APP_VERSION主键)  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_BUILD_INFO">T_ATOM_BUILD_INFO</a>

**说明：** 流水线原子构建信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LANGUAGE |   varchar   | 64 |   0    |    Y     |  N   |       | 语言  |
|  3   | SCRIPT |   text   | 65535 |   0    |    N     |  N   |       | 打包脚本  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | REPOSITORY_PATH |   varchar   | 500 |   0    |    Y     |  N   |       | 代码存放路径  |
|  9   | SAMPLE_PROJECT_PATH |   varchar   | 500 |   0    |    N     |  N   |       | 样例工程路径  |
|  10   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否启用1启用0禁用  |

**表名：** <a id="T_ATOM_DEV_LANGUAGE_ENV_VAR">T_ATOM_DEV_LANGUAGE_ENV_VAR</a>

**说明：** 插件环境变量信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | LANGUAGE |   varchar   | 64 |   0    |    N     |  N   |       | 插件开发语言  |
|  3   | ENV_KEY |   varchar   | 64 |   0    |    N     |  N   |       | 环境变量key值  |
|  4   | ENV_VALUE |   varchar   | 256 |   0    |    N     |  N   |       | 环境变量value值  |
|  5   | BUILD_HOST_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 适用构建机类型PUBLIC:公共构建机，THIRD:第三方构建机，ALL:所有  |
|  6   | BUILD_HOST_OS |   varchar   | 32 |   0    |    N     |  N   |       | 适用构建机操作系统WINDOWS:windows构建机，LINUX:linux构建机，MAC_OS:mac构建机，ALL:所有  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_ENV_INFO">T_ATOM_ENV_INFO</a>

**说明：** 流水线原子执行环境信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件Id  |
|  3   | PKG_PATH |   varchar   | 1024 |   0    |    N     |  N   |       | 安装包路径  |
|  4   | LANGUAGE |   varchar   | 64 |   0    |    Y     |  N   |       | 语言  |
|  5   | MIN_VERSION |   varchar   | 20 |   0    |    Y     |  N   |       | 支持插件开发语言的最低版本  |
|  6   | TARGET |   varchar   | 256 |   0    |    N     |  N   |       | 插件执行入口  |
|  7   | SHA_CONTENT |   varchar   | 1024 |   0    |    Y     |  N   |       | 插件SHA签名串  |
|  8   | PRE_CMD |   text   | 65535 |   0    |    Y     |  N   |       | 插件执行前置命令  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  13   | PKG_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 插件包名  |
|  14   | POST_ENTRY_PARAM |   varchar   | 64 |   0    |    Y     |  N   |       | 入口参数  |
|  15   | POST_CONDITION |   varchar   | 1024 |   0    |    Y     |  N   |       | 执行条件  |
|  16   | OS_NAME |   varchar   | 128 |   0    |    Y     |  N   |       | 支持的操作系统名称  |
|  17   | OS_ARCH |   varchar   | 128 |   0    |    Y     |  N   |       | 支持的操作系统架构  |
|  18   | RUNTIME_VERSION |   varchar   | 128 |   0    |    Y     |  N   |       | 运行时版本  |
|  19   | DEFAULT_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否为默认环境信息  |
|  20   | FINISH_KILL_FLAG |   bit   | 1 |   0    |    Y     |  N   |       | 插件运行结束后是否立即杀掉其进程  |

**表名：** <a id="T_ATOM_FEATURE">T_ATOM_FEATURE</a>

**说明：** 原子插件特性信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  3   | VISIBILITY_LEVEL |   int   | 10 |   0    |    N     |  N   |   0    | 可见范围  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | RECOMMEND_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否推荐  |
|  9   | PRIVATE_REASON |   varchar   | 256 |   0    |    Y     |  N   |       | 插件代码库不开源原因  |
|  10   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否删除  |
|  11   | YAML_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | yaml可用标识  |
|  12   | QUALITY_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 质量红线可用标识  |
|  13   | CERTIFICATION_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否认证标识  |

**表名：** <a id="T_ATOM_LABEL_REL">T_ATOM_LABEL_REL</a>

**说明：** 原子与标签关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LABEL_ID |   varchar   | 32 |   0    |    N     |  N   |       | 标签ID  |
|  3   | ATOM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件Id  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_OFFLINE">T_ATOM_OFFLINE</a>

**说明：** 原子下架表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  3   | BUFFER_DAY |   tinyint   | 4 |   0    |    N     |  N   |       |   |
|  4   | EXPIRE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 过期时间  |
|  5   | STATUS |   tinyint   | 4 |   0    |    N     |  N   |       | 状态  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_OPERATE_LOG">T_ATOM_OPERATE_LOG</a>

**说明：** 流水线原子操作日志表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件Id  |
|  3   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 日志内容  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_PIPELINE_BUILD_REL">T_ATOM_PIPELINE_BUILD_REL</a>

**说明：** 流水线原子构建关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件Id  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_PIPELINE_REL">T_ATOM_PIPELINE_REL</a>

**说明：** 流水线原子与流水线关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 插件的唯一标识  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_ATOM_VERSION_LOG">T_ATOM_VERSION_LOG</a>

**说明：** 流水线原子版本日志表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | ATOM_ID |   varchar   | 32 |   0    |    N     |  N   |       | 插件Id  |
|  3   | RELEASE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |       | 发布类型  |
|  4   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 日志内容  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_BUILD_RESOURCE">T_BUILD_RESOURCE</a>

**说明：** 流水线构建资源信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | BUILD_RESOURCE_CODE |   varchar   | 30 |   0    |    N     |  N   |       | 构建资源代码  |
|  3   | BUILD_RESOURCE_NAME |   varchar   | 45 |   0    |    N     |  N   |       | 构建资源名称  |
|  4   | DEFAULT_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否为默认原子  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_BUSINESS_CONFIG">T_BUSINESS_CONFIG</a>

**说明：** store业务配置表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | BUSINESS |   varchar   | 64 |   0    |    N     |  N   |       | 业务名称  |
|  2   | FEATURE |   varchar   | 64 |   0    |    N     |  N   |       | 要控制的功能特性  |
|  3   | BUSINESS_VALUE |   varchar   | 255 |   0    |    N     |  N   |       | 业务取值  |
|  4   | CONFIG_VALUE |   text   | 65535 |   0    |    N     |  N   |       | 配置值  |
|  5   | DESCRIPTION |   varchar   | 255 |   0    |    Y     |  N   |       | 配置描述  |
|  6   | ID |   int   | 10 |   0    |    N     |  Y   |       | 主键ID  |

**表名：** <a id="T_CATEGORY">T_CATEGORY</a>

**说明：** 范畴信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CATEGORY_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 范畴代码  |
|  3   | CATEGORY_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 范畴名称  |
|  4   | ICON_URL |   varchar   | 256 |   0    |    Y     |  N   |       | 范畴图标链接  |
|  5   | TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 类型  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_CLASSIFY">T_CLASSIFY</a>

**说明：** 流水线原子分类信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CLASSIFY_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 所属镜像分类代码  |
|  3   | CLASSIFY_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 所属镜像分类名称  |
|  4   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |       | 权值  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  9   | TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 类型  |

**表名：** <a id="T_CONTAINER">T_CONTAINER</a>

**说明：** 流水线构建容器表（这里的容器与Docker不是同一个概念，而是流水线模型中的一个元素）

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | NAME |   varchar   | 45 |   0    |    N     |  N   |       | 名称  |
|  3   | TYPE |   varchar   | 20 |   0    |    N     |  N   |       | 类型  |
|  4   | OS |   varchar   | 15 |   0    |    N     |  N   |       | 操作系统  |
|  5   | REQUIRED |   tinyint   | 4 |   0    |    N     |  N   |   0    | 是否必须  |
|  6   | MAX_QUEUE_MINUTES |   int   | 10 |   0    |    Y     |  N   |   60    | 最大排队时间  |
|  7   | MAX_RUNNING_MINUTES |   int   | 10 |   0    |    Y     |  N   |   600    | 最大运行时间  |
|  8   | PROPS |   text   | 65535 |   0    |    Y     |  N   |       | 自定义扩展容器前端表单属性字段的JSON串  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  12   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_CONTAINER_RESOURCE_REL">T_CONTAINER_RESOURCE_REL</a>

**说明：** 流水线容器与构建资源关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CONTAINER_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建容器ID  |
|  3   | RESOURCE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 资源ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_IMAGE">T_IMAGE</a>

**说明：** 镜像信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | IMAGE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 镜像名称  |
|  3   | IMAGE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 镜像代码  |
|  4   | CLASSIFY_ID |   varchar   | 32 |   0    |    N     |  N   |       | 所属分类ID  |
|  5   | VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 版本号  |
|  6   | IMAGE_SOURCE_TYPE |   varchar   | 20 |   0    |    N     |  N   |   bkdevops    | 镜像来源，bkdevops：蓝盾源third：第三方源  |
|  7   | IMAGE_REPO_URL |   varchar   | 256 |   0    |    Y     |  N   |       | 镜像仓库地址  |
|  8   | IMAGE_REPO_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 镜像在仓库名称  |
|  9   | TICKET_ID |   varchar   | 256 |   0    |    Y     |  N   |       | ticket身份ID  |
|  10   | IMAGE_STATUS |   tinyint   | 4 |   0    |    N     |  N   |       | 镜像状态，0：初始化|1：提交中|2：验证中|3：验证失败|4：测试中|5：审核中|6：审核驳回|7：已发布|8：上架中止|9：下架中|10：已下架  |
|  11   | IMAGE_STATUS_MSG |   varchar   | 1024 |   0    |    Y     |  N   |       | 状态对应的描述，如上架失败原因  |
|  12   | IMAGE_SIZE |   varchar   | 20 |   0    |    N     |  N   |       | 镜像大小  |
|  13   | IMAGE_TAG |   varchar   | 256 |   0    |    Y     |  N   |       | 镜像tag  |
|  14   | LOGO_URL |   varchar   | 256 |   0    |    Y     |  N   |       | logo地址  |
|  15   | ICON |   text   | 65535 |   0    |    Y     |  N   |       | 镜像图标(BASE64字符串)  |
|  16   | SUMMARY |   varchar   | 256 |   0    |    Y     |  N   |       | 镜像简介  |
|  17   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 镜像描述  |
|  18   | PUBLISHER |   varchar   | 50 |   0    |    N     |  N   |   system    | 镜像发布者  |
|  19   | PUB_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 发布时间  |
|  20   | LATEST_FLAG |   bit   | 1 |   0    |    N     |  N   |       | 是否为最新版本镜像，TRUE：最新FALSE：非最新  |
|  21   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  22   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  23   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  24   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  25   | AGENT_TYPE_SCOPE |   varchar   | 64 |   0    |    N     |  N   |   []    | 支持的构建机环境  |
|  26   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 删除标识true：是，false：否  |
|  27   | DOCKER_FILE_TYPE |   varchar   | 32 |   0    |    N     |  N   |   INPUT    | dockerFile类型（INPUT：手动输入，*_LINK：链接）  |
|  28   | DOCKER_FILE_CONTENT |   text   | 65535 |   0    |    Y     |  N   |       | dockerFile内容  |

**表名：** <a id="T_IMAGE_AGENT_TYPE">T_IMAGE_AGENT_TYPE</a>

**说明：** 镜像与机器类型关联表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | IMAGE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 镜像代码  |
|  3   | AGENT_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 机器类型PUBLIC_DEVNET，PUBLIC_IDC，PUBLIC_DEVCLOUD  |

**表名：** <a id="T_IMAGE_CATEGORY_REL">T_IMAGE_CATEGORY_REL</a>

**说明：** 镜像与范畴关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | CATEGORY_ID |   varchar   | 32 |   0    |    N     |  N   |       | 镜像范畴ID  |
|  3   | IMAGE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 镜像ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_IMAGE_FEATURE">T_IMAGE_FEATURE</a>

**说明：** 镜像特性表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | IMAGE_CODE |   varchar   | 256 |   0    |    N     |  N   |       | 镜像代码  |
|  3   | PUBLIC_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否为公共镜像，TRUE：是FALSE：不是  |
|  4   | RECOMMEND_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'1'    | 是否推荐，TRUE：是FALSE：不是  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  9   | CERTIFICATION_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否官方认证，TRUE：是FALSE：不是  |
|  10   | WEIGHT |   int   | 10 |   0    |    Y     |  N   |       | 权重（数值越大代表权重越高）  |
|  11   | IMAGE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   1    | 镜像类型：0：蓝鲸官方，1：第三方  |
|  12   | DELETE_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 删除标识true：是，false：否  |

**表名：** <a id="T_IMAGE_LABEL_REL">T_IMAGE_LABEL_REL</a>

**说明：** 镜像与标签关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | LABEL_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板标签ID  |
|  3   | IMAGE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 镜像ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_IMAGE_VERSION_LOG">T_IMAGE_VERSION_LOG</a>

**说明：** 镜像版本日志表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | IMAGE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 镜像ID  |
|  3   | RELEASE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |       | 发布类型，0：新上架1：非兼容性升级2：兼容性功能更新3：兼容性问题修正  |
|  4   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 版本日志内容  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_LABEL">T_LABEL</a>

**说明：** 标签信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LABEL_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 镜像标签代码  |
|  3   | LABEL_NAME |   varchar   | 32 |   0    |    N     |  N   |       | 标签名称  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 类型  |

**表名：** <a id="T_LOGO">T_LOGO</a>

**说明：** 商店logo信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TYPE |   varchar   | 16 |   0    |    N     |  N   |       | 类型  |
|  3   | LOGO_URL |   varchar   | 512 |   0    |    N     |  N   |       | LOGOURL地址  |
|  4   | LINK |   varchar   | 512 |   0    |    Y     |  N   |       | 跳转链接  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 修改时间  |
|  9   | ORDER |   int   | 10 |   0    |    N     |  N   |   0    | 显示顺序  |

**表名：** <a id="T_REASON">T_REASON</a>

**说明：** 原因定义表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 类型  |
|  3   | CONTENT |   varchar   | 512 |   0    |    N     |  N   |       | 日志内容  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否启用  |
|  9   | ORDER |   int   | 10 |   0    |    N     |  N   |   0    | 显示顺序  |

**表名：** <a id="T_REASON_REL">T_REASON_REL</a>

**说明：** 原因和组件关联关系

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 类型  |
|  3   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  4   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  5   | REASON_ID |   varchar   | 32 |   0    |    N     |  N   |       | 原因ID  |
|  6   | NOTE |   text   | 65535 |   0    |    Y     |  N   |       | 原因说明  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_STORE_APPROVE">T_STORE_APPROVE</a>

**说明：** 审核表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  4   | CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 日志内容  |
|  5   | APPLICANT |   varchar   | 50 |   0    |    N     |  N   |       | 申请人  |
|  6   | TYPE |   varchar   | 64 |   0    |    Y     |  N   |       | 类型  |
|  7   | STATUS |   varchar   | 64 |   0    |    Y     |  N   |       | 状态  |
|  8   | APPROVER |   varchar   | 50 |   0    |    Y     |  N   |       | 批准人  |
|  9   | APPROVE_MSG |   varchar   | 256 |   0    |    Y     |  N   |       | 批准信息  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  14   | TOKEN |   varchar   | 64 |   0    |    Y     |  N   |       |   |

**表名：** <a id="T_STORE_BUILD_APP_REL">T_STORE_BUILD_APP_REL</a>

**说明：** store构建与编译环境关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | BUILD_INFO_ID |   varchar   | 32 |   0    |    N     |  N   |       | 构建信息Id(对应T_STORE_BUILD_INFO主键)  |
|  3   | APP_VERSION_ID |   int   | 10 |   0    |    Y     |  N   |       | 编译环境版本Id(对应T_APP_VERSION主键)  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_BUILD_INFO">T_STORE_BUILD_INFO</a>

**说明：** store组件构建信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | LANGUAGE |   varchar   | 64 |   0    |    Y     |  N   |       | 开发语言  |
|  3   | SCRIPT |   text   | 65535 |   0    |    N     |  N   |       | 打包脚本  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | REPOSITORY_PATH |   varchar   | 500 |   0    |    Y     |  N   |       | 代码存放路径  |
|  9   | ENABLE |   bit   | 1 |   0    |    N     |  N   |   b'1'    | 是否启用1启用0禁用  |
|  10   | SAMPLE_PROJECT_PATH |   varchar   | 500 |   0    |    N     |  N   |       | 样例工程路径  |
|  11   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |

**表名：** <a id="T_STORE_COMMENT">T_STORE_COMMENT</a>

**说明：** store组件评论信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 商店组件ID  |
|  3   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  4   | COMMENT_CONTENT |   text   | 65535 |   0    |    N     |  N   |       | 评论内容  |
|  5   | COMMENTER_DEPT |   varchar   | 200 |   0    |    N     |  N   |       | 评论者组织架构信息  |
|  6   | SCORE |   int   | 10 |   0    |    N     |  N   |       | 评分  |
|  7   | PRAISE_COUNT |   int   | 10 |   0    |    Y     |  N   |   0    | 点赞个数  |
|  8   | PROFILE_URL |   varchar   | 256 |   0    |    Y     |  N   |       | 评论者头像url地址  |
|  9   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_COMMENT_PRAISE">T_STORE_COMMENT_PRAISE</a>

**说明：** store组件评论点赞信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 评论ID  |
|  3   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  4   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  5   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  6   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_COMMENT_REPLY">T_STORE_COMMENT_REPLY</a>

**说明：** store组件评论回复信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | COMMENT_ID |   varchar   | 32 |   0    |    N     |  N   |       | 评论ID  |
|  3   | REPLY_CONTENT |   text   | 65535 |   0    |    Y     |  N   |       | 回复内容  |
|  4   | PROFILE_URL |   varchar   | 256 |   0    |    Y     |  N   |       | 评论者头像url地址  |
|  5   | REPLY_TO_USER |   varchar   | 50 |   0    |    Y     |  N   |       | 被回复者  |
|  6   | REPLYER_DEPT |   varchar   | 200 |   0    |    N     |  N   |       | 回复者组织架构信息  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_DEPT_REL">T_STORE_DEPT_REL</a>

**说明：** 商店组件与与机构关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | DEPT_ID |   int   | 10 |   0    |    N     |  N   |       | 项目所属二级机构ID  |
|  4   | DEPT_NAME |   varchar   | 1024 |   0    |    N     |  N   |       | 项目所属二级机构名称  |
|  5   | STATUS |   tinyint   | 4 |   0    |    N     |  N   |   0    | 状态  |
|  6   | COMMENT |   varchar   | 256 |   0    |    Y     |  N   |       | 评论  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  11   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |

**表名：** <a id="T_STORE_DOCKING_PLATFORM">T_STORE_DOCKING_PLATFORM</a>

**说明：** 组件对接平台信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | PLATFORM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 平台代码  |
|  3   | PLATFORM_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 平台名称  |
|  4   | WEBSITE |   varchar   | 256 |   0    |    Y     |  N   |       | 网址  |
|  5   | SUMMARY |   varchar   | 1024 |   0    |    N     |  N   |       | 简介  |
|  6   | PRINCIPAL |   varchar   | 50 |   0    |    N     |  N   |       | 负责人  |
|  7   | LOGO_URL |   varchar   | 256 |   0    |    Y     |  N   |       | 平台logo地址  |
|  8   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  9   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  10   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  11   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  12   | OWNER_DEPT_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 所属机构名称  |
|  13   | LABELS |   varchar   | 1024 |   0    |    Y     |  N   |       | 标签  |
|  14   | ERROR_CODE_PREFIX |   int   | 10 |   0    |    Y     |  N   |       | 平台所属错误码前缀  |

**表名：** <a id="T_STORE_DOCKING_PLATFORM_ERROR_CODE">T_STORE_DOCKING_PLATFORM_ERROR_CODE</a>

**说明：** 平台所属错误码信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | ERROR_CODE |   int   | 10 |   0    |    N     |  N   |       | code码  |
|  3   | ERROR_MSG_ZH_CN |   varchar   | 500 |   0    |    N     |  N   |       | 中文简体描述信息  |
|  4   | ERROR_MSG_ZH_TW |   varchar   | 500 |   0    |    Y     |  N   |       | 中文繁体描述信息  |
|  5   | ERROR_MSG_EN |   varchar   | 500 |   0    |    Y     |  N   |       | 英文描述信息  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  8   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  10   | PLATFORM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 错误码所属平台代码  |

**表名：** <a id="T_STORE_DOCKING_PLATFORM_REL">T_STORE_DOCKING_PLATFORM_REL</a>

**说明：** 组件对接平台关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 组件类型  |
|  4   | PLATFORM_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 平台代码  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_STORE_ENV_VAR">T_STORE_ENV_VAR</a>

**说明：** 环境变量信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  4   | VAR_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 变量名  |
|  5   | VAR_VALUE |   text   | 65535 |   0    |    N     |  N   |       | 变量值  |
|  6   | VAR_DESC |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  7   | SCOPE |   varchar   | 16 |   0    |    N     |  N   |       | 生效范围TEST：测试PRD：正式ALL：所有  |
|  8   | ENCRYPT_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否加密，TRUE：是FALSE：否  |
|  9   | VERSION |   int   | 10 |   0    |    N     |  N   |   1    | 版本号  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_ERROR_CODE_INFO">T_STORE_ERROR_CODE_INFO</a>

**说明：** store组件错误码信息

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    Y     |  N   |       | 组件代码,为空则表示属于通用错误码  |
|  3   | ERROR_CODE |   int   | 10 |   0    |    N     |  N   |       | 错误码  |
|  4   | STORE_TYPE |   tinyint   | 4 |   0    |    Y     |  N   |       | 组件类型0：插件1：模板  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_HONOR_INFO">T_STORE_HONOR_INFO</a>

**说明：** 研发商店荣誉信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | HONOR_TITLE |   varchar   | 10 |   0    |    N     |  N   |       | 头衔  |
|  3   | HONOR_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 荣誉名称  |
|  4   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_STORE_HONOR_REL">T_STORE_HONOR_REL</a>

**说明：** 研发商店荣誉关联信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件代码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  4   | HONOR_ID |   varchar   | 32 |   0    |    N     |  N   |       | 荣誉ID  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  9   | STORE_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 组件名称  |
|  10   | MOUNT_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否佩戴  |

**表名：** <a id="T_STORE_INDEX_BASE_INFO">T_STORE_INDEX_BASE_INFO</a>

**说明：** 研发商店指标基本信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | INDEX_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 指标标识  |
|  3   | INDEX_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 指标名称  |
|  4   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  5   | OPERATION_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 运算类型ATOM:插件PLATFORM:平台  |
|  6   | ATOM_CODE |   varchar   | 64 |   0    |    Y     |  N   |       | 指标对应的插件件代码  |
|  7   | ATOM_VERSION |   varchar   | 64 |   0    |    Y     |  N   |       | 指标对应的插件版本  |
|  8   | FINISH_TASK_NUM |   int   | 10 |   0    |    Y     |  N   |       | 完成执行任务数量  |
|  9   | TOTAL_TASK_NUM |   int   | 10 |   0    |    Y     |  N   |       | 执行任务总数  |
|  10   | EXECUTE_TIME_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 执行时间类型INDEX_CHANGE:指标变动COMPONENT_UPGRADE:组件升级CRON:定时  |
|  11   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  12   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  13   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  14   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  15   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  16   | WEIGHT |   int   | 10 |   0    |    N     |  N   |   0    | 指标展示权重  |

**表名：** <a id="T_STORE_INDEX_ELEMENT_DETAIL">T_STORE_INDEX_ELEMENT_DETAIL</a>

**说明：** 研发商店组件指标要素详情表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件代码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  4   | INDEX_ID |   varchar   | 32 |   0    |    N     |  N   |       | 指标ID  |
|  5   | INDEX_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 指标标识  |
|  6   | ELEMENT_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 指标要素名称  |
|  7   | ELEMENT_VALUE |   varchar   | 64 |   0    |    N     |  N   |       | 指标要素值  |
|  8   | REMARK |   varchar   | 1024 |   0    |    Y     |  N   |       | 备注  |
|  9   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  10   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  12   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_STORE_INDEX_LEVEL_INFO">T_STORE_INDEX_LEVEL_INFO</a>

**说明：** 研发商店指标等级信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LEVEL_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 等级名称  |
|  3   | INDEX_ID |   varchar   | 32 |   0    |    N     |  N   |       | 指标ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  7   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  8   | ICON_URL |   varchar   | 1024 |   0    |    N     |  N   |       | icon地址  |

**表名：** <a id="T_STORE_INDEX_RESULT">T_STORE_INDEX_RESULT</a>

**说明：** 研发商店组件指标结果表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件代码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  4   | INDEX_ID |   varchar   | 32 |   0    |    N     |  N   |       | 指标ID  |
|  5   | INDEX_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 指标标识  |
|  6   | ICON_TIPS |   text   | 65535 |   0    |    N     |  N   |       | 图标提示信息  |
|  7   | LEVEL_ID |   varchar   | 32 |   0    |    N     |  N   |       | 等级ID  |
|  8   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  9   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  10   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  11   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_STORE_MEDIA_INFO">T_STORE_MEDIA_INFO</a>

**说明：** 媒体信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件标识  |
|  3   | MEDIA_URL |   varchar   | 256 |   0    |    N     |  N   |       | 媒体资源链接  |
|  4   | MEDIA_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 媒体资源类型PICTURE:图片VIDEO:视频  |
|  5   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件4：微扩展  |
|  6   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  7   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  8   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  9   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 更新时间  |

**表名：** <a id="T_STORE_MEMBER">T_STORE_MEMBER</a>

**说明：** store组件成员信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | USERNAME |   varchar   | 64 |   0    |    N     |  N   |       | 用户名称  |
|  4   | TYPE |   tinyint   | 4 |   0    |    N     |  N   |       | 类型  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  9   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |

**表名：** <a id="T_STORE_OPT_LOG">T_STORE_OPT_LOG</a>

**说明：** store操作日志表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  4   | OPT_TYPE |   varchar   | 64 |   0    |    N     |  N   |       | 操作类型  |
|  5   | OPT_DESC |   varchar   | 512 |   0    |    N     |  N   |       | 操作内容  |
|  6   | OPT_USER |   varchar   | 64 |   0    |    N     |  N   |       | 操作用户  |
|  7   | OPT_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 操作时间  |
|  8   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |

**表名：** <a id="T_STORE_PIPELINE_BUILD_REL">T_STORE_PIPELINE_BUILD_REL</a>

**说明：** 商店组件构建关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 商店组件ID  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | BUILD_ID |   varchar   | 34 |   0    |    N     |  N   |       | 构建ID  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_PIPELINE_REL">T_STORE_PIPELINE_REL</a>

**说明：** 商店组件与与流水线关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 商店组件编码  |
|  3   | PIPELINE_ID |   varchar   | 34 |   0    |    N     |  N   |       | 流水线ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  8   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 商店组件类型0：插件1：模板2：镜像3：IDE插件  |
|  9   | BUS_TYPE |   varchar   | 32 |   0    |    N     |  N   |   BUILD    | 业务类型BUILD:构建INDEX:研发商店指标  |

**表名：** <a id="T_STORE_PKG_RUN_ENV_INFO">T_STORE_PKG_RUN_ENV_INFO</a>

**说明：** 组件安装包运行时环境信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 组件类型  |
|  3   | LANGUAGE |   varchar   | 64 |   0    |    N     |  N   |       | 开发语言  |
|  4   | OS_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 支持的操作系统名称  |
|  5   | OS_ARCH |   varchar   | 128 |   0    |    N     |  N   |       | 支持的操作系统架构  |
|  6   | RUNTIME_VERSION |   varchar   | 128 |   0    |    N     |  N   |       | 运行时版本  |
|  7   | PKG_NAME |   varchar   | 128 |   0    |    N     |  N   |       | 安装包名称  |
|  8   | PKG_DOWNLOAD_PATH |   varchar   | 1024 |   0    |    N     |  N   |       | 安装包下载路径  |
|  9   | DEFAULT_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否为默认安装包  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  12   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 修改时间  |
|  13   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |

**表名：** <a id="T_STORE_PROJECT_REL">T_STORE_PROJECT_REL</a>

**说明：** 商店组件与项目关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | PROJECT_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 用户组所属项目  |
|  4   | TYPE |   tinyint   | 4 |   0    |    N     |  N   |       | 类型  |
|  5   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  6   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  7   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  8   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  9   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |

**表名：** <a id="T_STORE_PUBLISHER_INFO">T_STORE_PUBLISHER_INFO</a>

**说明：** 发布者信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | PUBLISHER_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 发布者标识  |
|  3   | PUBLISHER_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 发布者名称  |
|  4   | PUBLISHER_TYPE |   varchar   | 32 |   0    |    N     |  N   |       | 发布者类型PERSON：个人，ORGANIZATION：组织  |
|  5   | OWNERS |   varchar   | 1024 |   0    |    N     |  N   |       | 主体负责人  |
|  6   | HELPER |   varchar   | 256 |   0    |    N     |  N   |       | 技术支持  |
|  7   | FIRST_LEVEL_DEPT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 一级部门ID  |
|  8   | FIRST_LEVEL_DEPT_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 一级部门名称  |
|  9   | SECOND_LEVEL_DEPT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 二级部门ID  |
|  10   | SECOND_LEVEL_DEPT_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 二级部门名称  |
|  11   | THIRD_LEVEL_DEPT_ID |   bigint   | 20 |   0    |    N     |  N   |       | 三级部门ID  |
|  12   | THIRD_LEVEL_DEPT_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 三级部门名称  |
|  13   | FOURTH_LEVEL_DEPT_ID |   bigint   | 20 |   0    |    Y     |  N   |       | 四级部门ID  |
|  14   | FOURTH_LEVEL_DEPT_NAME |   varchar   | 256 |   0    |    Y     |  N   |       | 四级部门名称  |
|  15   | ORGANIZATION_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 组织架构名称  |
|  16   | BG_NAME |   varchar   | 256 |   0    |    N     |  N   |       | 所属机构名称  |
|  17   | CERTIFICATION_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否认证标识true：是，false：否  |
|  18   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  19   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  20   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  21   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  22   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 更新时间  |

**表名：** <a id="T_STORE_PUBLISHER_MEMBER_REL">T_STORE_PUBLISHER_MEMBER_REL</a>

**说明：** 发布者成员关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | PUBLISHER_ID |   varchar   | 32 |   0    |    N     |  N   |       | 发布者ID  |
|  3   | MEMBER_ID |   varchar   | 32 |   0    |    N     |  N   |       | 成员ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建人  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 最近修改人  |
|  6   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 更新时间  |

**表名：** <a id="T_STORE_RELEASE">T_STORE_RELEASE</a>

**说明：** store组件发布升级信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件代码  |
|  3   | FIRST_PUB_CREATOR |   varchar   | 64 |   0    |    N     |  N   |       | 首次发布人  |
|  4   | FIRST_PUB_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 首次发布时间  |
|  5   | LATEST_UPGRADER |   varchar   | 64 |   0    |    N     |  N   |       | 最近升级人  |
|  6   | LATEST_UPGRADE_TIME |   datetime   | 19 |   0    |    N     |  N   |       | 最近升级时间  |
|  7   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型0：插件1：模板2：镜像3：IDE插件  |
|  8   | CREATOR |   varchar   | 64 |   0    |    N     |  N   |   system    | 创建人  |
|  9   | MODIFIER |   varchar   | 64 |   0    |    N     |  N   |   system    | 最近修改人  |
|  10   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_SENSITIVE_API">T_STORE_SENSITIVE_API</a>

**说明：** 敏感API接口信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  4   | API_NAME |   varchar   | 64 |   0    |    N     |  N   |       | API名称  |
|  5   | ALIAS_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 别名  |
|  6   | API_STATUS |   varchar   | 64 |   0    |    N     |  N   |       | API状态  |
|  7   | API_LEVEL |   varchar   | 64 |   0    |    N     |  N   |       | API等级  |
|  8   | APPLY_DESC |   varchar   | 1024 |   0    |    Y     |  N   |       | 申请说明  |
|  9   | APPROVE_MSG |   varchar   | 1024 |   0    |    Y     |  N   |       | 批准信息  |
|  10   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |       | 创建者  |
|  11   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |       | 修改者  |
|  12   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  13   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_STORE_SENSITIVE_CONF">T_STORE_SENSITIVE_CONF</a>

**说明：** store私有配置表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  4   | FIELD_NAME |   varchar   | 64 |   0    |    N     |  N   |       | 字段名称  |
|  5   | FIELD_VALUE |   text   | 65535 |   0    |    N     |  N   |       | 字段值  |
|  6   | FIELD_DESC |   text   | 65535 |   0    |    Y     |  N   |       | 字段描述  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  11   | FIELD_TYPE |   varchar   | 16 |   0    |    Y     |  N   |   BACKEND    | 字段类型  |

**表名：** <a id="T_STORE_STATISTICS">T_STORE_STATISTICS</a>

**说明：** store统计信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 商店组件ID  |
|  3   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  4   | DOWNLOADS |   int   | 10 |   0    |    Y     |  N   |       | 下载量  |
|  5   | COMMITS |   int   | 10 |   0    |    Y     |  N   |       | 评论数量  |
|  6   | SCORE |   int   | 10 |   0    |    Y     |  N   |       | 评分  |
|  7   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  8   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  9   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  10   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  11   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |

**表名：** <a id="T_STORE_STATISTICS_DAILY">T_STORE_STATISTICS_DAILY</a>

**说明：** store每日统计信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | 组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | 组件类型  |
|  4   | TOTAL_DOWNLOADS |   int   | 10 |   0    |    Y     |  N   |   0    | 总下载量  |
|  5   | DAILY_DOWNLOADS |   int   | 10 |   0    |    Y     |  N   |   0    | 每日下载量  |
|  6   | DAILY_SUCCESS_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 每日执行成功数  |
|  7   | DAILY_FAIL_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 每日执行失败总数  |
|  8   | DAILY_FAIL_DETAIL |   varchar   | 4096 |   0    |    Y     |  N   |       | 每日执行失败详情  |
|  9   | STATISTICS_TIME |   datetime   | 23 |   0    |    N     |  N   |       | 统计时间  |
|  10   | CREATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 23 |   0    |    N     |  N   |   CURRENT_TIMESTAMP(3)    | 更新时间  |

**表名：** <a id="T_STORE_STATISTICS_TOTAL">T_STORE_STATISTICS_TOTAL</a>

**说明：** store全量统计信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | STORE_CODE |   varchar   | 64 |   0    |    N     |  N   |       | store组件编码  |
|  3   | STORE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   0    | store组件类型  |
|  4   | DOWNLOADS |   int   | 10 |   0    |    Y     |  N   |   0    | 下载量  |
|  5   | COMMITS |   int   | 10 |   0    |    Y     |  N   |   0    | 评论数量  |
|  6   | SCORE |   int   | 10 |   0    |    Y     |  N   |   0    | 评分  |
|  7   | SCORE_AVERAGE |   decimal   | 4 |   1    |    Y     |  N   |   0.0    | 评论均分  |
|  8   | PIPELINE_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 流水线数量  |
|  9   | RECENT_EXECUTE_NUM |   int   | 10 |   0    |    Y     |  N   |   0    | 最近执行次数  |
|  10   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  11   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  12   | HOT_FLAG |   bit   | 1 |   0    |    Y     |  N   |   b'0'    | 是否为受欢迎组件  |

**表名：** <a id="T_TEMPLATE">T_TEMPLATE</a>

**说明：** 模板信息表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | TEMPLATE_NAME |   varchar   | 200 |   0    |    N     |  N   |       | 模板名称  |
|  3   | TEMPLATE_CODE |   varchar   | 32 |   0    |    N     |  N   |       | 模板代码  |
|  4   | CLASSIFY_ID |   varchar   | 32 |   0    |    N     |  N   |       | 所属分类ID  |
|  5   | VERSION |   varchar   | 20 |   0    |    N     |  N   |       | 版本号  |
|  6   | TEMPLATE_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   1    | 模板类型  |
|  7   | TEMPLATE_RD_TYPE |   tinyint   | 4 |   0    |    N     |  N   |   1    | 模板研发类型  |
|  8   | TEMPLATE_STATUS |   tinyint   | 4 |   0    |    N     |  N   |       | 模板状态  |
|  9   | TEMPLATE_STATUS_MSG |   varchar   | 1024 |   0    |    Y     |  N   |       | 模板状态信息  |
|  10   | LOGO_URL |   varchar   | 256 |   0    |    Y     |  N   |       | LOGOURL地址  |
|  11   | SUMMARY |   varchar   | 256 |   0    |    Y     |  N   |       | 简介  |
|  12   | DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 描述  |
|  13   | PUBLISHER |   varchar   | 50 |   0    |    N     |  N   |   system    | 原子发布者  |
|  14   | PUB_DESCRIPTION |   text   | 65535 |   0    |    Y     |  N   |       | 发布描述  |
|  15   | PUBLIC_FLAG |   bit   | 1 |   0    |    N     |  N   |   b'0'    | 是否为公共镜像  |
|  16   | LATEST_FLAG |   bit   | 1 |   0    |    N     |  N   |       | 是否为最新版本原子  |
|  17   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  18   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  19   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  20   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
|  21   | PUB_TIME |   datetime   | 19 |   0    |    Y     |  N   |       | 发布时间  |

**表名：** <a id="T_TEMPLATE_CATEGORY_REL">T_TEMPLATE_CATEGORY_REL</a>

**说明：** 模板与范畴关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | CATEGORY_ID |   varchar   | 32 |   0    |    N     |  N   |       | 镜像范畴ID  |
|  3   | TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |

**表名：** <a id="T_TEMPLATE_LABEL_REL">T_TEMPLATE_LABEL_REL</a>

**说明：** 模板与标签关联关系表

**数据列：**

| 序号 | 名称 | 数据类型 |  长度  | 小数位 | 允许空值 | 主键 | 默认值 | 说明 |
| :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
|  1   | ID |   varchar   | 32 |   0    |    N     |  Y   |       | 主键ID  |
|  2   | LABEL_ID |   varchar   | 32 |   0    |    N     |  N   |       | 标签ID  |
|  3   | TEMPLATE_ID |   varchar   | 32 |   0    |    N     |  N   |       | 模板ID  |
|  4   | CREATOR |   varchar   | 50 |   0    |    N     |  N   |   system    | 创建者  |
|  5   | MODIFIER |   varchar   | 50 |   0    |    N     |  N   |   system    | 修改者  |
|  6   | CREATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 创建时间  |
|  7   | UPDATE_TIME |   datetime   | 19 |   0    |    N     |  N   |   CURRENT_TIMESTAMP    | 更新时间  |
