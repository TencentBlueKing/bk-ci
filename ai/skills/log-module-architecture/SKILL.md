---
name: log-module-architecture
description: Log 构建日志模块架构指南，涵盖日志接收存储、实时流式输出、多存储后端（ES/Lucene）、日志索引管理。当用户需要开发日志功能、查询构建日志、实现日志存储或处理日志流时使用。
---

# Log 构建日志模块架构指南

## 概述

Log（构建日志）模块是 BK-CI 的日志服务，负责接收、存储和查询流水线构建过程中产生的日志数据。

**模块职责**：
- 构建日志的接收与存储
- 日志的实时流式输出
- 日志的查询与下载
- 多存储后端支持（ES、Lucene）
- 日志索引管理

## 一、模块结构

```
src/backend/ci/core/log/
├── api-log/               # API 接口定义
│   └── src/main/kotlin/com/tencent/devops/log/
│       ├── api/           # Resource 接口
│       │   ├── AppLogResource.kt       # App 级别接口
│       │   ├── UserLogResource.kt      # 用户级别接口
│       │   ├── ServiceLogResource.kt   # 服务间接口
│       │   ├── OpLogResource.kt        # 运营接口
│       │   └── print/                  # 日志打印接口
│       │       ├── BuildLogPrintResource.kt
│       │       └── ServiceLogPrintResource.kt
│       ├── configuration/  # 配置
│       │   └── LogPrinterConfiguration.kt
│       └── meta/          # ANSI 颜色处理
│           ├── Ansi.kt
│           ├── AnsiColor.kt
│           └── AnsiAttribute.kt
├── biz-log/               # 业务逻辑层
│   └── biz-log-lucene/    # Lucene 存储实现
└── boot-log/              # 启动模块
```

## 二、日志存储架构

### 2.1 存储模式

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        日志存储架构                                      │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                          │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     日志接收层                                    │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ Agent       │  │ 插件        │  │ 其他服务                │   │   │
│  │  │ 日志上报    │  │ 日志输出    │  │ 日志调用                │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     日志服务层                                    │   │
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────────┐   │   │
│  │  │ 日志接收    │  │ 日志解析    │  │ 日志分发                │   │   │
│  │  │ LogPrint    │  │ ANSI处理    │  │ 多后端写入              │   │   │
│  │  └─────────────┘  └─────────────┘  └─────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                              │                                           │
│                              ▼                                           │
│  ┌──────────────────────────────────────────────────────────────────┐   │
│  │                     存储后端层                                    │   │
│  │  ┌─────────────────────────┐  ┌─────────────────────────────┐   │   │
│  │  │     ElasticSearch       │  │        Lucene               │   │   │
│  │  │  - 分布式存储            │  │  - 本地存储                  │   │   │
│  │  │  - 全文检索              │  │  - 轻量级部署                │   │   │
│  │  └─────────────────────────┘  └─────────────────────────────┘   │   │
│  └──────────────────────────────────────────────────────────────────┘   │
│                                                                          │
└─────────────────────────────────────────────────────────────────────────┘
```

### 2.2 存储模式选择

| 模式 | 说明 | 适用场景 |
|------|------|---------|
| ElasticSearch | 分布式全文搜索引擎 | 大规模生产环境 |
| Lucene | 本地索引存储 | 轻量级部署、单机环境 |

## 三、数据库设计

### 3.1 核心表结构

| 表名 | 说明 |
|------|------|
| `T_LOG_INDICES_V2` | 日志索引表（关联 ES 索引） |
| `T_LOG_STATUS` | 日志状态表（记录日志打印状态） |
| `T_LOG_SUBTAGS` | 日志子标签表 |

### 3.2 关键字段说明

**T_LOG_INDICES_V2**：
```sql
- ID: 主键
- BUILD_ID: 构建ID
- INDEX_NAME: ES 索引名称
- LAST_LINE_NUM: 最后行号
- ENABLE: 是否启用 V2
- LOG_CLUSTER_NAME: ES 集群名称
- USE_CLUSTER: 是否使用多集群
```

**T_LOG_STATUS**：
```sql
- ID: 主键
- BUILD_ID: 构建ID
- TAG: 标签（通常是插件ID）
- SUB_TAG: 子标签
- JOB_ID: Job ID（Container Hash ID）
- USER_JOB_ID: 真正的 Job ID
- STEP_ID: 插件步骤ID
- MODE: 存储模式
- EXECUTE_COUNT: 执行次数
- FINISHED: 是否完成
```

## 四、API 接口设计

### 4.1 日志打印接口

```kotlin
@Path("/build/logs/print")
interface BuildLogPrintResource {
    
