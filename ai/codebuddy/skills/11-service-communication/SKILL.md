---
name: 11-service-communication
description: 服务间调用规范
---

# 服务间调用

服务间调用规范.

## 触发条件

当用户需要实现微服务间的 API 调用、Feign 客户端使用时，使用此 Skill。

## 核心原则：跨模块必须使用服务间调用

### ⚠️ 强制规则

**当不同微服务模块之间需要交互时，必须通过 `Client` 组件调用 `Service*Resource` 接口，严禁直接依赖其他模块的 Service 层或 DAO 层。**

### 微服务模块列表

BK-CI 包含以下核心微服务模块：

| 模块 | 职责 | 典型 API |
|------|------|----------|
| **project** | 项目管理 | `ServiceProjectResource` |
| **process** | 流水线编排调度 | `ServiceBuildResource`, `ServicePipelineResource` |
| **repository** | 代码库管理 | `ServiceRepositoryResource` |
| **artifactory** | 制品库 | `ServiceArtifactoryResource` |
| **store** | 研发商店（插件/模板） | `ServiceStoreResource`, `ServiceAtomResource` |
| **environment** | 构建机/环境管理 | `ServiceEnvironmentResource` |
| **dispatch** | 构建调度分发 | `ServiceDispatchResource` |
| **auth** | 权限认证（RBAC） | `ServicePermissionAuthResource` |
| **ticket** | 凭证管理 | `ServiceCredentialResource`, `ServiceCertResource` |
| **log** | 构建日志 | `ServiceLogResource` |
| **quality** | 质量红线 | `ServiceQualityResource` |
| **notify** | 通知服务 | `ServiceNotifyResource` |
| **repository** | 代码库 | `ServiceRepositoryResource` |
| **openapi** | 对外接口 | - |
| **metrics** | 度量指标 | `ServiceMetricsResource` |
| **websocket** | WebSocket | `ServiceWebsocketResource` |

### 跨模块调用示例

#### ✅ 正确：使用 Client 调用

**场景 1：ticket 模块调用 process 模块获取构建信息**

```kotlin
// ticket/biz-ticket/CredentialServiceImpl.kt
@Service
class CredentialServiceImpl(
    private val client: Client
) {
    fun buildGet(projectId: String, buildId: String, credentialId: String): CredentialInfo? {
        // ✅ 正确：ticket 调用 process，通过 Client
        val buildBasicInfoResult = client.get(ServiceBuildResource::class)
            .serviceBasic(projectId, buildId)
        
        if (buildBasicInfoResult.isNotOk()) {
            throw RemoteServiceException("Failed to build the basic information")
        }
        
        val buildBasicInfo = buildBasicInfoResult.data
            ?: throw RemoteServiceException("Build info not found")
        
        // ... 业务逻辑
    }
}
```

**场景 2：ticket 模块调用 auth 模块进行权限校验**

```kotlin
// ticket/biz-ticket/RbacCertPermissionService.kt
@Service
class RbacCertPermissionService(
    private val client: Client,
    private val tokenService: TokenService
) {
    override fun validatePermission(
        userId: String,
        projectId: String,
        resourceCode: String,
        authPermission: AuthPermission
    ) {
        // ✅ 正确：ticket 调用 auth，通过 Client
        val checkResult = client.get(ServicePermissionAuthResource::class)
            .validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceType = AuthResourceType.TICKET_CERT.value,
                resourceCode = resourceCode,
                action = buildCertAction(authPermission)
            ).data ?: false
        
        if (!checkResult) {
            throw PermissionForbiddenException("No permission")
        }
    }
}
```

**场景 3：store 模块调用 project 模块获取项目信息**

