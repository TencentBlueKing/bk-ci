---
name: project-module-architecture
description: Project 项目管理模块架构指南，涵盖项目 CRUD、成员管理、项目配置、标签管理、项目迁移。当用户开发项目管理功能、处理项目成员、配置项目属性或实现项目相关逻辑时使用。
---

# Project 项目管理模块架构指南

> **模块定位**: Project 是 BK-CI 的基础模块，负责项目的创建、管理、权限、配置等功能。所有其他微服务都依赖 Project 模块，项目是 BK-CI 中资源隔离的最小单位。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/project/
├── api-project/          # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/project/
│       ├── api/
│       │   ├── op/           # 运维接口（7文件）
│       │   ├── open/         # 开放接口
│       │   ├── service/      # 服务间调用接口（10文件）
│       │   └── user/         # 用户接口（6文件）
│       ├── constant/         # 常量定义
│       └── pojo/             # 数据对象（60+ 文件）
│           ├── code/         # 消息码
│           └── enums/        # 枚举定义
│
├── biz-project/          # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/project/
│       ├── config/           # 配置类
│       ├── dao/              # 数据访问层（20+ 文件）
│       ├── dispatch/         # 事件分发
│       ├── jmx/              # JMX 监控
│       ├── listener/         # 事件监听
│       ├── pojo/             # 内部数据对象
│       ├── resources/        # API 实现（25+ 文件）
│       ├── service/          # 业务服务
│       │   ├── impl/         # 服务实现（15+ 文件）
│       │   └── permission/   # 权限服务
│       └── util/             # 工具类
│
├── api-project-sample/   # 示例 API（扩展用）
├── biz-project-sample/   # 示例业务（扩展用）
├── boot-project/         # Spring Boot 启动模块
└── model-project/        # 数据模型层（JOOQ 生成）
```

### 1.2 模块职责矩阵

| 模块 | 职责 | 核心类数量 |
|------|------|------------|
| **api-project** | REST API 接口定义、POJO | 100+ |
| **biz-project** | 业务逻辑、API 实现 | 120+ |
| **model-project** | JOOQ 数据模型 | 自动生成 |

## 二、核心概念

### 2.1 项目实体模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           BK-CI 项目模型                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                        项目 (Project)                             │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ PROJECT_ID  │  │ project_name│  │ english_name            │   │   │
│  │  │ (UUID,不用) │  │ (项目名称)   │  │ (真正的projectId!)      │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  │                                                                   │   │
│  │  ⚠️ 注意：其他模块说的 projectId 就是 english_name                │   │
│  │                                                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │                    组织架构信息                              │ │   │
│  │  │  bgId/bgName | deptId/deptName | centerId/centerName        │ │   │
│  │  │  businessLineId/businessLineName                            │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  │                                                                   │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │                    项目属性 (Properties)                     │ │   │
│  │  │  pipelineDialectType | pipelineAsCodeSettings              │ │   │
│  │  │  pipelineLimit | subjectScopes                             │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     关联资源                                      │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │   │
│  │  │pipeline │  │ repo    │  │ ticket  │  │ env     │  ...       │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 项目核心字段

| 字段 | 类型 | 说明 |
|------|------|------|
| `ID` | Long | 数据库自增主键 |
| `PROJECT_ID` | String | 项目唯一标识（UUID，**仅内部使用，其他模块不使用此字段**） |
| `project_name` | String | 项目中文名称（唯一） |
| `english_name` | String | **项目英文标识（唯一，这才是其他模块所说的 projectId）** |
| `creator` | String | 创建者 |
| `CHANNEL` | String | 项目渠道（BS/CODECC/AM等） |
| `approval_status` | Int | 审批状态 |
| `enabled` | Boolean | 是否启用 |
| `is_secrecy` | Boolean | 是否保密项目 |
| `properties` | JSON | 项目扩展属性 |
| `subject_scopes` | JSON | 可授权人员范围 |

> ⚠️ **重要说明：projectId 的真正含义**
> 
> 在 `T_PROJECT` 表中，`PROJECT_ID` 字段是一个 UUID，**但这个字段几乎没有实际用途**。
> 
> **对于其他所有微服务（process、auth、repository、artifactory 等）来说，它们接口中的 `projectId` 参数实际上指的是 `T_PROJECT.english_name` 字段！**
> 
> 例如：
> - `ServiceProcessResource.list(projectId)` → 这里的 `projectId` 是 `english_name`
> - `ServiceAuthResource.validatePermission(projectId)` → 这里的 `projectId` 也是 `english_name`
> - URL 路径 `/api/user/projects/{projectId}/pipelines` → 这里的 `{projectId}` 是 `english_name`
> 
> **命名约定**：
> - 在代码中，`projectId` / `projectCode` / `englishName` 都指同一个东西：`T_PROJECT.english_name`
> - `T_PROJECT.PROJECT_ID`（UUID）基本不使用，可以忽略

### 2.3 项目渠道类型

```kotlin
enum class ProjectChannelCode(val code: String) {
    BS("BS"),           // 蓝盾平台
    CODECC("CODECC"),   // 代码检查
    AM("AM"),           // 应用市场
    GCLOUD("GCLOUD"),   // 游戏云
    GITCI("GITCI"),     // GitCI
    STREAM("STREAM"),   // Stream
    PAAS("PAAS"),       // PaaS
}
```

### 2.4 项目审批状态

```kotlin
enum class ProjectApproveStatus(val status: Int) {
    CREATE_PENDING(1),      // 创建审批中
    CREATE_APPROVED(2),     // 创建已通过
    CREATE_REJECTED(3),     // 创建已驳回
    UPDATE_PENDING(4),      // 更新审批中
    UPDATE_APPROVED(5),     // 更新已通过
    UPDATE_REJECTED(6),     // 更新已驳回
}
```

## 三、分层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
│                    HTTP Request / 服务间调用                             │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-project)                             │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │UserProject   │ │ServiceProject│ │OPProject     │ │OpenProject   │    │
│  │Resource      │ │Resource      │ │Resource      │ │Resource      │    │
│  │(用户项目管理) │ │(服务间调用)   │ │(运维管理)     │ │(开放接口)    │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐                     │
│  │ServiceProject│ │OPDataSource  │ │OPSharding    │                     │
│  │TagResource   │ │Resource      │ │RoutingRule   │                     │
│  │(项目标签)     │ │(数据源管理)   │ │(分片路由)     │                     │
│  └──────────────┘ └──────────────┘ └──────────────┘                     │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-project)                               │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      ResourceImpl 实现层                          │   │
│  │  UserProjectResourceImpl | ServiceProjectResourceImpl | ...      │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Service 层                                   │   │
│  │  AbsProjectServiceImpl       - 项目核心服务（77KB，最大）          │   │
│  │  ProjectApprovalService      - 项目审批服务                       │   │
│  │  ProjectTagService           - 项目标签服务                       │   │
│  │  ProjectPermissionService    - 项目权限服务                       │   │
│  │  ShardingRoutingRuleService  - 分片路由服务                       │   │
│  │  UserLocaleService           - 用户语言服务                       │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      Permission 层                                │   │
│  │  ProjectPermissionServiceImpl     - 默认权限实现                  │   │
│  │  RbacProjectPermissionService     - RBAC 权限实现                 │   │
│  │  StreamProjectPermissionServiceImpl - Stream 权限实现             │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层 (biz-project/dao)                         │
│  ProjectDao (42KB) | ProjectApprovalDao | ShardingRoutingRuleDao        │
│  UserDao | ServiceDao | GrayTestDao | NoticeDao | ...                   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-project + MySQL)                      │
│  数据库：devops_ci_project（共 15+ 张表）                                │
└─────────────────────────────────────────────────────────────────────────┘
```