    @POST
    @Path("/")
    fun addLogLine(
        @HeaderParam(AUTH_HEADER_BUILD_ID) buildId: String,
        @HeaderParam(AUTH_HEADER_PIPELINE_ID) pipelineId: String,
        @HeaderParam(AUTH_HEADER_PROJECT_ID) projectId: String,
        logMessage: LogMessage
    ): Result<Boolean>
    
    @POST
    @Path("/multi")
    fun addLogMultiLine(
        @HeaderParam(AUTH_HEADER_BUILD_ID) buildId: String,
        @HeaderParam(AUTH_HEADER_PIPELINE_ID) pipelineId: String,
        @HeaderParam(AUTH_HEADER_PROJECT_ID) projectId: String,
        logMessages: List<LogMessage>
    ): Result<Boolean>
    
    @POST
    @Path("/status")
    fun updateLogStatus(
        @HeaderParam(AUTH_HEADER_BUILD_ID) buildId: String,
        @HeaderParam(AUTH_HEADER_PIPELINE_ID) pipelineId: String,
        @HeaderParam(AUTH_HEADER_PROJECT_ID) projectId: String,
        logStatusEvent: LogStatusEvent
    ): Result<Boolean>
}

// 日志消息
data class LogMessage(
    val message: String,           // 日志内容
    val timestamp: Long,           // 时间戳
    val tag: String?,              // 标签
    val subTag: String?,           // 子标签
    val jobId: String?,            // Job ID
    val logType: LogType?,         // 日志类型
    val executeCount: Int?         // 执行次数
)
```

### 4.2 日志查询接口

```kotlin
@Path("/user/logs")
interface UserLogResource {
    
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun getInitLogs(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @QueryParam("tag") tag: String?,
        @QueryParam("subTag") subTag: String?,
        @QueryParam("jobId") jobId: String?,
        @QueryParam("executeCount") executeCount: Int?
    ): Result<QueryLogs>
    
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/more")
    fun getMoreLogs(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @QueryParam("num") num: Int?,
        @QueryParam("fromStart") fromStart: Boolean?,
        @QueryParam("start") start: Long,
        @QueryParam("end") end: Long,
        @QueryParam("tag") tag: String?,
        @QueryParam("subTag") subTag: String?,
        @QueryParam("jobId") jobId: String?,
        @QueryParam("executeCount") executeCount: Int?
    ): Result<QueryLogs>
    
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/download")
    fun downloadLogs(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @QueryParam("tag") tag: String?,
        @QueryParam("subTag") subTag: String?,
        @QueryParam("jobId") jobId: String?,
        @QueryParam("executeCount") executeCount: Int?
    ): Response
}
```

### 4.3 服务间接口

```kotlin
@Path("/service/logs")
interface ServiceLogResource {
    
    @POST
    @Path("/{projectId}/{pipelineId}/{buildId}/")
    fun addLogLine(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        logMessage: LogMessage
    ): Result<Boolean>
    
    @GET
    @Path("/{projectId}/{pipelineId}/{buildId}/status")
    fun getLogStatus(
        @PathParam("projectId") projectId: String,
        @PathParam("pipelineId") pipelineId: String,
        @PathParam("buildId") buildId: String,
        @QueryParam("tag") tag: String?,
        @QueryParam("executeCount") executeCount: Int?
    ): Result<LogStatus>
}
```

## 五、日志处理流程

### 5.1 日志写入流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ Agent/  │     │ Log     │     │ Log     │     │ ES/     │
│ Plugin  │     │ Print   │     │ Service │     │ Lucene  │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.上报日志    │               │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │ 2.解析ANSI    │               │
     │               │───────┐       │               │
     │               │       │       │               │
     │               │<──────┘       │               │
     │               │               │               │
     │               │ 3.批量写入    │               │
     │               │──────────────>│               │
     │               │               │               │
     │               │               │ 4.索引存储    │
     │               │               │──────────────>│
     │               │               │               │
     │ 5.确认        │               │               │
     │<──────────────│               │               │
```

### 5.2 日志查询流程

