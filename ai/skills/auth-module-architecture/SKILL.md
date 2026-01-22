---
name: auth-module-architecture
description: Auth 权限认证模块架构指南，涵盖 IAM 集成、RBAC 权限模型、资源权限校验、权限迁移、OAuth 认证。当用户开发权限功能、配置 IAM 资源、实现权限校验或处理认证流程时使用。
---

# Auth 权限认证模块架构指南

> **模块定位**: Auth 是 BK-CI 的权限认证核心模块，负责用户认证、权限校验、用户组管理、OAuth2 认证等功能，采用 RBAC（基于角色的访问控制）模型。

## 一、模块整体结构

### 1.1 子模块划分

```
src/backend/ci/core/auth/
├── api-auth/             # API 接口定义层
│   └── src/main/kotlin/com/tencent/devops/auth/
│       ├── api/
│       │   ├── callback/     # 回调接口（IAM、ITSM）
│       │   ├── login/        # 登录接口
│       │   ├── manager/      # 管理员接口
│       │   ├── migrate/      # 迁移接口
│       │   ├── oauth2/       # OAuth2 接口
│       │   ├── op/           # 运维接口
│       │   ├── open/         # 开放接口
│       │   ├── service/      # 服务间调用接口
│       │   ├── sync/         # 同步接口
│       │   └── user/         # 用户接口
│       ├── constant/         # 常量定义
│       └── pojo/             # 数据对象
│
├── biz-auth/             # 业务逻辑层
│   └── src/main/kotlin/com/tencent/devops/auth/
│       ├── aspect/           # AOP 切面
│       ├── common/           # 通用配置
│       ├── cron/             # 定时任务
│       ├── dao/              # 数据访问层（40+ 文件）
│       ├── entity/           # 实体定义
│       ├── filter/           # 过滤器
│       ├── provider/
│       │   ├── rbac/         # RBAC 实现（核心）
│       │   └── sample/       # 示例实现
│       ├── refresh/          # 刷新机制
│       ├── resources/        # API 实现
│       ├── service/          # 业务服务（30+ 文件）
│       ├── sharding/         # 分片策略
│       └── utils/            # 工具类
│
├── boot-auth/            # Spring Boot 启动模块
└── model-auth/           # 数据模型层（JOOQ 生成）
```

### 1.2 模块职责矩阵

| 模块 | 职责 | 核心类数量 |
|------|------|------------|
| **api-auth** | REST API 接口定义 | 50+ |
| **biz-auth** | 业务逻辑、RBAC 实现 | 150+ |
| **model-auth** | JOOQ 数据模型 | 自动生成 |

## 二、核心概念

### 2.1 RBAC 权限模型