## 四、核心数据库表

### 4.1 项目核心表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_PROJECT` | 项目信息表 | `ID`, `PROJECT_ID`, `project_name`, `english_name`, `creator`, `CHANNEL`, `approval_status`, `enabled`, `properties` |
| `T_PROJECT_APPROVAL` | 项目审批表 | `ID`, `PROJECT_ID`, `APPLICANT`, `APPROVAL_STATUS`, `APPROVER`, `TIPS_STATUS` |
| `T_PROJECT_UPDATE_HISTORY` | 项目更新历史 | `ID`, `PROJECT_ID`, `BEFORE_INFO`, `AFTER_INFO`, `OPERATOR` |
| `T_PROJECT_LABEL` | 项目标签定义 | `ID`, `LABEL_NAME`, `CREATE_USER` |
| `T_PROJECT_LABEL_REL` | 项目标签关联 | `ID`, `PROJECT_ID`, `LABEL_ID` |

### 4.2 用户相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_USER` | 用户信息表 | `USER_ID`, `NAME`, `BG_ID`, `DEPT_ID`, `CENTER_ID`, `USER_TYPE` |
| `T_USER_DAILY_LOGIN` | 用户登录记录 | `USER_ID`, `DATE`, `LOGIN_TIME`, `OS`, `IP` |
| `T_USER_LOCALE` | 用户语言设置 | `USER_ID`, `LANGUAGE` |
| `T_FAVORITE` | 用户收藏 | `service_id`, `username` |

