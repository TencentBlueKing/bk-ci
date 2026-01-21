---
name: openapi-module-architecture
description: OpenAPI 开放接口模块架构指南，涵盖 API 网关、接口鉴权、限流配置、SDK 生成、API 文档。当用户开发开放 API、配置接口鉴权、实现限流策略或生成 SDK 时使用。
---

# OpenAPI 开放接口模块架构指南

## 概述

OpenAPI（开放接口）模块是 BK-CI 对外暴露的 API 网关服务，负责将内部微服务的能力通过标准化的 API 对外开放，供第三方系统集成调用。

**模块职责**：
- 对外 API 网关（蓝鲸 API Gateway 对接）
- API 版本管理（v3、v4）
- AppCode 权限管理
- API 调用统计与度量
- Swagger 文档生成

## 一、模块结构

```
src/backend/ci/core/openapi/
├── api-openapi/           # API 接口定义
│   └── src/main/kotlin/com/tencent/devops/openapi/
│       ├── api/           # Resource 接口
│       │   ├── apigw/     # API Gateway 接口
│       │   │   ├── v3/    # V3 版本 API
│       │   │   │   ├── ApigwPipelineResourceV3.kt
│       │   │   │   ├── ApigwBuildResourceV3.kt
│       │   │   │   ├── ApigwProjectResourceV3.kt
│       │   │   │   └── ...
│       │   │   └── v4/    # V4 版本 API
│       │   │       ├── ApigwPipelineResourceV4.kt
│       │   │       ├── ApigwBuildResourceV4.kt
│       │   │       └── ...
│       │   └── op/        # 运营管理接口
│       │       ├── OpAppCodeGroupResource.kt
│       │       ├── OpAppCodeProjectResource.kt
│       │       └── OpSwaggerDocResource.kt
│       ├── constant/      # 常量定义
│       │   └── OpenAPIMessageCode.kt
│       ├── pojo/          # 数据传输对象
│       │   ├── AppCodeGroup.kt
│       │   ├── AppCodeProjectResponse.kt
│       │   └── MetricsApiData.kt
│       └── utils/         # 工具类
│           └── markdown/  # Markdown 文档生成
├── biz-openapi/           # 业务逻辑层
└── boot-openapi/          # 启动模块
```

## 二、API 版本管理

### 2.1 版本演进

| 版本 | 说明 | 状态 |
|------|------|------|
| v3 | 基础版本 API | 维护中 |
| v4 | 增强版本 API | 主推版本 |

### 2.2 API 分类

```
OpenAPI
├── Pipeline API          # 流水线管理
│   ├── 创建/编辑/删除流水线
│   ├── 流水线列表查询
│   └── 流水线版本管理
├── Build API             # 构建执行
│   ├── 启动/停止构建
│   ├── 构建状态查询
│   └── 构建历史查询
├── Project API           # 项目管理
│   ├── 项目列表
│   └── 项目信息查询
├── Repository API        # 代码库管理
│   ├── 代码库列表
│   └── 代码库操作
├── Credential API        # 凭证管理
│   ├── 凭证创建/查询
│   └── 凭证类型管理
├── Artifactory API       # 制品库
│   ├── 文件上传/下载
│   └── 文件任务管理
├── Quality API           # 质量红线
│   ├── 规则管理
│   └── 拦截记录查询
├── Log API               # 构建日志
│   └── 日志查询/下载
└── Auth API              # 权限认证
    ├── 权限校验
    └── 授权管理
```

## 三、数据库设计

### 3.1 核心表结构

| 表名 | 说明 |
|------|------|
| `T_APP_CODE_GROUP` | AppCode 组织架构关联 |
| `T_APP_CODE_PROJECT` | AppCode 项目关联 |
| `T_APP_USER_INFO` | AppCode 管理员信息 |
| `T_OPENAPI_METRICS_FOR_API` | API 维度调用统计 |
| `T_OPENAPI_METRICS_FOR_PROJECT` | 项目维度调用统计 |

### 3.2 关键字段说明

**T_APP_CODE_GROUP**：
```sql
- ID: 主键
- APP_CODE: 应用编码
- BG_ID/BG_NAME: 事业群信息
- DEPT_ID/DEPT_NAME: 部门信息
- CENTER_ID/CENTER_NAME: 中心信息
- CREATOR/UPDATER: 创建/更新人
```

**T_APP_CODE_PROJECT**：
```sql
- ID: 主键
- APP_CODE: 应用编码
- PROJECT_ID: 项目ID（english_name）
- CREATOR: 创建人
```