```kotlin
// store/biz-store/MarketTemplateServiceImpl.kt
@Service
class MarketTemplateServiceImpl(
    private val client: Client
) {
    fun getTemplateDetail(projectCode: String): TemplateDetail {
        // ✅ 正确：store 调用 project，通过 Client
        val projectName = projectCode?.let { 
            client.get(ServiceProjectResource::class).get(it).data?.projectName 
        }
        
        // ... 业务逻辑
    }
    
    fun batchGetProjectNames(projectCodeList: Set<String>): Map<String, String> {
        // ✅ 正确：批量调用
        return client.get(ServiceProjectResource::class)
            .getNameByCode(projectCodeList.joinToString(","))
            .data ?: emptyMap()
    }
}
```

**场景 4：process 模块调用 project 模块**

```kotlin
// process/biz-process/TempNotifyTemplateUtils.kt
@Component
class TempNotifyTemplateUtils(
    private val client: Client
) {
    fun getProjectName(projectId: String): String {
        // ✅ 正确：process 调用 project，通过 Client
        return client.get(ServiceProjectResource::class)
            .get(projectId)
            .data!!
            .projectName
    }
}
```

#### ❌ 错误：直接依赖其他模块的内部实现

```kotlin
// ❌ 错误示例（假设代码）
@Service
class CredentialServiceImpl(
    // ❌ 错误：ticket 直接依赖 process 的 Service 层
    private val buildService: BuildService,  // 来自 process 模块
    
    // ❌ 错误：ticket 直接依赖 project 的 DAO 层
    private val projectDao: ProjectDao  // 来自 project 模块
) {
    fun getCredential(): CredentialInfo {
        // ❌ 错误：直接调用其他模块的 Service
        val build = buildService.getBuild(buildId)
        
        // ❌ 错误：直接调用其他模块的 DAO
        val project = projectDao.get(projectId)
    }
}
```

**问题**：
1. 破坏微服务边界，导致模块耦合
2. 无法独立部署（依赖其他模块的 JAR 包）
3. 无法进行负载均衡和故障隔离
4. 无法通过 Consul 进行服务发现

### 判断是否需要跨模块调用

| 场景 | 是否跨模块 | 调用方式 |
|------|-----------|----------|
| `ticket` 模块调用 `process` 模块 | ✅ 是 | 必须使用 `Client` |
| `store` 模块调用 `project` 模块 | ✅ 是 | 必须使用 `Client` |
| `process` 模块调用 `auth` 模块 | ✅ 是 | 必须使用 `Client` |
| `ticket` 模块内部 Service 调用 DAO | ❌ 否 | 直接依赖注入 |
| `process` 模块内部 Service 间调用 | ❌ 否 | 直接依赖注入 |

**简单判断规则**：
- 如果调用的类在 `ci/core/{另一个模块}/` 下 → **必须使用 Client**
- 如果调用的类在 `ci/core/{当前模块}/` 下 → 直接依赖注入

## Client 组件

```kotlin
@Component
class Client @Autowired constructor(
    private val compositeDiscoveryClient: CompositeDiscoveryClient?,
    private val clientErrorDecoder: ClientErrorDecoder,
    private val commonConfig: CommonConfig,
    private val bkTag: BkTag,
    objectMapper: ObjectMapper
) {
    // 获取服务客户端
    fun <T : Any> get(clz: KClass<T>): T
    
    // 带后缀获取
    fun <T : Any> get(clz: KClass<T>, suffix: String): T
}
```

## 使用方式

### 1. 基本调用

```kotlin
@Service
class BuildService(
    private val client: Client
) {
    fun getBuildVariables(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Map<String, String> {
        return client.get(ServiceBuildResource::class)
            .getBuildVariableValue(
                userId = "system",
                projectId = projectId,
                pipelineId = pipelineId,
                buildId = buildId,
                variableNames = listOf("VAR1", "VAR2")
            ).data ?: emptyMap()
    }
}
```

### 2. 异常处理

```kotlin
fun getProjectInfo(projectId: String): ProjectVO? {
    return try {
        client.get(ServiceProjectResource::class)
            .get(projectId)
            .data
    } catch (e: RemoteServiceException) {
        logger.warn("获取项目信息失败: ${e.errorMessage}")
        null
    } catch (e: ClientException) {
        logger.error("服务调用异常", e)
        throw e
    }
}
```

### 3. 带重试调用