### 4.3 服务与配置表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_SERVICE` | 服务信息表 | `id`, `name`, `english_name`, `link`, `status` |
| `T_SERVICE_TYPE` | 服务类型 | `id`, `title`, `english_title`, `weight` |
| `T_ACTIVITY` | 活动信息 | `ID`, `TYPE`, `NAME`, `LINK`, `STATUS` |
| `T_NOTICE` | 公告信息 | `ID`, `NOTICE_TITLE`, `EFFECT_DATE`, `INVALID_DATE` |

### 4.4 分片相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_SHARDING_ROUTING_RULE` | 分片路由规则 | `ID`, `CLUSTER_NAME`, `MODULE_CODE`, `DATA_SOURCE_NAME`, `TABLE_NAME`, `ROUTING_NAME`, `ROUTING_RULE` |
| `T_TABLE_SHARDING_CONFIG` | 表分片配置 | `ID`, `CLUSTER_NAME`, `MODULE_CODE`, `TABLE_NAME`, `SHARDING_NUM` |
| `T_DATA_SOURCE` | 数据源配置 | `ID`, `CLUSTER_NAME`, `MODULE_CODE`, `DATA_SOURCE_NAME`, `DS_URL` |

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `UserProjectResource` | `/user/projects` | 用户项目管理（创建/更新/查询） |
| `ServiceProjectResource` | `/service/projects` | 服务间项目查询 |
| `OPProjectResource` | `/op/projects` | 运维项目管理 |
| `OPProjectServiceResource` | `/op/project/service` | 运维服务管理 |
| `OpenProjectResource` | `/open/projects` | 开放项目接口 |
| `ServiceProjectTagResource` | `/service/project/tags` | 项目标签服务 |
| `ServiceShardingRoutingRuleResource` | `/service/sharding/routing/rules` | 分片路由规则 |
| `ServiceProjectApprovalResource` | `/service/project/approval` | 项目审批服务 |
| `UserLocaleResource` | `/user/locale` | 用户语言设置 |

### 5.2 Service 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `AbsProjectServiceImpl` | 77KB | 项目核心服务（最大，抽象基类） |
| `ProjectApprovalService` | 18KB | 项目审批流程 |
| `ProjectTagService` | 16KB | 项目标签管理 |
| `AbsShardingRoutingRuleServiceImpl` | 16KB | 分片路由规则 |
| `AbsUserProjectServiceServiceImpl` | 19KB | 用户项目服务 |
| `RbacProjectPermissionService` | 10KB | RBAC 项目权限 |
| `I18nMessageServiceImpl` | 11KB | 国际化消息 |

### 5.3 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `ProjectDao` | 42KB | 项目数据访问（最大） |
| `ProjectApprovalDao` | 14KB | 项目审批数据访问 |
| `ServiceDao` | 10KB | 服务数据访问 |
| `ShardingRoutingRuleDao` | 7KB | 分片路由数据访问 |
| `TableShardingConfigDao` | 6KB | 表分片配置数据访问 |
| `UserDao` | 6KB | 用户数据访问 |

## 六、核心流程

### 6.1 项目创建流程