```
┌─────────────────────────────────────────────────────────────────────────┐
│                           BK-CI RBAC 权限模型                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────┐     ┌──────────────┐     ┌──────────────┐                 │
│  │   用户    │────►│   用户组      │────►│   权限策略    │                 │
│  │  (User)   │     │ (Group)      │     │ (Policy)     │                 │
│  └──────────┘     └──────────────┘     └──────────────┘                 │
│       │                  │                     │                         │
│       │                  │                     ▼                         │
│       │                  │           ┌──────────────────┐               │
│       │                  │           │      操作        │               │
│       │                  │           │   (Action)       │               │
│       │                  │           │ create/view/edit │               │
│       │                  │           │ delete/execute   │               │
│       │                  │           └────────┬─────────┘               │
│       │                  │                    │                         │
│       ▼                  ▼                    ▼                         │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                        资源 (Resource)                           │   │
│  │  ┌─────────┐  ┌─────────┐  ┌─────────┐  ┌─────────┐            │   │
│  │  │ project │  │pipeline │  │ repo    │  │ env     │  ...       │   │
│  │  └─────────┘  └─────────┘  └─────────┘  └─────────┘            │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 核心实体关系

| 实体 | 说明 | 对应表 |
|------|------|--------|
| **用户 (User)** | 系统用户 | `T_AUTH_USER_INFO` |
| **用户组 (Group)** | 权限组，关联权限策略 | `T_AUTH_RESOURCE_GROUP` |
| **组成员 (Member)** | 用户组成员关系 | `T_AUTH_RESOURCE_GROUP_MEMBER` |
| **资源 (Resource)** | 被管理的资源 | `T_AUTH_RESOURCE` |
| **操作 (Action)** | 资源上的操作 | `T_AUTH_ACTION` |
| **权限 (Permission)** | 组对资源的权限 | `T_AUTH_RESOURCE_GROUP_PERMISSION` |

### 2.3 默认用户组类型

```kotlin
enum class DefaultGroupType {
    MANAGER,      // 管理员组
    DEVELOPER,    // 开发人员组
    MAINTAINER,   // 运维人员组
    TESTER,       // 测试人员组
    PM,           // 产品人员组
    QC,           // 质量管理员组
    VIEWER        // 查看者组
}
```

## 三、分层架构图

```
┌─────────────────────────────────────────────────────────────────────────┐
│                              请求入口                                    │
│           HTTP Request / 服务间调用 / IAM 回调 / OAuth2                  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         API 层 (api-auth)                                │
│  ┌──────────────┐ ┌──────────────┐ ┌──────────────┐ ┌──────────────┐    │
│  │UserAuth      │ │ServicePerm   │ │OpenProject   │ │Oauth2Service │    │
│  │ApplyResource │ │AuthResource  │ │AuthResource  │ │EndpointRes   │    │
│  │(用户权限申请) │ │(服务间鉴权)   │ │(开放项目权限) │ │(OAuth2认证)  │    │
│  └──────────────┘ └──────────────┘ └──────────────┘ └──────────────┘    │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                       业务层 (biz-auth)                                  │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      ResourceImpl 实现层                          │   │
│  │  ServicePermissionAuthResourceImpl | OpenProjectAuthResourceImpl │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      RBAC Provider 层 (核心)                      │   │
│  │  RbacPermissionService           - 权限校验核心服务                 │   │
│  │  RbacPermissionResourceGroupService - 用户组管理服务               │   │
│  │  RbacPermissionResourceMemberService - 组成员管理服务              │   │
│  │  RbacPermissionResourceService   - 资源管理服务                    │   │
│  │  PermissionGradeManagerService   - 分级管理员服务                  │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                    │                                     │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                      通用 Service 层                              │   │
│  │  PermissionAuthorizationService  - 授权服务                       │   │
│  │  AuthDeptServiceImpl             - 部门服务                       │   │
│  │  ManagerUserService              - 管理员用户服务                  │   │
│  │  StrategyService                 - 策略服务                       │   │
│  │  AuthMonitorSpaceService         - 监控空间服务                    │   │
│  └──────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DAO 层 (biz-auth/dao)                            │
│  AuthResourceGroupDao | AuthResourceGroupMemberDao | AuthResourceDao    │
│  AuthAuthorizationDao | AuthOauth2ClientDetailsDao | ...                │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      数据层 (model-auth + MySQL)                         │
│  数据库：devops_ci_auth（共 30+ 张表）                                   │
└─────────────────────────────────────────────────────────────────────────┘
```

## 四、核心数据库表

### 4.1 用户组相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_RESOURCE_GROUP` | 资源用户组 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `GROUP_CODE`, `GROUP_NAME`, `IAM_GROUP_ID` |
| `T_AUTH_RESOURCE_GROUP_MEMBER` | 组成员关系 | `ID`, `PROJECT_CODE`, `IAM_GROUP_ID`, `MEMBER_ID`, `MEMBER_TYPE`, `EXPIRED_TIME` |
| `T_AUTH_RESOURCE_GROUP_PERMISSION` | 组权限 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `IAM_GROUP_ID`, `ACTION`, `RESOURCE_CODE` |
| `T_AUTH_RESOURCE_GROUP_CONFIG` | 组配置 | `ID`, `RESOURCE_TYPE`, `GROUP_CODE`, `GROUP_NAME`, `ACTIONS` |

### 4.2 资源相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_RESOURCE` | 资源信息 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `RESOURCE_NAME`, `IAM_RESOURCE_CODE` |
| `T_AUTH_RESOURCE_TYPE` | 资源类型 | `ID`, `RESOURCE_TYPE`, `NAME`, `PARENT`, `SYSTEM` |
| `T_AUTH_ACTION` | 操作定义 | `ACTION`, `RESOURCE_TYPE`, `ACTION_NAME`, `ACTION_TYPE` |

### 4.3 授权相关表

| 表名 | 说明 | 核心字段 |
|------|------|----------|
| `T_AUTH_AUTHORIZATION` | 资源授权 | `ID`, `PROJECT_CODE`, `RESOURCE_TYPE`, `RESOURCE_CODE`, `HANDOVER_FROM`, `HANDOVER_TO` |
| `T_AUTH_IAM_CALLBACK` | IAM 回调 | `ID`, `GATEWAY`, `PATH`, `RESOURCE`, `SYSTEM` |

### 4.4 OAuth2 相关表

