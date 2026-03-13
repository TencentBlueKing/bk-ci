# Store 模块数据库表参考

## 插件相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_ATOM` | 插件主表 | `ATOM_CODE`, `NAME`, `VERSION`, `ATOM_STATUS`, `LATEST_FLAG` |
| `T_ATOM_ENV_INFO` | 插件执行环境 | `ATOM_ID`, `PKG_PATH`, `LANGUAGE`, `TARGET` |
| `T_ATOM_FEATURE` | 插件特性 | `ATOM_CODE`, `VISIBILITY_LEVEL`, `YAML_FLAG`, `QUALITY_FLAG` |
| `T_ATOM_BUILD_INFO` | 插件构建信息 | `LANGUAGE`, `SCRIPT`, `SAMPLE_PROJECT_PATH` |
| `T_ATOM_VERSION_LOG` | 版本日志 | `ATOM_ID`, `RELEASE_TYPE`, `CONTENT` |
| `T_ATOM_LABEL_REL` | 插件标签关联 | `ATOM_ID`, `LABEL_ID` |
| `T_ATOM_OFFLINE` | 插件下架记录 | `ATOM_CODE`, `EXPIRE_TIME`, `STATUS` |

## 插件核心字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ID` | String | 插件版本 ID（UUID） |
| `ATOM_CODE` | String | 插件唯一标识（不变） |
| `NAME` | String | 插件名称 |
| `VERSION` | String | 版本号（如 1.0.0） |
| `ATOM_STATUS` | Int | 插件状态 |
| `CLASS_TYPE` | String | 插件大类（marketBuild 等） |
| `JOB_TYPE` | String | 适用 Job 类型（AGENT/AGENT_LESS） |
| `OS` | String | 支持的操作系统 |
| `CLASSIFY_ID` | String | 分类 ID |
| `LATEST_FLAG` | Boolean | 是否最新版本 |
| `DEFAULT_FLAG` | Boolean | 是否默认插件 |
| `PUBLISHER` | String | 发布者 |
| `REPOSITORY_HASH_ID` | String | 代码库 HashId |

## 模板相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_TEMPLATE` | 模板主表 | `TEMPLATE_CODE`, `TEMPLATE_NAME`, `VERSION`, `TEMPLATE_STATUS` |
| `T_TEMPLATE_CATEGORY_REL` | 模板分类关联 | `TEMPLATE_ID`, `CATEGORY_ID` |
| `T_TEMPLATE_LABEL_REL` | 模板标签关联 | `TEMPLATE_ID`, `LABEL_ID` |

## 镜像相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_IMAGE` | 镜像主表 | `IMAGE_CODE`, `IMAGE_NAME`, `VERSION`, `IMAGE_STATUS` |
| `T_IMAGE_CATEGORY_REL` | 镜像分类关联 | `IMAGE_ID`, `CATEGORY_ID` |
| `T_IMAGE_LABEL_REL` | 镜像标签关联 | `IMAGE_ID`, `LABEL_ID` |

## 通用表

| 表名 | 说明 |
|------|------|
| `T_CLASSIFY` | 分类表 |
| `T_CATEGORY` | 范畴表 |
| `T_LABEL` | 标签表 |
| `T_STORE_MEMBER` | 组件成员表 |
| `T_STORE_PROJECT_REL` | 组件项目关联表 |
| `T_STORE_COMMENT` | 评论表 |
| `T_STORE_COMMENT_REPLY` | 评论回复表 |
| `T_STORE_COMMENT_PRAISE` | 评论点赞表 |
| `T_STORE_STATISTICS` | 统计表 |
| `T_STORE_APPROVE` | 审批表 |
| `T_STORE_SENSITIVE_API` | 敏感 API 表 |
| `T_STORE_SENSITIVE_CONF` | 敏感配置表 |

## 容器编译环境表

| 表名 | 说明 |
|------|------|
| `T_APPS` | 编译环境信息表 |
| `T_APP_ENV` | 编译环境变量表 |
| `T_APP_VERSION` | 编译环境版本表 |
| `T_CONTAINER` | 容器信息表 |
| `T_BUILD_RESOURCE` | 构建资源表 |
