

# Process 模块 API 层详细分析

> **模块路径**: `src/backend/ci/core/process/api-process/src/main/kotlin/com/tencent/devops/process/api/`

## 一、API 层概述

### 1.1 目录结构

```
api-process/src/main/kotlin/com/tencent/devops/process/api/
├── user/           # 用户级接口（前端调用）- 20+ 文件
├── service/        # 服务间调用接口 - 25+ 文件
├── builds/         # 构建机调用接口 - 8 文件
├── template/       # 模板相关接口 - 6 文件
├── op/             # 运维操作接口 - 12 文件
├── open/           # 开放平台接口 - 5 文件
└── app/            # App 端接口 - 3 文件
```

### 1.2 接口分类统计

| 分类 | 文件数 | 主要用途 | 调用方 |
|------|--------|----------|--------|
| **user/** | 20+ | 用户操作流水线、构建 | 前端 Web |
| **service/** | 25+ | 微服务间调用 | 其他微服务 |
| **builds/** | 8 | 构建机回调 | Agent |
| **template/** | 6 | 模板管理 | 前端/服务 |
| **op/** | 12 | 运维管理 | 运维平台 |

## 二、User 接口详解

### 2.1 UserPipelineResource - 流水线管理

**文件**: `user/UserPipelineResource.kt`

**路径**: `/user/pipelines`

```kotlin
@Tag(name = "USER_PIPELINE", description = "用户-流水线资源")
@Path("/user/pipelines")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserPipelineResource {

    @Operation(summary = "新建流水线编排")
    @POST
    @Path("/projects/{projectId}")
    fun create(
        @Parameter(description = "用户ID", required = true)
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @Parameter(description = "项目ID", required = true)
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model,
        @Parameter(description = "渠道号，默认为BS", required = false)
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<PipelineId>

    @Operation(summary = "编辑流水线编排")
    @PUT
    @Path("/projects/{projectId}/{pipelineId}")
    fun edit(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线模型", required = true)
        pipeline: Model,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<DeployPipelineResult>

    @Operation(summary = "复制流水线编排")
    @POST
    @Path("/projects/{projectId}/{pipelineId}/copy")
    fun copy(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线COPY", required = true)
        pipeline: PipelineCopy
    ): Result<PipelineId>

    @Operation(summary = "删除流水线编排")
    @DELETE
    @Path("/projects/{projectId}/{pipelineId}")
    fun delete(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Boolean>

    @Operation(summary = "获取流水线编排")
    @GET
    @Path("/projects/{projectId}/{pipelineId}")
    fun get(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Model>

    @Operation(summary = "流水线重命名")
    @POST
    @Path("/projects/{projectId}/{pipelineId}/rename")
    fun rename(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "流水线名称", required = true)
        name: PipelineName
    ): Result<Boolean>

    @Operation(summary = "还原流水线编排")
    @PUT
    @Path("/projects/{projectId}/{pipelineId}/restore")
    fun restore(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String
    ): Result<Boolean>

    @Operation(summary = "收藏流水线")
    @PUT
    @Path("/projects/{projectId}/{pipelineId}/favor")
    fun favor(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "是否收藏", required = true)
        @QueryParam("favor")
        favor: Boolean
    ): Result<Boolean>
}
```

**核心方法说明**:

| 方法 | HTTP | 路径 | 功能 |
|------|------|------|------|
| `create` | POST | `/projects/{projectId}` | 创建新流水线 |
| `edit` | PUT | `/projects/{projectId}/{pipelineId}` | 编辑流水线 |
| `copy` | POST | `/projects/{projectId}/{pipelineId}/copy` | 复制流水线 |
| `delete` | DELETE | `/projects/{projectId}/{pipelineId}` | 删除流水线 |
| `get` | GET | `/projects/{projectId}/{pipelineId}` | 获取流水线模型 |
| `rename` | POST | `/projects/{projectId}/{pipelineId}/rename` | 重命名 |
| `restore` | PUT | `/projects/{projectId}/{pipelineId}/restore` | 还原已删除流水线 |
| `favor` | PUT | `/projects/{projectId}/{pipelineId}/favor` | 收藏/取消收藏 |

### 2.2 UserBuildResource - 构建管理

**文件**: `user/UserBuildResource.kt`

**路径**: `/user/builds`

```kotlin
@Tag(name = "USER_BUILD", description = "用户-构建资源")
@Path("/user/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface UserBuildResource {

    @Operation(summary = "启动构建")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/start")
    fun manualStartup(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "启动参数", required = true)
        values: Map<String, String>,
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @QueryParam("buildNo")
        buildNo: Int?
    ): Result<BuildId>

    @Operation(summary = "停止构建")
    @DELETE
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/stop")
    fun manualShutdown(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Boolean>

    @Operation(summary = "重试构建")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/retry")
    fun retry(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @QueryParam("taskId")
        taskId: String?,
        @QueryParam("failedContainer")
        failedContainer: Boolean?,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<BuildId>

    @Operation(summary = "获取构建详情")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    fun getBuildDetail(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<ModelDetail>

    @Operation(summary = "获取构建历史")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/history")
    fun getHistoryBuild(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("page")
        page: Int?,
        @QueryParam("pageSize")
        pageSize: Int?,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<BuildHistoryPage<BuildHistory>>

    @Operation(summary = "获取构建状态")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/status")
    fun getBuildStatus(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<BuildHistoryWithVars>

    @Operation(summary = "获取构建变量")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/variables")
    fun getBuildVars(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String
    ): Result<BuildHistoryVariables>
}
```

**核心方法说明**:

| 方法 | HTTP | 路径 | 功能 |
|------|------|------|------|
| `manualStartup` | POST | `.../start` | 手动启动构建 |
| `manualShutdown` | DELETE | `.../stop` | 停止构建 |
| `retry` | POST | `.../retry` | 重试构建（可指定任务） |
| `getBuildDetail` | GET | `.../detail` | 获取构建详情（含模型） |
| `getHistoryBuild` | GET | `.../history` | 分页获取构建历史 |
| `getBuildStatus` | GET | `.../status` | 获取构建状态 |
| `getBuildVars` | GET | `.../variables` | 获取构建变量 |

### 2.3 UserPipelineViewResource - 视图管理

**文件**: `user/UserPipelineViewResource.kt`

**路径**: `/user/pipelineViews`

```kotlin
@Tag(name = "USER_PIPELINE_VIEW", description = "用户-流水线视图")
@Path("/user/pipelineViews")
interface UserPipelineViewResource {

    @Operation(summary = "获取视图列表")
    @GET
    @Path("/projects/{projectId}")
    fun listView(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String
    ): Result<List<PipelineViewInfo>>

    @Operation(summary = "创建视图")
    @POST
    @Path("/projects/{projectId}")
    fun addView(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        pipelineView: PipelineViewForm
    ): Result<PipelineViewId>

    @Operation(summary = "获取视图下的流水线")
    @GET
    @Path("/projects/{projectId}/views/{viewId}/pipelines")
    fun listViewPipelines(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("viewId")
        viewId: String,
        @QueryParam("page")
        page: Int?,
        @QueryParam("pageSize")
        pageSize: Int?
    ): Result<PipelineViewPipelinePage<PipelineInfo>>
}
```

## 三、Service 接口详解

### 3.1 ServiceBuildResource - 服务间构建调用

**文件**: `service/ServiceBuildResource.kt`

**路径**: `/service/builds`

```kotlin
@Tag(name = "SERVICE_BUILD", description = "服务-构建资源")
@Path("/service/builds")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
interface ServiceBuildResource {

    @Operation(summary = "启动流水线构建")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/start")
    fun manualStartupNew(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @Parameter(description = "启动参数", required = true)
        values: Map<String, String>,
        @QueryParam("channelCode")
        channelCode: ChannelCode?,
        @QueryParam("buildNo")
        buildNo: Int?,
        @QueryParam("startType")
        startType: StartType?
    ): Result<BuildId>

    @Operation(summary = "获取构建详情")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/detail")
    fun getBuildDetail(
        @HeaderParam(AUTH_HEADER_USER_ID)
        userId: String,
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<ModelDetail>

    @Operation(summary = "获取构建变量值")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/variables")
    fun getBuildVariableValue(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "变量名列表", required = true)
        variableNames: List<String>
    ): Result<Map<String, String>>

    @Operation(summary = "批量获取构建状态")
    @POST
    @Path("/projects/{projectId}/batchStatus")
    fun getBatchBuildStatus(
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "构建ID列表", required = true)
        buildIds: Set<String>,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<List<BuildHistory>>

    @Operation(summary = "根据流水线ID获取最新构建")
    @POST
    @Path("/projects/{projectId}/latestBuild")
    fun getPipelineLatestBuild(
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID列表", required = true)
        pipelineIds: List<String>
    ): Result<Map<String, BuildHistory?>>

    @Operation(summary = "设置构建变量")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/variables/set")
    fun setVarByBuild(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "变量键值对", required = true)
        varValues: Map<String, String>
    ): Result<Boolean>
}
```

**核心方法说明**:

| 方法 | 功能 | 使用场景 |
|------|------|----------|
| `manualStartupNew` | 启动构建 | 其他服务触发构建 |
| `getBuildDetail` | 获取构建详情 | 查询构建状态 |
| `getBuildVariableValue` | 获取构建变量 | 跨服务获取变量 |
| `getBatchBuildStatus` | 批量获取状态 | 批量查询优化 |
| `getPipelineLatestBuild` | 获取最新构建 | 流水线状态展示 |
| `setVarByBuild` | 设置构建变量 | 动态修改变量 |

### 3.2 ServicePipelineResource - 服务间流水线调用

**文件**: `service/ServicePipelineResource.kt`

**路径**: `/service/pipelines`

```kotlin
@Tag(name = "SERVICE_PIPELINE", description = "服务-流水线资源")
@Path("/service/pipelines")
interface ServicePipelineResource {

    @Operation(summary = "获取流水线信息")
    @GET
    @Path("/projects/{projectId}/{pipelineId}")
    fun getPipelineInfo(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<PipelineInfo?>

    @Operation(summary = "批量获取流水线信息")
    @POST
    @Path("/projects/{projectId}/batchGet")
    fun batchGetPipelineInfo(
        @PathParam("projectId")
        projectId: String,
        @Parameter(description = "流水线ID列表", required = true)
        pipelineIds: List<String>,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<List<PipelineInfo>>

    @Operation(summary = "获取流水线模型")
    @GET
    @Path("/projects/{projectId}/{pipelineId}/model")
    fun getModel(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("version")
        version: Int?
    ): Result<Model?>

    @Operation(summary = "判断流水线是否存在")
    @GET
    @Path("/projects/{projectId}/{pipelineId}/exist")
    fun isPipelineExist(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @QueryParam("channelCode")
        channelCode: ChannelCode?
    ): Result<Boolean>

    @Operation(summary = "根据流水线名称获取ID")
    @GET
    @Path("/projects/{projectId}/getPipelineIdByName")
    fun getPipelineIdByName(
        @PathParam("projectId")
        projectId: String,
        @QueryParam("pipelineName")
        pipelineName: String
    ): Result<String?>
}
```

## 四、Build 接口详解（构建机调用）

### 4.1 BuildBuildResource - 构建机回调

**文件**: `builds/BuildBuildResource.kt`

**路径**: `/build/builds`

```kotlin
@Tag(name = "BUILD_BUILD", description = "构建-构建机资源")
@Path("/build/builds")
interface BuildBuildResource {

    @Operation(summary = "构建机启动完成回调")
    @PUT
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/vmStatus")
    fun setVMStatus(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "状态", required = true)
        status: BuildStatus
    ): Result<Boolean>

    @Operation(summary = "获取待执行任务")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/claim")
    fun claimTask(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<BuildTask>

    @Operation(summary = "任务执行完成回调")
    @PUT
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/tasks/{taskId}/complete")
    fun completeTask(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @PathParam("taskId")
        taskId: String,
        @PathParam("vmSeqId")
        vmSeqId: String,
        @Parameter(description = "任务结果", required = true)
        result: BuildTaskResult
    ): Result<Boolean>

    @Operation(summary = "心跳上报")
    @PUT
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/heartbeat")
    fun heartbeat(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @PathParam("vmSeqId")
        vmSeqId: String
    ): Result<HeartBeatInfo>

    @Operation(summary = "获取构建变量")
    @GET
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/variables")
    fun getAllVariable(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String
    ): Result<BuildVariables>

    @Operation(summary = "设置构建变量")
    @POST
    @Path("/projects/{projectId}/pipelines/{pipelineId}/builds/{buildId}/variables")
    fun setVariable(
        @PathParam("projectId")
        projectId: String,
        @PathParam("pipelineId")
        pipelineId: String,
        @PathParam("buildId")
        buildId: String,
        @Parameter(description = "变量键值对", required = true)
        varValues: Map<String, String>
    ): Result<Boolean>
}
```

**构建机交互流程**:

```
Agent 启动
    │
    ▼
setVMStatus(RUNNING)     # 通知构建机已启动
    │
    ▼
claimTask()              # 领取待执行任务
    │
    ▼
执行任务...
    │
    ├─► getAllVariable()  # 获取变量
    ├─► setVariable()     # 设置变量
    ├─► heartbeat()       # 心跳上报
    │
    ▼
completeTask()           # 任务完成回调
    │
    ▼
claimTask()              # 继续领取下一个任务
    │
    ▼
... 循环直到无任务
```

## 五、接口注解规范

### 5.1 类级别注解

```kotlin
@Tag(name = "USER_PIPELINE", description = "用户-流水线资源")  // Swagger 分组
@Path("/user/pipelines")                                      // 基础路径
@Produces(MediaType.APPLICATION_JSON)                         // 响应类型
@Consumes(MediaType.APPLICATION_JSON)                         // 请求类型
interface UserPipelineResource { ... }
```

### 5.2 方法级别注解

```kotlin
@Operation(summary = "新建流水线编排")  // Swagger 描述
@POST                                   // HTTP 方法
@Path("/projects/{projectId}")          // 路径
fun create(
    @Parameter(description = "用户ID", required = true)
    @HeaderParam(AUTH_HEADER_USER_ID)   // 请求头参数
    userId: String,
    
    @Parameter(description = "项目ID", required = true)
    @PathParam("projectId")             // 路径参数
    projectId: String,
    
    @Parameter(description = "渠道号", required = false)
    @QueryParam("channelCode")          // 查询参数
    channelCode: ChannelCode?,
    
    @Parameter(description = "流水线模型", required = true)
    pipeline: Model                     // 请求体
): Result<PipelineId>                   // 统一返回包装
```

### 5.3 参数校验注解

```kotlin
@BkField(minLength = 1, maxLength = 128)           // 长度校验
@BkField(patternStyle = BkStyleEnum.CODE_STYLE)    // 格式校验
@BkField(required = true)                          // 必填校验
```

## 六、接口实现规范

### 6.1 实现类位置

```
biz-process/src/main/kotlin/com/tencent/devops/process/api/
├── user/
│   ├── UserPipelineResourceImpl.kt
│   ├── UserBuildResourceImpl.kt
│   └── ...
├── service/
│   ├── ServiceBuildResourceImpl.kt
│   ├── ServicePipelineResourceImpl.kt
│   └── ...
└── builds/
    └── BuildBuildResourceImpl.kt
```

### 6.2 实现类模板

```kotlin
@RestResource
class UserPipelineResourceImpl @Autowired constructor(
    private val pipelineInfoFacadeService: PipelineInfoFacadeService,
    private val pipelinePermissionService: PipelinePermissionService
) : UserPipelineResource {

    override fun create(
        userId: String,
        projectId: String,
        pipeline: Model,
        channelCode: ChannelCode?
    ): Result<PipelineId> {
        // 1. 权限校验
        pipelinePermissionService.checkPipelinePermission(
            userId = userId,
            projectId = projectId,
            permission = AuthPermission.CREATE
        )
        
        // 2. 调用 Facade Service
        val pipelineId = pipelineInfoFacadeService.createPipeline(
            userId = userId,
            projectId = projectId,
            model = pipeline,
            channelCode = channelCode ?: ChannelCode.BS
        )
        
        return Result(PipelineId(pipelineId))
    }
}
```

## 七、新增 API 检查清单

- [ ] 接口定义在 `api-process` 模块的对应目录
- [ ] 使用正确的 `@Tag` 分组
- [ ] 使用 `@Operation` 描述接口功能
- [ ] 使用 `@Parameter` 描述参数
- [ ] 返回类型使用 `Result<T>` 包装
- [ ] 实现类在 `biz-process` 模块
- [ ] 实现类使用 `@RestResource` 注解
- [ ] 通过构造器注入依赖

---

**版本**: 1.0.0 | **更新日期**: 2025-12-10
