---
name: auth-module-architecture
description: Auth 权限认证模块架构指南，涵盖 IAM 集成、RBAC 权限模型、资源权限校验、权限迁移、OAuth 认证。当用户开发权限功能、配置 IAM 资源、实现权限校验或处理认证流程时使用。
---

# Auth 权限认证模块架构指南

> **模块定位**: Auth 是 BK-CI 的权限认证核心模块，负责用户认证、权限校验、用户组管理、OAuth2 认证等功能，采用 RBAC（基于角色的访问控制）模型。

## 一、模块整体结构

```
src/backend/ci/core/auth/
├── api-auth/          # REST API 接口定义层（50+ 接口类）
├── biz-auth/          # 业务逻辑层（150+ 核心类）
│   └── provider/rbac/ # RBAC 实现（核心）
├── boot-auth/         # Spring Boot 启动模块
└── model-auth/        # JOOQ 自动生成的数据模型层
```

详细类速查见 [REFERENCE.md](./REFERENCE.md)，数据库表结构见 [TABLES.md](./TABLES.md)。

## 二、RBAC 权限模型

核心实体关系：**用户** -> **用户组** -> **权限策略** -> **操作** -> **资源**

| 实体 | 对应表 | 说明 |
|------|--------|------|
| 用户 (User) | `T_AUTH_USER_INFO` | 系统用户 |
| 用户组 (Group) | `T_AUTH_RESOURCE_GROUP` | 关联权限策略 |
| 组成员 (Member) | `T_AUTH_RESOURCE_GROUP_MEMBER` | 用户组成员关系 |
| 资源 (Resource) | `T_AUTH_RESOURCE` | 被管理的资源 |
| 操作 (Action) | `T_AUTH_ACTION` | 资源上的操作 |
| 权限 (Permission) | `T_AUTH_RESOURCE_GROUP_PERMISSION` | 组对资源的权限 |

### 默认用户组类型

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

### 资源类型定义

```kotlin
enum class AuthResourceType(val value: String) {
    PROJECT("project"),                    // 项目
    PIPELINE_DEFAULT("pipeline"),          // 流水线
    PIPELINE_GROUP("pipeline_group"),      // 流水线组
    PIPELINE_TEMPLATE("pipeline_template"),// 流水线模板
    CREDENTIAL("credential"),              // 凭证
    CERT("cert"),                          // 证书
    ENVIRONMENT_ENVIRONMENT("environment"),// 环境
    ENVIRONMENT_ENV_NODE("env_node"),      // 环境节点
    CODE_REPERTORY("repertory"),           // 代码库
    QUALITY_RULE("rule"),                  // 质量规则
    // 完整列表见 AuthResourceType.kt
}
```

## 三、核心流程

### 3.1 权限校验流程

```
用户请求 -> ServicePermissionAuthResource.validateUserResourcePermission()
         -> RbacPermissionService.validateUserResourcePermission()
            ├─ 检查是否超级管理员 (SuperManagerService)
            ├─ 检查项目成员缓存 (BkInternalPermissionCache)
            └─ AuthHelper.isAllowed() -> IAM 策略引擎
```

#### 完整权限校验示例

```kotlin
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType

// 方式一：服务间调用校验 - 检查用户对流水线的执行权限
val result = client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
    userId = "user001",
    token = "service-token",
    action = AuthPermission.EXECUTE.value,
    projectCode = "my-project",
    resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
)
if (!result.data!!) {
    throw PermissionForbiddenException("用户无执行权限")
}

// 方式二：通过 AuthPermissionApi 校验 - 检查用户对特定流水线的执行权限
import com.tencent.devops.common.auth.api.AuthPermissionApi

authPermissionApi.validateUserResourcePermission(
    user = "user001",
    serviceCode = authServiceCode,
    resourceType = AuthResourceType.PIPELINE_DEFAULT,
    projectCode = "my-project",
    resourceCode = "p-abc123",  // 具体流水线ID
    permission = AuthPermission.EXECUTE
)
```

**验证**: 校验成功返回 `Result(true)`，失败返回 `Result(false)` 或抛出 `PermissionForbiddenException`。可在日志中搜索 `validateUserResourcePermission` 确认调用链路。

### 3.2 用户组创建流程