**T_OPENAPI_METRICS_FOR_API**：
```sql
- API: 接口代码
- KEY: AppCode 或用户ID
- SECOND_LEVEL_CONCURRENCY: 秒级并发量
- PEAK_CONCURRENCY: 峰值并发量
- CALL_5M: 5分钟调用量
- CALL_1H: 1小时调用量
- CALL_24H: 24小时调用量
- CALL_7D: 7天调用量
```

## 四、API 接口设计

### 4.1 流水线 API (V4)

```kotlin
@Path("/apigw/v4/projects/{projectId}/pipelines")
interface ApigwPipelineResourceV4 {
    
    @POST
    @Path("/")
    fun create(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        pipeline: PipelineModelAndSetting
    ): Result<PipelineId>
    
    @GET
    @Path("/")
    fun list(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<Page<Pipeline>>
    
    @GET
    @Path("/{pipelineId}")
    fun get(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String
    ): Result<Pipeline>
    
    @PUT
    @Path("/{pipelineId}")
    fun edit(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        pipeline: PipelineModelAndSetting
    ): Result<Boolean>
    
    @DELETE
    @Path("/{pipelineId}")
    fun delete(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String
    ): Result<Boolean>
}
```

### 4.2 构建 API (V4)

```kotlin
@Path("/apigw/v4/projects/{projectId}/pipelines/{pipelineId}/builds")
interface ApigwBuildResourceV4 {
    
    @POST
    @Path("/start")
    fun start(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        values: Map<String, String>?,
        @QueryParam("buildNo") buildNo: Int?,
        @QueryParam("channelCode") channelCode: ChannelCode?
    ): Result<BuildId>
    
    @POST
    @Path("/{buildId}/stop")
    fun stop(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String
    ): Result<Boolean>
    
    @GET
    @Path("/{buildId}/status")
    fun getStatus(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String
    ): Result<BuildStatus>
    
    @GET
    @Path("/history")
    fun getHistory(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) appCode: String,
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID) userId: String,
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<Page<BuildHistory>>
}
```

### 4.3 运营管理接口

```kotlin
@Path("/op/appCode")
interface OpAppCodeProjectResource {
    
    @POST
    @Path("/projects")
    fun addProject(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        request: AppCodeProjectRequest
    ): Result<Boolean>
    
    @DELETE
    @Path("/projects")
    fun deleteProject(
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @QueryParam("appCode") appCode: String,
        @QueryParam("projectId") projectId: String
    ): Result<Boolean>
    
    @GET
    @Path("/projects/list")
    fun listProjects(
        @QueryParam("appCode") appCode: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<Page<AppCodeProjectResponse>>
}
```

## 五、认证与授权

### 5.1 认证方式

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        OpenAPI 认证流程                                  │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     请求头认证                                    │   │
│  │  ┌─────────────────────────────────────────────────────────────┐ │   │
│  │  │ X-DEVOPS-APP-CODE: 应用编码（由蓝鲸 API Gateway 注入）       │ │   │
│  │  │ X-DEVOPS-APP-SECRET: 应用密钥（可选）                        │ │   │
│  │  │ X-DEVOPS-UID: 用户ID（操作人）                               │ │   │
│  │  └─────────────────────────────────────────────────────────────┘ │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     权限校验                                      │   │
│  │  1. 验证 AppCode 是否有效                                        │   │
│  │  2. 验证 AppCode 是否有权访问目标项目                             │   │
│  │  3. 验证用户是否有对应资源的操作权限                              │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 5.2 请求头说明

| Header | 说明 | 必填 |
|--------|------|------|
| `X-DEVOPS-APP-CODE` | 应用编码 | 是 |
| `X-DEVOPS-APP-SECRET` | 应用密钥 | 部分接口 |
| `X-DEVOPS-UID` | 操作用户ID | 是 |

### 5.3 AppCode 权限管理

```kotlin
// AppCode 与项目的关联
data class AppCodeProject(
    val appCode: String,      // 应用编码
    val projectId: String,    // 项目ID（english_name）
    val creator: String       // 创建人
)

// 验证 AppCode 是否有权访问项目
fun validateAppCodeProject(appCode: String, projectId: String): Boolean {
    return appCodeProjectDao.exists(appCode, projectId)
}
```

## 六、API 调用统计

### 6.1 统计维度

| 维度 | 说明 |
|------|------|
| API 维度 | 按接口统计调用量 |
| 项目维度 | 按项目统计调用量 |
| AppCode 维度 | 按应用统计调用量 |