| 表名 | 说明 |
|------|------|
| `T_AUTH_OAUTH2_CLIENT_DETAILS` | OAuth2 客户端信息 |
| `T_AUTH_OAUTH2_ACCESS_TOKEN` | 访问令牌 |
| `T_AUTH_OAUTH2_REFRESH_TOKEN` | 刷新令牌 |
| `T_AUTH_OAUTH2_CODE` | 授权码 |
| `T_AUTH_OAUTH2_SCOPE` | 授权范围 |

## 五、核心类速查

### 5.1 API 接口层

| 类名 | 路径前缀 | 职责 |
|------|----------|------|
| `ServicePermissionAuthResource` | `/service/auth/permission` | 服务间权限校验 |
| `ServiceProjectAuthResource` | `/service/auth/project` | 服务间项目权限 |
| `ServiceResourceGroupResource` | `/service/auth/resource/group` | 用户组管理 |
| `ServiceResourceMemberResource` | `/service/auth/resource/member` | 组成员管理 |
| `UserAuthApplyResource` | `/user/auth/apply` | 用户权限申请 |
| `UserAuthAuthorizationResource` | `/user/auth/authorization` | 用户授权管理 |
| `OpenPermissionAuthResource` | `/open/auth/permission` | 开放权限接口 |
| `OpenProjectAuthResource` | `/open/auth/project` | 开放项目权限 |
| `Oauth2ServiceEndpointResource` | `/service/oauth2` | OAuth2 服务端点 |

### 5.2 RBAC Provider 层（核心服务）

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `RbacPermissionService` | 32KB | 权限校验核心，对接 IAM SDK |
| `RbacPermissionResourceGroupService` | 33KB | 用户组 CRUD 管理 |
| `RbacPermissionResourceMemberService` | 29KB | 组成员管理 |
| `RbacPermissionResourceService` | 21KB | 资源注册与管理 |
| `RbacPermissionManageFacadeServiceImpl` | 117KB | 权限管理门面（最大） |
| `RbacPermissionResourceGroupPermissionService` | 39KB | 组权限管理 |
| `RbacPermissionResourceGroupSyncService` | 37KB | 组同步服务 |
| `PermissionGradeManagerService` | 27KB | 分级管理员 |
| `RbacPermissionApplyService` | 30KB | 权限申请服务 |

### 5.3 通用 Service 层

| 类名 | 职责 |
|------|------|
| `PermissionAuthorizationServiceImpl` | 资源授权（代持人）管理 |
| `AuthDeptServiceImpl` | 部门信息服务 |
| `ManagerUserService` | 超级管理员管理 |
| `StrategyService` | 权限策略管理 |
| `ThirdLoginService` | 第三方登录 |
| `AuthUserBlackListService` | 用户黑名单 |

### 5.4 DAO 层

| 类名 | 文件大小 | 职责 |
|------|----------|------|
| `AuthResourceGroupMemberDao` | 34KB | 组成员数据访问（最大） |
| `AuthResourceGroupDao` | 19KB | 用户组数据访问 |
| `AuthResourceDao` | 15KB | 资源数据访问 |
| `AuthResourceGroupPermissionDao` | 11KB | 组权限数据访问 |
| `AuthAuthorizationDao` | 10KB | 授权数据访问 |

## 六、核心流程

### 6.1 权限校验流程

```
用户请求
    │
    ▼
ServicePermissionAuthResource.validateUserResourcePermission()
    │
    ▼
ServicePermissionAuthResourceImpl
    │
    ▼
RbacPermissionService.validateUserResourcePermission()
    │
    ├─► 检查是否超级管理员 (SuperManagerService)
    ├─► 检查项目成员缓存 (BkInternalPermissionCache)
    │
    ▼
AuthHelper.isAllowed()  ← IAM SDK
    │
    ├─► 构建 ActionDTO (操作)
    ├─► 构建 ResourceDTO (资源)
    └─► 调用 IAM 策略引擎
```

### 6.2 用户组创建流程

```
UserAuthApplyResource.createGroup()
    │
    ▼
RbacPermissionResourceGroupService.createGroup()
    │
    ├─► 校验组名长度 (5-32字符)
    ├─► 检查组名是否重复
    │
    ▼
iamV2ManagerService.createRoleGroup()  ← IAM SDK
    │
    ▼
authResourceGroupDao.create()  ← 保存到本地数据库
    │
    ▼
permissionResourceGroupPermissionService.grantGroupPermission()  ← 授予权限
```

### 6.3 OAuth2 认证流程

