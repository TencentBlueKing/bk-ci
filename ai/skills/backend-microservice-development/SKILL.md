---
name: backend-microservice-development
description: 后端微服务开发规范，涵盖目录结构、分层架构（API/Service/DAO）、依赖注入、配置管理、Spring Boot 最佳实践。当用户进行后端开发、创建新微服务、编写 Kotlin/Java 代码或设计服务架构时使用。
core_files:
  - "src/backend/ci/core/{service}/api-{service}/"
  - "src/backend/ci/core/{service}/biz-{service}/"
  - "src/backend/ci/core/{service}/boot-{service}/"
related_skills:
  - 02-api-interface-design
  - 03-unit-testing
  - microservice-infrastructure
token_estimate: 3500
---

# 后端微服务开发

## Quick Reference

```
核心服务：process(流水线) | project(项目) | repository(代码库) | auth(权限)
四层架构：api-{service}(接口) → biz-{service}(业务+DAO) → boot-{service}(启动) → model-{service}(数据模型)
包命名：com.tencent.devops.<module>
Resource 前缀：User*(Web) | Service*(内部) | Build*(Agent) | Open*(外部)
```

### 最简示例

```kotlin
// Resource 接口定义
@Tag(name = "USER_PIPELINE", description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
interface UserPipelineResource {
    @GET
    @Operation(summary = "获取流水线列表")
    fun list(@HeaderParam(AUTH_HEADER_USER_ID) userId: String): Result<List<PipelineInfo>>
}

// Resource 实现（构造器注入）
@RestResource
class UserPipelineResourceImpl @Autowired constructor(
    private val pipelineService: PipelineService
) : UserPipelineResource
```

## When to Use

- 创建新微服务或添加新 Resource
- 编写 Kotlin/Java 后端代码
- 设计服务间调用架构
- 需要了解项目分层规范

## When NOT to Use

- 前端 Vue 开发 → 使用 `04-frontend-vue-development`
- Agent Go 开发 → 使用 `05-go-agent-development`
- 数据库 DDL 编写 → 使用 `database-design`

---

## 四层分层架构

```
core/{service}/
├── api-{service}/      # API接口定义层 - 对外暴露服务契约
├── biz-{service}/      # 业务逻辑层 - 包含 Service、DAO
├── boot-{service}/     # Spring Boot启动层 - 独立部署单元
└── model-{service}/    # 数据模型层 - JOOQ 生成的数据库访问对象
```

## 16 个核心微服务

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
| openapi | 对外 API |
| metrics | 度量服务 |
| websocket | WebSocket 服务 |
| misc | 杂项服务 |

## Resource 命名规范

| 前缀 | 用途 | 路径前缀 | 示例 |
|------|------|----------|------|
| `User*Resource` | 用户 Web 交互 | `/user/` | `UserPipelineResource` |
| `Service*Resource` | 服务间内部调用 | `/service/` | `ServiceBuildResource` |
| `Build*Resource` | 构建过程 Agent/Worker | `/build/` | `BuildArtifactoryResource` |
| `BuildAgent*Resource` | 第三方 Agent 专用 | `/buildAgent/` | `BuildAgentCredentialResource` |
| `Open*Resource` | 对外开放（OpenAPI） | `/open/` | `OpenPipelineTaskResource` |
| `Op*Resource` | 运营管理接口 | `/op/` | `OpProjectResource` |

## 注解使用标准

### API 定义

```kotlin
@RestResource              // REST 资源实现类
@Path("/user/artifactories")
@GET / @POST / @PUT / @DELETE
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
```

### Swagger 文档

```kotlin
@Tag(name = "USER_ARTIFACTORY", description = "版本仓库-仓库资源")
@Operation(summary = "根据元数据获取文件")
@Parameter(description = "用户ID", required = true)
```

### 参数校验

```kotlin
@BkField(minLength = 1, maxLength = 128)
@BkField(patternStyle = BkStyleEnum.CODE_STYLE)
```

## 方法命名约定

| 操作 | 命名前缀 |
|------|---------|
| 查询 | `get*`, `query*`, `list*`, `search*` |
| 创建 | `create*`, `upload*`, `archive*` |
| 修改 | `update*`, `modify*` |
| 删除 | `delete*`, `clear*`, `remove*` |

## 强制规则

- ✅ 服务间通过 API 接口通信，禁止直接访问其他服务的数据库
- ✅ 依赖关系在 `build.gradle.kts` 中明确声明
- ❌ 禁止循环依赖
- ❌ 禁止手写 SQL，使用 JOOQ

---

## Checklist

开发后端功能前确认：
- [ ] 功能归属到合理的现有服务
- [ ] Resource 命名符合前缀规范
- [ ] 使用构造器注入依赖
- [ ] 返回值使用 `Result<T>` 包装
- [ ] 添加 Swagger 注解生成文档