```
用户请求
    │
    ▼
UserProjectResource.create()
    │
    ▼
UserProjectResourceImpl.create()
    │
    ▼
AbsProjectServiceImpl.create()
    │
    ├─► 参数校验
    │   ├─► 校验项目名称长度（2-64字符）
    │   ├─► 校验英文名格式（小写字母+数字+下划线）
    │   └─► 校验项目名/英文名唯一性
    │
    ├─► 权限校验
    │   └─► projectPermissionService.verifyUserProjectPermission()
    │
    ├─► 创建项目
    │   ├─► 生成 projectId (UUID)
    │   ├─► projectDao.create()
    │   └─► 分配分片路由规则
    │
    ├─► 注册到权限中心
    │   └─► authPermissionApi.createResource()
    │
    └─► 发送项目创建事件
        └─► projectDispatcher.dispatch(ProjectCreateBroadCastEvent)
```

### 6.2 项目查询流程

```
ServiceProjectResource.list()
    │
    ▼
ServiceProjectResourceImpl.list()
    │
    ▼
AbsProjectServiceImpl.list()
    │
    ├─► 获取用户有权限的项目列表
    │   └─► projectPermissionService.getUserProjects()
    │
    ├─► 查询项目详情
    │   └─► projectDao.listByEnglishName()
    │
    └─► 组装返回数据
        └─► ProjectVO
```

### 6.3 项目审批流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     项目审批流程                                 │
├─────────────────────────────────────────────────────────────────┤
│  1. 用户提交创建/更新请求                                        │
│     UserProjectResource.create() / update()                     │
│                          │                                       │
│                          ▼                                       │
│  2. 检查是否需要审批                                             │
│     ProjectApprovalService.checkApprovalRequired()              │
│                          │                                       │
│           ┌──────────────┴──────────────┐                       │
│           │                              │                       │
│           ▼                              ▼                       │
│     需要审批                         不需要审批                   │
│     创建审批单                       直接生效                    │
│     status=PENDING                   status=APPROVED            │
│           │                                                      │
│           ▼                                                      │
│  3. 审批人审批                                                   │
│     OPProjectResource.approve()                                 │
│                          │                                       │
│           ┌──────────────┴──────────────┐                       │
│           │                              │                       │
│           ▼                              ▼                       │
│        通过                           驳回                       │
│     status=APPROVED                status=REJECTED              │
│     项目生效                         通知用户                    │
└─────────────────────────────────────────────────────────────────┘
```

## 七、与其他模块的关系

### 7.1 被依赖关系

Project 模块是基础模块，被所有其他微服务依赖：

```
┌─────────────────────────────────────────────────────────────────┐
│                    Project 模块被依赖关系                        │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │
│  │ process │  │  auth   │  │  store  │  │  repo   │            │
│  └────┬────┘  └────┬────┘  └────┬────┘  └────┬────┘            │
│       │            │            │            │                   │
│       └────────────┴────────────┴────────────┘                   │
│                          │                                       │
│                          ▼                                       │
│                  ┌───────────────┐                               │
│                  │    project    │                               │
│                  │  (基础模块)    │                               │
│                  └───────────────┘                               │
│                                                                  │
│  其他依赖模块：                                                   │
│  - artifactory, dispatch, environment, ticket                   │
│  - quality, notify, log, openapi, metrics                       │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### 7.2 服务间调用示例

```kotlin
// 其他服务调用 Project 服务获取项目信息
// 注意：这里的 projectId 参数实际是 T_PROJECT.english_name
client.get(ServiceProjectResource::class).get(
    userId = userId,
    projectId = projectId,  // ⚠️ 这是 english_name，不是 T_PROJECT.PROJECT_ID
    accessToken = null
)

// 获取用户有权限的项目列表
client.get(ServiceProjectResource::class).list(
    userId = userId,
    productIds = null,
    channelCodes = null,
    sort = null,
    page = null,
    pageSize = null
)

// 典型的 URL 路径示例：
// GET /api/user/projects/{projectId}/pipelines
// 这里的 {projectId} 就是 english_name，例如 "my-project-001"
```

## 八、项目属性扩展

### 8.1 ProjectProperties 结构