```
UserAuthApplyResource.createGroup()
  -> RbacPermissionResourceGroupService.createGroup()
     ├─ 校验组名长度 (5-32字符)
     ├─ 检查组名是否重复
     ├─ iamV2ManagerService.createRoleGroup()  <- IAM SDK
     ├─ authResourceGroupDao.create()          <- 保存本地数据库
     └─ permissionResourceGroupPermissionService.grantGroupPermission()
```

**验证**: 创建成功后，检查 `T_AUTH_RESOURCE_GROUP` 表中是否有对应记录，且 `IAM_GROUP_ID` 非空（表示 IAM 侧同步成功）。

### 3.3 OAuth2 授权码流程

```
1. GET  /oauth2/authorize?client_id=xxx&redirect_uri=xxx  -> 用户登录授权
2. 回调 redirect_uri?code=xxx                              -> 返回授权码
3. POST /oauth2/token (code + client_secret)               -> 换取 Token
4. 返回 Access Token + Refresh Token
```

核心端点类：`Oauth2DesktopEndpointResource`（授权）、`Oauth2ServiceEndpointResource`（Token）。

## 四、IAM 集成

Auth 模块深度集成腾讯蓝鲸 IAM（权限中心），核心依赖：

```kotlin
import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.helper.AuthHelper
import com.tencent.bk.sdk.iam.service.PolicyService
import com.tencent.bk.sdk.iam.service.v2.V2ManagerService
```

### IAM 回调接口

IAM 通过回调拉取 BK-CI 资源信息：

```kotlin
// 路径：/open/auth/resource/callback
@Path("/open/auth/resource/callback")
interface OpenAuthResourceCallBackResource {
    fun getResource(...)      // 获取资源详情
    fun listResource(...)     // 列出资源
    fun searchResource(...)   // 搜索资源
}
```

## 五、开发指南

### 5.1 新增权限操作

1. 在 `T_AUTH_ACTION` 表添加操作定义
2. 在 `AuthPermission` 枚举中添加对应值
3. 在 IAM 系统注册操作
4. 更新 `RbacAuthUtils.buildAction()` 映射
5. **验证**: 调用 `ServicePermissionAuthResource.validateUserResourcePermission()` 测试新操作，确认 IAM 策略引擎返回预期结果

### 5.2 新增资源类型

1. 在 `AuthResourceType` 枚举中添加类型
2. 在 `T_AUTH_RESOURCE_TYPE` 表添加记录
3. 实现 `AuthResourceCallBackResource` 回调接口
4. 在 IAM 系统注册资源类型
5. **验证**: 通过 IAM 控制台确认资源类型已注册，调用回调接口 `/open/auth/resource/callback` 验证资源能被正确拉取

### 5.3 新增资源类型完整示例

```kotlin
import com.tencent.devops.common.auth.api.AuthResourceType

// 1. 在 AuthResourceType 枚举中添加
enum class AuthResourceType(val value: String) {
    // ... 已有类型
    MY_NEW_RESOURCE("my_new_resource"),  // 新资源类型
}

// 2. 实现回调接口，让 IAM 能拉取资源
@RestController
class MyResourceCallbackImpl : AuthResourceCallBackResource {
    override fun getResource(resourceType: String, resourceCode: String): ResourceInfo {
        // 从数据库查询资源信息并返回
        val resource = myResourceDao.get(resourceCode)
        return ResourceInfo(
            id = resource.id,
            displayName = resource.name
        )
    }
}

// 3. 在 SQL 中添加资源类型记录
// INSERT INTO T_AUTH_RESOURCE_TYPE (RESOURCE_TYPE, NAME, PARENT, SYSTEM)
// VALUES ('my_new_resource', '我的资源', 'project', 'bk_ci');
```

## 六、常见问题

**Q: 如何判断用户是否是项目成员？**
调用 `RbacPermissionService.validateUserProjectPermission()` 或检查用户是否在任意项目用户组中。

**Q: 如何给用户授权？**
将用户添加到对应的用户组 `RbacPermissionResourceMemberService.addGroupMember()`。

**Q: OAuth2 支持哪些授权模式？**
支持授权码模式（Authorization Code）和密码模式（Password）。

**Q: 如何处理权限缓存？**
使用 `BkInternalPermissionCache` 进行缓存，通过 Redis 存储，支持主动刷新。