### 6.2 统计指标

```kotlin
data class MetricsApiData(
    val api: String,                    // API 接口
    val key: String,                    // AppCode 或用户
    val secondLevelConcurrency: Int,    // 秒级并发
    val peakConcurrency: Int,           // 峰值并发
    val call5m: Int,                    // 5分钟调用量
    val call1h: Int,                    // 1小时调用量
    val call24h: Int,                   // 24小时调用量
    val call7d: Int                     // 7天调用量
)
```

## 七、与其他模块的关系

### 7.1 依赖关系

```
OpenAPI 模块
    │
    ├──> Process（流水线）
    │    - 流水线 CRUD 操作
    │    - 构建启动/停止
    │
    ├──> Project（项目）
    │    - 项目信息查询
    │
    ├──> Repository（代码库）
    │    - 代码库管理
    │
    ├──> Artifactory（制品库）
    │    - 文件上传/下载
    │
    ├──> Ticket（凭证）
    │    - 凭证管理
    │
    ├──> Quality（质量红线）
    │    - 规则管理
    │
    ├──> Log（日志）
    │    - 日志查询
    │
    └──> Auth（权限）
         - 权限校验
```

### 7.2 网关对接

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ 第三方系统   │     │ 蓝鲸 API    │     │ BK-CI       │
│             │────>│ Gateway     │────>│ OpenAPI     │
└─────────────┘     └─────────────┘     └─────────────┘
                          │
                          │ 注入 AppCode
                          │ 鉴权校验
                          │ 流量控制
                          ▼
```

## 八、开发规范

### 8.1 新增 API 接口

1. 在 `api-openapi` 模块定义接口
2. 接口路径遵循 `/apigw/{version}/projects/{projectId}/...` 格式
3. 必须包含认证头参数
4. 返回值使用 `Result<T>` 包装

```kotlin
@Path("/apigw/v4/projects/{projectId}/myResource")
interface ApigwMyResourceV4 {
    
    @GET
    @Path("/")
    @Operation(summary = "获取资源列表")
    fun list(
        @HeaderParam(AUTH_HEADER_DEVOPS_APP_CODE) 
        @Parameter(description = "AppCode", required = true)
        appCode: String,
        
        @HeaderParam(AUTH_HEADER_DEVOPS_USER_ID)
        @Parameter(description = "用户ID", required = true)
        userId: String,
        
        @PathParam("projectId")
        @Parameter(description = "项目ID", required = true)
        projectId: String
    ): Result<List<MyResource>>
}
```

### 8.2 调用示例

```bash
# 启动构建
curl -X POST \
  'https://devops.example.com/prod/v4/apigw-app/projects/my-project/pipelines/p-xxx/builds/start' \
  -H 'X-DEVOPS-APP-CODE: my-app' \
  -H 'X-DEVOPS-UID: admin' \
  -H 'Content-Type: application/json' \
  -d '{"key1": "value1"}'

# 查询构建状态
curl -X GET \
  'https://devops.example.com/prod/v4/apigw-app/projects/my-project/pipelines/p-xxx/builds/b-xxx/status' \
  -H 'X-DEVOPS-APP-CODE: my-app' \
  -H 'X-DEVOPS-UID: admin'
```

### 8.3 错误码规范

```kotlin
object OpenAPIMessageCode {
    const val ERROR_OPENAPI_INNER_SERVICE_FAIL = "2128001"      // 内部服务调用失败
    const val ERROR_OPENAPI_JWT_PARSE_FAIL = "2128002"          // JWT 解析失败
    const val ERROR_OPENAPI_APP_NOT_EXIST = "2128003"           // AppCode 不存在
    const val ERROR_OPENAPI_NO_PROJECT_PERMISSION = "2128004"   // 无项目权限
}
```

## 九、常见问题

**Q: 如何申请 AppCode？**
A: 通过蓝鲸开发者中心申请应用，获取 AppCode 和 AppSecret。

**Q: 如何授权 AppCode 访问项目？**
A: 通过 `OpAppCodeProjectResource` 接口添加 AppCode 与项目的关联，或由项目管理员在前端授权。

**Q: V3 和 V4 API 有什么区别？**
A: V4 是增强版本，提供更丰富的功能和更好的兼容性。新项目建议使用 V4。

**Q: API 调用频率有限制吗？**
A: 有，通过蓝鲸 API Gateway 进行流量控制，具体限制取决于配置。

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