```kotlin
fun callWithRetry(projectId: String): Result<ProjectVO> {
    return RetryUtils.clientRetry(retryTime = 3, retryPeriodMills = 500) {
        client.get(ServiceProjectResource::class).get(projectId)
    }
}
```

## 服务接口定义

### Service*Resource 接口规范

所有服务间调用的接口必须遵循以下规范：

#### 1. 命名规范

```kotlin
// ✅ 正确：以 Service 开头
interface ServiceProjectResource
interface ServiceBuildResource
interface ServicePermissionAuthResource
interface ServiceCredentialResource

// ❌ 错误：不使用 Service 前缀
interface ProjectResource        // 这是用户态接口
interface BuildResource          // 这是构建机接口
```

#### 2. 接口定义位置

**必须在 `api-{模块}` 模块中定义**，不能在 `biz-{模块}` 中定义。

```
project/
├── api-project/          # ✅ 接口定义在这里
│   └── ServiceProjectResource.kt
├── biz-project/          # ✅ 接口实现在这里
│   └── ServiceProjectResourceImpl.kt
└── model-project/
```

#### 3. 接口完整示例

```kotlin
// api-project 模块中定义
@Path("/service/projects")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Api(tags = ["SERVICE_PROJECT"], description = "服务-项目管理")
interface ServiceProjectResource {
    
    @GET
    @Path("/{projectId}")
    @Operation(summary = "获取项目信息")
    fun get(
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId") 
        projectId: String
    ): Result<ProjectVO>
    
    @GET
    @Path("/")
    @Operation(summary = "获取项目列表")
    fun list(
        @Parameter(description = "用户ID", required = true)
        @QueryParam("userId") 
        userId: String
    ): Result<List<ProjectVO>>
    
    @POST
    @Path("/batch")
    @Operation(summary = "批量获取项目名称")
    fun getNameByCode(
        @Parameter(description = "项目代码列表，逗号分隔", required = true)
        @QueryParam("projectCodes")
        projectCodes: String
    ): Result<Map<String, String>>
}
```

#### 4. 接口实现

```kotlin
// biz-project 模块中实现
@RestResource
class ServiceProjectResourceImpl @Autowired constructor(
    private val projectService: ProjectService
) : ServiceProjectResource {
    
    override fun get(projectId: String): Result<ProjectVO> {
        return Result(projectService.getProject(projectId))
    }
    
    override fun list(userId: String): Result<List<ProjectVO>> {
        return Result(projectService.listProjects(userId))
    }
    
    override fun getNameByCode(projectCodes: String): Result<Map<String, String>> {
        val codes = projectCodes.split(",")
        return Result(projectService.getProjectNames(codes))
    }
}
```

### 接口类型前缀约定

| 前缀 | 说明 | 调用方 |
|------|------|--------|
| `Service*Resource` | 服务间调用 | 后端微服务 |
| `User*Resource` | 用户态接口 | 前端页面 |
| `Build*Resource` | 构建接口 | Agent/Worker |
| `Open*Resource` | 开放接口 | 外部系统 |

## 最佳实践

### 1. 统一异常处理

```kotlin
fun getProjectInfo(projectId: String): ProjectVO? {
    return try {
        client.get(ServiceProjectResource::class)
            .get(projectId)
            .data
    } catch (e: RemoteServiceException) {
        // 远程服务异常（业务异常）
        logger.warn("获取项目信息失败: projectId=$projectId, error=${e.errorMessage}")
        null
    } catch (e: ClientException) {
        // 客户端异常（网络、超时等）
        logger.error("服务调用异常: projectId=$projectId", e)
        throw e
    }
}
```

### 2. 添加重试机制

```kotlin
fun callWithRetry(projectId: String): Result<ProjectVO> {
    return RetryUtils.clientRetry(
        retryTime = 3,              // 重试 3 次
        retryPeriodMills = 500      // 间隔 500ms
    ) {
        client.get(ServiceProjectResource::class).get(projectId)
    }
}
```

### 3. 批量调用优化

