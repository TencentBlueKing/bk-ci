---
name: 01-backend-microservice-development
description: 后端微服务开发规范，涵盖目录结构、分层架构（API/Service/DAO）、依赖注入、配置管理、Spring Boot 最佳实践。当用户进行后端开发、创建新微服务、编写 Kotlin/Java 代码或设计服务架构时使用。
---

# Skill 01: 后端微服务开发

## 概述
BK-CI 后端采用 Kotlin/Java + Spring Boot 3 + Gradle Kotlin DSL 的微服务架构，包含 16 个核心微服务。

## 四层分层架构

每个微服务遵循标准的四层模块化结构：

```
core/{service}/
├── api-{service}/      # API接口定义层 - 对外暴露服务契约
├── biz-{service}/      # 业务逻辑层 - 包含 DAO、Service
├── boot-{service}/     # Spring Boot启动层 - 独立部署单元
└── model-{service}/    # 数据模型层 - JOOQ 生成的数据库访问对象
```

## 核心微服务清单

| 服务 | 职责 |
|------|------|
| project | 项目管理（所有模块基础依赖） |
| process | 流水线编排与调度（核心服务） |
| repository | 代码库管理 |
| artifactory | 制品库（对接 COS/S3） |
| store | 研发商店（插件、模板） |
| environment | 构建机/环境管理 |
| dispatch | 构建调度分发 |
| auth | 权限认证（RBAC） |
| ticket | 凭证管理 |
| log | 构建日志 |
| quality | 质量红线 |
| notify | 通知服务 |
| openapi | 对外API |
| metrics | 度量服务 |
| websocket | WebSocket服务 |
| misc | 杂项服务 |

## 类命名规范

### Resource 层（API 接口）

**接口命名模式**：`[访问级别/场景前缀][资源类型]Resource`

**按包目录组织**（推荐结构）：
```
api-{module}/
├── user/         # User* 接口（用户 Web 交互）
├── service/      # Service* 接口（服务间调用）
├── builds/       # Build* 接口（构建过程中使用）
├── app/          # App* 接口（移动端 App）
├── open/         # Open* 接口（对外开放接口）
├── external/     # External* 接口（外部系统对接）
├── op/           # Op* 接口（运营平台管理）
└── auth/         # 权限相关接口
```

**命名前缀及用途**：

| 前缀 | 用途 | 路径前缀 | 示例 | Tag 命名 |
|------|------|----------|------|----------|
| `User*Resource` | 用户级别资源，前端 Web 交互 | `/user/` | `UserPipelineResource` | `USER_*` |
| `Service*Resource` | 服务间内部调用 | `/service/` | `ServiceBuildResource`, `ServicePipelineResource` | `SERVICE_*` |
| `Build*Resource` | 构建过程中 Agent/Worker 使用 | `/build/` | `BuildBuildResource`, `BuildArtifactoryResource` | `BUILD_*` |
| `BuildAgent*Resource` | 第三方 Agent 专用接口 | `/buildAgent/` | `BuildAgentBuildResource`, `BuildAgentCredentialResource` | `BUILD_AGENT_*` |
| `AgentLess*Resource` | 无编译环境（Docker）接口 | `/docker-agentless/` | `AgentLessDockerHostResource` | `DOCKER_HOST` |
| `App*Resource` | 移动端 App 接口 | `/app/` | `AppPipelineBuildResource`, `AppRepositoryResource` | `APP_*` |
| `Open*Resource` | 对外开放接口（OpenAPI） | `/open/` | `OpenPipelineTaskResource`, `OpenProjectResource` | `OPEN_*` 或 `OPEN_SERVICE_*` |
| `External*Resource` | 外部系统对接（如 Codecc） | `/external/` | `ExternalPipelineResource`, `ExternalCodeccRepoResource` | `EXTERNAL_*` |
| `Op*Resource` | 运营管理接口（含平台管理、脚本调用） | `/op/` | `OpJobQuotaSystemResource`, `OpProjectResource` | `OP_*` |
| `Remote*Resource` | 远程调用/权限中心回调 | `/service/*/auth` | `RemoteNodeResource` | `SERVICE_AUTH_*` |