```kotlin
data class ProjectProperties(
    val pipelineDialectType: PipelineDialectType?,    // 流水线方言类型
    val pipelineAsCodeSettings: PipelineAsCodeSettings?, // PAC 设置
    val pipelineLimit: Int?,                          // 流水线数量限制
    val pluginDetailsDisplayOrder: PluginDetailsDisplayOrder?, // 插件详情显示顺序
    val pipelineNameFormat: String?,                  // 流水线命名格式
    val concurrencyQuota: Int?                        // 并发配额
)
```

### 8.2 SubjectScopes（可授权范围）

```kotlin
data class SubjectScopeInfo(
    val type: SubjectScopeType,  // ALL_MEMBER / DEPARTMENT / USER / GROUP
    val id: String,              // 部门ID / 用户ID / 组ID
    val name: String             // 名称
)
```

## 九、分片路由机制

### 9.1 分片策略

Project 模块支持数据库分片，用于大规模部署：

```kotlin
// 分片路由规则分配
shardingRoutingRuleAssignService.assignShardingRoutingRule(
    projectId = projectId,
    moduleCodes = listOf(
        SystemModuleEnum.PROCESS,
        SystemModuleEnum.REPOSITORY,
        SystemModuleEnum.DISPATCH
    )
)
```

### 9.2 路由规则查询

```kotlin
// 获取项目的分片路由规则
client.get(ServiceShardingRoutingRuleResource::class).getShardingRoutingRuleByName(
    routingName = projectId,
    moduleCode = SystemModuleEnum.PROCESS
)
```

## 十、开发规范

### 10.1 新增项目属性

1. 在 `ProjectProperties` 中添加字段
2. 在 `ProjectVO` 和 `ProjectCreateInfo` 中添加对应字段
3. 更新 `ProjectDao` 的查询和更新方法
4. 如需审批，更新 `ProjectApprovalService`

### 10.2 新增项目渠道

1. 在 `ProjectChannelCode` 枚举中添加渠道
2. 在 `T_PROJECT` 表的 `CHANNEL` 字段支持新值
3. 根据需要调整权限校验逻辑

### 10.3 项目查询示例

```kotlin
// 根据英文名查询项目（englishName 就是其他模块所说的 projectId）
val project = projectDao.getByEnglishName(
    dslContext = dslContext,
    englishName = projectCode  // projectCode = projectId = englishName
)

// 查询用户有权限的项目
val projects = projectPermissionService.getUserProjects(userId)

// 分页查询项目列表
val page = projectDao.list(
    dslContext = dslContext,
    projectName = searchName,
    englishName = null,
    enabled = true,
    offset = offset,
    limit = limit
)

// ⚠️ 重要：当其他模块传入 projectId 时，应该用 getByEnglishName 查询
// 例如 process 模块调用：
// val projectInfo = client.get(ServiceProjectResource::class).get(projectId = "my-project")
// 这里的 "my-project" 是 english_name
```

## 十一、常见问题

**Q: projectId 和 englishName 的区别？**
A: ⚠️ **这是一个常见的误解**：
- `T_PROJECT.PROJECT_ID` 是一个 UUID，**但几乎没有实际用途**
- `T_PROJECT.english_name` 才是真正的项目标识
- **其他所有模块接口中的 `projectId` 参数，实际上都是 `english_name`**
- 简单记忆：`projectId` = `projectCode` = `englishName` = `T_PROJECT.english_name`

**Q: 如何判断项目是否启用？**
A: 检查 `T_PROJECT.enabled` 字段，`true` 表示启用。

**Q: 项目创建后如何通知其他服务？**
A: 通过 `ProjectCreateBroadCastEvent` 事件广播，其他服务监听该事件进行初始化。

**Q: 如何扩展项目属性？**
A: 在 `ProjectProperties` 中添加字段，存储在 `T_PROJECT.properties` JSON 字段中。

**Q: 保密项目和普通项目的区别？**
A: 保密项目（`authSecrecy=true`）有更严格的权限控制，只有明确授权的用户才能访问。

---

**版本**: 1.1.0 | **更新日期**: 2025-12-11

**更新说明**: 
- v1.1.0: 修正 projectId 的真正含义说明，明确 `T_PROJECT.english_name` 才是其他模块所说的 projectId