```kotlin
// ❌ 错误：N+1 查询问题
fun getProjectNames(projectIds: List<String>): Map<String, String> {
    return projectIds.associateWith { projectId ->
        client.get(ServiceProjectResource::class).get(projectId).data?.projectName ?: ""
    }
}

// ✅ 正确：批量调用
fun getProjectNames(projectIds: List<String>): Map<String, String> {
    return client.get(ServiceProjectResource::class)
        .getNameByCode(projectIds.joinToString(","))
        .data ?: emptyMap()
}
```

### 4. 日志记录

```kotlin
fun validatePermission(userId: String, projectId: String, action: String): Boolean {
    logger.info("Validating permission: userId=$userId, projectId=$projectId, action=$action")
    
    return try {
        val result = client.get(ServicePermissionAuthResource::class)
            .validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                action = action
            )
        
        logger.info("Permission check result: $result")
        result.data ?: false
        
    } catch (e: Exception) {
        logger.error("Permission check failed: userId=$userId, projectId=$projectId", e)
        false
    }
}
```

### 5. 结果判断

```kotlin
fun callService(): Data? {
    val result = client.get(ServiceProjectResource::class).get(projectId)
    
    // ✅ 推荐方式 1：isOk() 判断
    if (result.isOk()) {
        return result.data
    } else {
        logger.error("Service call failed: ${result.message}")
        return null
    }
    
    // ✅ 推荐方式 2：isNotOk() 判断
    if (result.isNotOk()) {
        throw RemoteServiceException("Service call failed: ${result.message}")
    }
    return result.data
}
```

## 常见跨模块调用场景

### 场景汇总表

| 调用方模块 | 被调用模块 | 典型场景 | 使用的 Resource |
|-----------|-----------|----------|----------------|
| **ticket** | **process** | 凭证获取构建信息 | `ServiceBuildResource` |
| **ticket** | **auth** | 凭证权限校验 | `ServicePermissionAuthResource` |
| **store** | **project** | 插件/模板获取项目信息 | `ServiceProjectResource` |
| **store** | **process** | 模板发布到流水线 | `ServicePipelineTemplateV2Resource` |
| **process** | **project** | 流水线获取项目信息 | `ServiceProjectResource` |
| **process** | **auth** | 流水线权限校验 | `ServicePermissionAuthResource` |
| **process** | **repository** | 流水线触发器检查代码库 | `ServiceRepositoryResource` |
| **process** | **artifactory** | 流水线归档制品 | `ServiceArtifactoryResource` |
| **quality** | **process** | 质量红线查询流水线 | `ServicePipelineResource` |
| **quality** | **project** | 质量红线获取项目信息 | `ServiceProjectResource` |
| **environment** | **project** | 构建机管理获取项目 | `ServiceProjectResource` |
| **notify** | **project** | 通知服务获取项目信息 | `ServiceProjectResource` |

### 权限校验场景（auth 模块）

几乎所有模块都需要调用 `auth` 模块进行权限校验：

```kotlin
// 在任何模块中进行权限校验
@Service
class SomePermissionService(
    private val client: Client,
    private val tokenService: TokenService
) {
    fun checkPermission(userId: String, projectId: String, resourceCode: String): Boolean {
        return client.get(ServicePermissionAuthResource::class)
            .validateUserResourcePermissionByRelation(
                token = tokenService.getSystemToken()!!,
                userId = userId,
                projectCode = projectId,
                resourceType = "YOUR_RESOURCE_TYPE",
                resourceCode = resourceCode,
                action = "YOUR_ACTION"
            ).data ?: false
    }
}
```

**涉及模块**：ticket, store, process, environment, quality, repository, artifactory

### 项目信息获取场景（project 模块）

几乎所有模块都需要调用 `project` 模块获取项目基本信息：

```kotlin
// 获取单个项目
val projectInfo = client.get(ServiceProjectResource::class).get(projectId).data

// 批量获取项目名称
val projectNames = client.get(ServiceProjectResource::class)
    .getNameByCode(projectIds.joinToString(","))
    .data ?: emptyMap()

// 获取用户的项目列表
val userProjects = client.get(ServiceProjectResource::class).list(userId).data
```