```
┌─────────┐     ┌─────────┐     ┌─────────┐     ┌─────────┐
│ 前端    │     │ Log     │     │ Log     │     │ ES/     │
│         │     │ Resource│     │ Service │     │ Lucene  │
└────┬────┘     └────┬────┘     └────┬────┘     └────┬────┘
     │               │               │               │
     │ 1.查询日志    │               │               │
     │──────────────>│               │               │
     │               │               │               │
     │               │ 2.获取索引    │               │
     │               │──────────────>│               │
     │               │               │               │
     │               │               │ 3.查询存储    │
     │               │               │──────────────>│
     │               │               │               │
     │               │               │ 4.返回结果    │
     │               │               │<──────────────│
     │               │               │               │
     │               │ 5.格式化日志  │               │
     │               │<──────────────│               │
     │               │               │               │
     │ 6.返回日志    │               │               │
     │<──────────────│               │               │
```

## 六、ANSI 颜色支持

### 6.1 颜色代码

```kotlin
enum class AnsiColor(val code: Int) {
    BLACK(30),
    RED(31),
    GREEN(32),
    YELLOW(33),
    BLUE(34),
    MAGENTA(35),
    CYAN(36),
    WHITE(37),
    DEFAULT(39)
}

enum class AnsiAttribute(val code: Int) {
    RESET(0),
    BOLD(1),
    ITALIC(3),
    UNDERLINE(4)
}
```

### 6.2 日志着色示例

```kotlin
// 在 Agent/插件中输出彩色日志
println("\u001B[32m[SUCCESS]\u001B[0m Build completed")  // 绿色
println("\u001B[31m[ERROR]\u001B[0m Build failed")       // 红色
println("\u001B[33m[WARNING]\u001B[0m Low disk space")   // 黄色
```

## 七、与其他模块的关系

### 7.1 依赖关系

```
Log 模块
    │
    ├──< Process（流水线）
    │    - 构建过程日志输出
    │    - 日志状态管理
    │
    ├──< Worker（构建机）
    │    - Agent 日志上报
    │    - 插件日志输出
    │
    ├──< Dispatch（调度）
    │    - 调度日志记录
    │
    └──> ElasticSearch / Lucene
         - 日志存储后端
```

## 八、开发规范

### 8.1 日志打印示例

```kotlin
// 在插件中打印日志
class MyPlugin : Plugin {
    override fun execute(context: PluginContext) {
        // 普通日志
        context.logger.info("开始执行插件")
        
        // 带颜色的日志
        context.logger.info("\u001B[32m[SUCCESS]\u001B[0m 步骤完成")
        
        // 错误日志
        context.logger.error("执行失败: ${e.message}")
    }
}

// 通过 API 打印日志
client.get(ServiceLogPrintResource::class).addLogLine(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    logMessage = LogMessage(
        message = "自定义日志内容",
        timestamp = System.currentTimeMillis(),
        tag = elementId,
        logType = LogType.LOG
    )
)
```

### 8.2 日志查询示例

```kotlin
// 获取构建日志
val logs = client.get(ServiceLogResource::class).getInitLogs(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    tag = elementId,
    executeCount = 1
)

// 获取更多日志（分页）
val moreLogs = client.get(ServiceLogResource::class).getMoreLogs(
    projectId = projectId,
    pipelineId = pipelineId,
    buildId = buildId,
    start = 100,
    end = 200,
    tag = elementId
)
```

### 8.3 日志标签规范

| 标签 | 说明 |
|------|------|
| `tag` | 通常是插件/Element ID |
| `subTag` | 子任务标识 |
| `jobId` | Container Hash ID |
| `userJobId` | 用户定义的 Job ID |
| `stepId` | 用户定义的步骤 ID |

## 九、常见问题

**Q: 日志存储在哪里？**
A: 根据配置，可以存储在 ElasticSearch（生产环境推荐）或 Lucene（轻量级部署）。

**Q: 日志保留多久？**
A: 通过 ES 索引策略或配置文件设置，通常保留 30-90 天。

**Q: 如何查看特定插件的日志？**
A: 使用 `tag` 参数过滤，`tag` 通常对应插件的 Element ID。

**Q: 日志太大怎么办？**
A: 
1. 使用分页查询（`start`/`end` 参数）
2. 下载日志文件
3. 按 `tag`/`subTag` 过滤

---

**版本**: 1.0.0 | **更新日期**: 2025-12-11