**注意事项**：
- **前缀大小写**：`Op` 和 `OP` 在项目中都有使用（如 `OpCredentialResource` 和 `OPProjectResource`），推荐使用 `Op` 作为类名前缀
- **目录组织**：大型模块（如 `process`）按访问级别分子包，小型模块可直接放在 `api-*` 根包下
- **Build 系列的区分**：
  - `Build*Resource`（路径 `/build/`）：构建过程中 Agent/Worker 通用接口
  - `BuildAgent*Resource`（路径 `/buildAgent/`）：第三方构建机 Agent 专用接口
  - `AgentLess*Resource`（路径 `/docker-agentless/`）：无编译环境（容器化构建）
- **Op 系列的多场景应用**：
  - 运营管理平台界面调用（如配额管理、项目列表）
  - 服务器运维脚本 curl 调用（如清零配额、数据统计、批量操作）
  - 系统初始化和数据迁移脚本
  - 示例：`curl -X POST "http://localhost/api/op/jobs/system/quota/clear/vm/DOCKER" -H "X-DEVOPS-UID: admin"`
- **Auth 回调**：`Remote*Resource` 通常用于权限中心等外部系统的回调接口
- **实际使用建议**：查看同模块现有 Resource 命名模式，保持一致性

**实现类命名**：`[资源类型]ResourceImpl`

### Service 层

**命名模式**：`[功能域]Service` / `[功能域]ServiceImpl`

示例：
- `ArchiveFileService`
- `PipelineBuildArtifactoryService`
- `BuildVariableService`

### 数据模型

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 传输对象 | `[功能]Info` / `[功能]Response` / `[功能]Request` | `FileInfo`, `CreateFileTaskReq` |
| 枚举 | `[功能]Type` / `[功能]Enum` | `ArtifactoryType` |

## 注解使用标准

### API 定义注解

```kotlin
@RestResource              // REST 资源实现类
@Service / @Component      // Spring 组件
@Path("/user/artifactories")
@GET / @POST / @PUT / @DELETE
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
```

### Swagger 文档注解

```kotlin
@Tag(name = "USER_ARTIFACTORY", description = "版本仓库-仓库资源")
@Operation(summary = "根据元数据获取文件")
@Parameter(description = "用户ID", required = true)
```

### 参数校验注解

```kotlin
@BkField(minLength = 1, maxLength = 128)
@BkField(patternStyle = BkStyleEnum.CODE_STYLE)
@BkField(required = true)
```

### 数据模型注解

```kotlin
@Schema(title = "版本仓库-文件信息")
@get:Schema(title = "文件名", required = true)  // Kotlin getter 注解
```

### JAX-RS 参数注解

```kotlin
@HeaderParam(AUTH_HEADER_USER_ID)
@PathParam("projectId")
@QueryParam("page")
```

## 依赖注入

**推荐使用构造器注入**：

```kotlin
class BuildArtifactoryResourceImpl @Autowired constructor(
    private val archiveFileService: ArchiveFileService,
    private val client: Client
) : BuildArtifactoryResource
```

## 统一返回包装

所有 API 返回值使用 `Result<T>` 包装：

```kotlin
fun search(...): Result<Page<FileInfo>>
fun acrossProjectCopy(...): Result<Count>
```

## 方法命名约定

| 操作 | 命名前缀 |
|------|---------|
| 查询数据 | `get*`, `query*`, `list*`, `search*`, `show*` |
| 创建/上传 | `create*`, `upload*`, `archive*` |
| 修改 | `update*`, `modify*` |
| 删除/清理 | `delete*`, `clear*`, `remove*` |
| 下载 | `download*`, `downloadUrl*` |
| 跨项目操作 | `acrossProject*` |

## 强制要求

1. ✅ 新功能必须归属到合理的现有服务
2. ✅ 服务间通过 API 接口通信，禁止直接访问其他服务的数据库
3. ✅ 每个服务的依赖关系在 `build.gradle.kts` 中明确声明
4. ❌ 禁止循环依赖
5. ❌ 禁止手写 SQL，使用 JOOQ

## 包命名规范

```
com.tencent.devops.<module>
```

示例：
- `com.tencent.devops.process.api.user`
- `com.tencent.devops.auth.service`
- `com.tencent.devops.artifactory.dao`

## Detekt 抑制（必要时）

```kotlin
@Suppress("TooManyFunctions", "LongParameterList")
interface UserArtifactoryResource { ... }
```