```
┌─────────────────────────────────────────────────────────────────┐
│                     OAuth2 授权码模式                            │
├─────────────────────────────────────────────────────────────────┤
│  1. 客户端请求授权                                               │
│     GET /oauth2/authorize?client_id=xxx&redirect_uri=xxx        │
│                          │                                       │
│                          ▼                                       │
│  2. 用户登录并授权                                               │
│     Oauth2DesktopEndpointResource.authorize()                   │
│                          │                                       │
│                          ▼                                       │
│  3. 返回授权码                                                   │
│     redirect_uri?code=xxx                                        │
│                          │                                       │
│                          ▼                                       │
│  4. 客户端用授权码换取 Token                                     │
│     POST /oauth2/token                                           │
│     Oauth2ServiceEndpointResource.getToken()                    │
│                          │                                       │
│                          ▼                                       │
│  5. 返回 Access Token + Refresh Token                           │
└─────────────────────────────────────────────────────────────────┘
```

## 七、与 IAM 集成

### 7.1 IAM SDK 依赖

Auth 模块深度集成腾讯蓝鲸 IAM（权限中心），核心依赖：

```kotlin
// IAM SDK 核心类
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
```

### 7.2 IAM 回调接口

```kotlin
// 资源回调接口 - IAM 拉取资源信息
@Path("/open/auth/resource/callback")
interface OpenAuthResourceCallBackResource {
    fun getResource(...)      // 获取资源详情
    fun listResource(...)     // 列出资源
    fun searchResource(...)   // 搜索资源
}
```

### 7.3 资源类型定义

```kotlin
enum class AuthResourceType(val value: String) {
    PROJECT("project"),                    // 项目
    PIPELINE_DEFAULT("pipeline"),          // 流水线
    PIPELINE_GROUP("pipeline_group"),      // 流水线组
    PIPELINE_TEMPLATE("pipeline_template"),// 流水线模板
    CREDENTIAL("credential"),              // 凭证
    CERT("cert"),                          // 证书
    CGS("cgs"),                            // 代码扫描
    ENVIRONMENT_ENVIRONMENT("environment"),// 环境
    ENVIRONMENT_ENV_NODE("env_node"),      // 环境节点
    CODE_REPERTORY("repertory"),           // 代码库
    EXPERIENCE_TASK("experience_task"),    // 体验任务
    EXPERIENCE_GROUP("experience_group"),  // 体验组
    QUALITY_RULE("rule"),                  // 质量规则
    QUALITY_GROUP("quality_group"),        // 质量红线组
}
```

## 八、配置说明

### 8.1 RBAC 配置类

```kotlin
// 位置：biz-auth/provider/rbac/config/RbacAuthConfiguration.kt (37KB)
@Configuration
class RbacAuthConfiguration {
    // IAM 客户端配置
    // 权限服务 Bean 定义
    // 缓存配置
}
```

### 8.2 MQ 配置

```kotlin
// 位置：biz-auth/provider/rbac/config/RbacMQConfiguration.kt
// 定义权限相关的消息队列
// - 组创建事件
// - 组修改事件
// - ITSM 回调事件
// - 权限同步事件
```

## 九、开发规范

### 9.1 新增权限操作

1. 在 `T_AUTH_ACTION` 表添加操作定义
2. 在 `AuthPermission` 枚举中添加对应值
3. 在 IAM 系统注册操作
4. 更新 `RbacAuthUtils.buildAction()` 映射

### 9.2 新增资源类型

1. 在 `AuthResourceType` 枚举中添加类型
2. 在 `T_AUTH_RESOURCE_TYPE` 表添加记录
3. 实现 `AuthResourceCallBackResource` 回调接口
4. 在 IAM 系统注册资源类型

### 9.3 权限校验示例

```kotlin
// 服务间调用校验权限
client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
    userId = userId,
    token = token,
    action = AuthPermission.EXECUTE.value,
    projectCode = projectCode,
    resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
)

// 使用 AuthPermissionApi
authPermissionApi.validateUserResourcePermission(
    user = userId,
    serviceCode = authServiceCode,
    resourceType = AuthResourceType.PIPELINE_DEFAULT,
    projectCode = projectCode,
    resourceCode = pipelineId,
    permission = AuthPermission.EXECUTE
)
```

## 十、常见问题

**Q: 如何判断用户是否是项目成员？**
A: 调用 `RbacPermissionService.validateUserProjectPermission()` 或检查用户是否在任意项目用户组中。

**Q: 如何给用户授权？**
A: 将用户添加到对应的用户组 `RbacPermissionResourceMemberService.addGroupMember()`。

**Q: OAuth2 支持哪些授权模式？**
A: 支持授权码模式（Authorization Code）和密码模式（Password）。

**Q: 如何处理权限缓存？**
A: 使用 `BkInternalPermissionCache` 进行缓存，通过 Redis 存储，支持主动刷新。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-10