**涉及模块**：store, process, quality, environment, notify, ticket

### 流水线构建信息场景（process 模块）

其他模块需要获取流水线构建信息：

```kotlin
// ticket 模块获取构建信息
val buildInfo = client.get(ServiceBuildResource::class)
    .serviceBasic(projectId, buildId)
    .data

// artifactory 模块归档构建产物
client.get(ServiceBuildResource::class)
    .uploadBuildArtifactory(projectId, pipelineId, buildId, artifactInfo)
```

**涉及模块**：ticket, artifactory, quality, log

## 注意事项

### 1. 避免循环依赖

❌ **错误示例**：
- 模块 A 调用模块 B 的接口
- 模块 B 又调用模块 A 的接口
→ 形成循环依赖

✅ **解决方案**：
- 重新设计接口，将共同依赖的逻辑提取到第三方模块
- 通过事件机制（观察者模式）解耦

### 2. 接口粒度控制

```kotlin
// ❌ 错误：接口太细粒度，导致频繁调用
interface ServiceProjectResource {
    fun getProjectName(projectId: String): String
    fun getProjectDesc(projectId: String): String
    fun getProjectCreator(projectId: String): String
}

// ✅ 正确：合理粒度，一次返回完整信息
interface ServiceProjectResource {
    fun get(projectId: String): Result<ProjectVO>  // 包含所有基本信息
}
```

### 3. 返回值统一包装

所有 `Service*Resource` 接口必须返回 `Result<T>` 类型：

```kotlin
// ✅ 正确
fun get(projectId: String): Result<ProjectVO>

// ❌ 错误：直接返回数据对象
fun get(projectId: String): ProjectVO
```

### 4. 超时设置

在 `common.yml` 中配置合理的超时时间：

```yaml
spring:
  cloud:
    consul:
      discovery:
        health-check-timeout: 10s
feign:
  client:
    config:
      default:
        connectTimeout: 5000      # 连接超时 5 秒
        readTimeout: 30000        # 读取超时 30 秒
```

### 5. 服务降级

关键路径建议实现降级逻辑：

```kotlin
fun getProjectName(projectId: String): String {
    return try {
        client.get(ServiceProjectResource::class)
            .get(projectId)
            .data
            ?.projectName
            ?: projectId  // 降级：返回 projectId
    } catch (e: Exception) {
        logger.error("Failed to get project name, use projectId as fallback", e)
        projectId  // 降级：返回 projectId
    }
}
```

## 检查清单

在实现跨模块调用前，确认：

- [ ] 确认是否真的需要跨模块调用（是否可以在当前模块完成）
- [ ] 检查目标模块是否已提供 `Service*Resource` 接口
- [ ] 如果接口不存在，先在目标模块的 `api-*` 模块中定义接口
- [ ] 使用 `Client` 组件调用，不直接依赖其他模块的 Service 层
- [ ] 添加异常处理（`RemoteServiceException` 和 `ClientException`）
- [ ] 考虑是否需要重试机制
- [ ] 考虑是否需要批量调用优化
- [ ] 添加完整的日志记录
- [ ] 考虑降级方案（对于非关键路径）
- [ ] 返回值使用 `Result<T>` 包装
- [ ] 避免形成循环依赖

## 相关 Skills

- [01-后端微服务开发](../01-backend-microservice-development/SKILL.md) - 微服务架构设计
- [02-API 接口设计](../02-api-interface-design/SKILL.md) - API 接口设计规范
- [08-事件驱动架构](../08-event-driven-architecture/SKILL.md) - 事件机制替代同步调用
- [13-重试机制](../13-retry-mechanism/SKILL.md) - 重试策略实践

## 相关文件

- `common-client/src/main/kotlin/com/tencent/devops/common/client/Client.kt`
- `common-client/src/main/kotlin/com/tencent/devops/common/client/ClientErrorDecoder.kt`
