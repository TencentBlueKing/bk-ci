---
name: api-interface-design
description: API 接口设计规范，涵盖 RESTful 设计原则、URL 命名、HTTP 方法选择、请求响应格式、错误码定义、版本控制。当用户设计 API 接口、定义 Resource 类、编写接口文档或进行接口评审时使用。
core_files:
  - "src/backend/ci/core/common/common-api/src/main/kotlin/com/tencent/devops/common/api/pojo/Result.kt"
  - "src/backend/ci/core/common/common-api/src/main/kotlin/com/tencent/devops/common/api/pojo/Page.kt"
related_skills:
  - 01-backend-microservice-development
  - common-technical-practices
token_estimate: 2500
---

# API 接口设计

## Quick Reference

```
路径格式：/{scope}/{resource}/{resourceId}/{subResource}
路径前缀：/user/(Web) | /service/(内部) | /build/(Agent) | /open/(外部)
返回格式：Result<T> { status, message, data }
分页格式：Page<T> { count, page, pageSize, totalPages, records }
错误码格式：21(平台)01(服务)001(业务码) → 如 2100013 = 无效参数
```

### 最简示例

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
        @HeaderParam(AUTH_HEADER_USER_ID) userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId") projectId: String,
        @QueryParam("page") page: Int?,
        @QueryParam("pageSize") pageSize: Int?
    ): Result<Page<PipelineInfo>>
}
```

## When to Use

- 设计 RESTful API 接口
- 定义 Resource 类
- 需要了解错误码规范
- 设计请求/响应数据结构

## When NOT to Use

- 实现业务逻辑 → 使用 `01-backend-microservice-development`
- 参数校验规则 → 使用 `common-technical-practices` (reference/4-parameter-validation.md)

---

## 路径命名规范

| 路径前缀 | 用途 | 调用方 |
|---------|------|--------|
| `/user/` | 用户态接口 | 前端 Web |
| `/service/` | 服务间调用 | 其他微服务 |
| `/build/` | 构建相关 | Agent/Worker |
| `/open/` | 对外开放 | 第三方系统 |

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

## 错误码规范

```
21(平台)01(服务)001(业务码)

平台：21 = BK-CI
服务：01 = 通用, 02 = process, 03 = project ...
业务码：001-999 = 具体错误
```

### 抛出错误

```kotlin
throw ErrorCodeException(
    statusCode = 400,
    errorCode = "2100013",
    defaultMessage = "无效参数",
    params = arrayOf(paramName)
)
```

## 统一返回格式

```kotlin
// 成功返回
data class Result<T>(
    val status: Int,      // 状态码
    val message: String?, // 提示信息
    val data: T?          // 返回数据
)

// 分页返回
data class Page<T>(
    val count: Long,      // 总数
    val page: Int,        // 当前页
    val pageSize: Int,    // 每页数量
    val totalPages: Int,  // 总页数
    val records: List<T>  // 数据列表
)
```

## 请求/响应对象命名

| 类型 | 命名模式 | 示例 |
|------|---------|------|
| 创建请求 | `[Resource]CreateRequest` | `PipelineCreateRequest` |
| 更新请求 | `[Resource]UpdateRequest` | `PipelineUpdateRequest` |
| 详情响应 | `[Resource]Info` | `PipelineInfo` |
| 列表项 | `[Resource]Summary` | `PipelineSummary` |

## 接口版本管理

```kotlin
@Path("/v3/user/pipelines")  // v3 版本
@Path("/v4/user/pipelines")  // v4 版本
```

---

## Checklist

设计 API 前确认：
- [ ] 路径前缀符合调用方场景
- [ ] 使用正确的 HTTP 方法
- [ ] 返回值使用 `Result<T>` 包装
- [ ] 分页接口使用 `Page<T>`
- [ ] 错误码符合规范格式
- [ ] 添加完整的 Swagger 注解
