---
name: 02-api-interface-design
description: API 接口设计规范.
---

# Skill 02: API 接口设计

## 概述
BK-CI 采用 RESTful 风格的 API 设计，使用 JAX-RS 注解定义接口，Swagger 注解生成文档。

## RESTful 设计原则

### 路径按调用范围划分

| 路径前缀 | 用途 | 调用方 |
|---------|------|--------|
| `/user/` | 用户态接口 | 前端 Web |
| `/service/` | 服务间调用 | 其他微服务 |
| `/build/` | 构建相关 | Agent/Worker |
| `/open/` | 对外开放 | 第三方系统 |

### 路径命名规范

```
/{scope}/{resource}/{resourceId}/{subResource}
```

示例：
- `GET /user/pipelines` - 获取流水线列表
- `GET /user/pipelines/{pipelineId}` - 获取单个流水线
- `POST /user/pipelines` - 创建流水线
- `PUT /user/pipelines/{pipelineId}` - 更新流水线
- `DELETE /user/pipelines/{pipelineId}` - 删除流水线

## 接口定义示例

### 完整的 Resource 接口定义

```kotlin
@Tag(name = "USER_PIPELINE", description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineResource {

    @Operation(summary = "获取流水线列表")
    @GET
    @Path("/")
    fun list(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "页码", required = false)
        @QueryParam("page")
        page: Int?,
        @Parameter(description = "每页数量", required = false)
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<Page<PipelineInfo>>

    @Operation(summary = "创建流水线")
    @POST
    @Path("/")
    fun create(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线信息", required = true)
        pipeline: PipelineCreateRequest
    ): Result<PipelineId>
}
```

## 错误码规范

### 错误码格式

```
21(平台)01(服务)001(业务码)
```

| 位置 | 含义 | 示例 |
|------|------|------|
| 前2位 | 平台标识 | 21 = BK-CI |
| 中间2位 | 服务标识 | 01 = 通用, 02 = process |
| 后3位 | 业务错误码 | 001 = 系统繁忙 |

### 常见错误码

| 错误码 | 含义 |
|--------|------|
| 2100001 | 系统内部繁忙，请稍后再试 |
| 2100013 | 无效参数 |
| 2119042 | 特定业务错误 |

### 抛出错误码异常

```kotlin
throw ErrorCodeException(
    statusCode = 400,
    errorCode = "2100013",
    defaultMessage = "无效参数",
    params = arrayOf(paramName)
)
```

## HTTP 状态码使用

| 状态码 | 场景 |
|--------|------|
| 200 | 请求成功 |
| 201 | 创建成功 |
| 400 | 请求参数错误 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

## 统一返回格式

### Result<T> 包装类

```kotlin
data class Result<T>(
    val status: Int,           // 状态码
    val message: String?,      // 提示信息
    val data: T?               // 返回数据
) {
    companion object {
        fun <T> success(data: T): Result<T>
        fun <T> failed(message: String): Result<T>
    }
}
```

### 分页返回格式

```kotlin
data class Page<T>(
    val count: Long,           // 总数
    val page: Int,             // 当前页
    val pageSize: Int,         // 每页数量
    val totalPages: Int,       // 总页数
    val records: List<T>       // 数据列表
)
```

## 请求/响应对象设计

### 请求对象命名

- 创建：`[Resource]CreateRequest`
- 更新：`[Resource]UpdateRequest`
- 查询：`[Resource]QueryRequest`

### 响应对象命名

- 详情：`[Resource]Info` / `[Resource]Detail`
- 列表项：`[Resource]Summary`
- 操作结果：`[Resource]Response`

### 示例

```kotlin
@Schema(title = "流水线创建请求")
data class PipelineCreateRequest(
    @get:Schema(title = "流水线名称", required = true)
    @BkField(minLength = 1, maxLength = 64)
    val name: String,
    
    @get:Schema(title = "流水线描述")
    val desc: String? = null,
    
    @get:Schema(title = "标签列表")
    val labels: List<String>? = null
)

@Schema(title = "流水线信息")
data class PipelineInfo(
    @get:Schema(title = "流水线ID")
    val pipelineId: String,
    
    @get:Schema(title = "流水线名称")
    val name: String,
    
    @get:Schema(title = "创建时间")
    val createTime: Long
)
```

## 接口版本管理

### 路径版本

```kotlin
@Path("/v3/user/pipelines")  // v3 版本
@Path("/v4/user/pipelines")  // v4 版本
```

### OpenAPI 版本

```
openapi/api-openapi/src/main/kotlin/com/tencent/devops/openapi/api/apigw/
├── v3/    # v3 版本接口
└── v4/    # v4 版本接口
```

## 安全规范

1. **认证**：所有接口需要通过 `@HeaderParam(AUTH_HEADER_USER_ID)` 获取用户身份
2. **鉴权**：调用 auth 服务进行权限校验
3. **参数校验**：使用 `@BkField` 注解进行参数验证
4. **敏感数据**：响应中不返回密码、密钥等敏感信息
