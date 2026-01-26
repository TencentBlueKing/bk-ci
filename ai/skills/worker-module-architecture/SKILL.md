---
name: worker-module-architecture
description: Worker 构建执行器模块架构指南，涵盖插件执行引擎、任务分发、日志上报、制品上传、Worker 生命周期。当用户开发 Worker 功能、实现插件执行、处理任务分发或优化执行器性能时使用。
---

# Worker 构建执行器模块架构指南

## 模块概述

Worker 模块是 BK-CI 构建执行器的核心组件，运行在构建机（Agent）上，负责接收并执行流水线任务。它是连接 CI 服务端与构建环境的桥梁，实现了任务调度、插件执行、日志上报、心跳维护等关键功能。

### 核心职责

1. **任务执行**：接收并执行流水线中的各类任务（脚本、插件等）
2. **日志服务**：收集构建日志并上报到 Log 服务
3. **心跳维护**：定期向服务端发送心跳，维持构建状态
4. **变量管理**：管理构建过程中的环境变量和上下文
5. **制品归档**：将构建产物上传到制品库
6. **插件运行**：下载并执行研发商店中的插件

### 运行模式

Worker 支持三种构建类型：

| 类型 | 说明 | 场景 |
|------|------|------|
| `DOCKER` | Docker 容器构建 | 公共构建资源池 |
| `AGENT` | 第三方构建机 | 用户自有构建机 |
| `WORKER` | 无编译环境 | 轻量级任务执行 |

---

## 目录结构

```
src/backend/ci/core/worker/
├── build.gradle.kts              # 模块构建配置
├── worker-agent/                 # Agent 启动入口
│   ├── src/main/kotlin/
│   │   └── com/tencent/devops/agent/
│   │       ├── Application.kt   # 主入口，根据构建类型启动
│   │       ├── AgentVersion.kt  # 版本信息
│   │       └── service/
│   │           └── BuildLessStarter.kt  # 无编译构建启动器
│   └── src/test/kotlin/          # 测试代码
├── worker-common/                # 公共组件库
│   ├── src/main/kotlin/
│   │   └── com/tencent/devops/worker/common/
│   │       ├── Runner.kt         # 核心运行器
│   │       ├── WorkRunner.kt     # 第三方构建机运行器
│   │       ├── api/              # API 客户端
│   │       ├── env/              # 环境变量管理
│   │       ├── heartbeat/        # 心跳服务
│   │       ├── logger/           # 日志服务
│   │       ├── service/          # 业务服务
│   │       ├── task/             # 任务执行框架
│   │       ├── utils/            # 工具类
│   │       └── constants/        # 常量定义
│   └── src/main/kotlin/
│       └── com/tencent/devops/plugin/worker/task/
│           ├── archive/          # 归档任务
│           ├── scm/              # 代码拉取任务
│           └── image/            # 镜像任务
└── worker-api-sdk/               # API SDK 实现
    └── src/main/kotlin/
        └── com/tencent/devops/worker/common/api/
            ├── archive/          # 制品库 API
            ├── atom/             # 插件 API
            ├── log/              # 日志 API
            ├── process/          # 流水线 API
            └── ...               # 其他 API
```

---

## 核心组件

### 1. Runner - 核心运行器

`Runner.kt` 是 Worker 的核心入口，负责整个构建生命周期管理：

```kotlin
object Runner {
    fun run(workspaceInterface: WorkspaceInterface, systemExit: Boolean = true) {
        // 1. 获取构建变量
        val buildVariables = getBuildVariables()
        
        // 2. 准备工作空间、启动日志服务、启动心跳
        val workspacePathFile = prepareWorker(buildVariables, workspaceInterface)
        
        // 3. 循环获取并执行任务
        loopPickup(workspacePathFile, buildVariables)
        
        // 4. 清理并结束构建
        finishWorker(buildVariables)
    }
}
```

**核心流程**：

```
┌─────────────────────────────────────────────────────────────┐
│                    Worker 启动流程                           │
├─────────────────────────────────────────────────────────────┤
│  1. EngineService.setStarted()  →  上报启动状态，获取构建变量   │
│  2. LoggerService.start()       →  启动日志服务                │
│  3. Heartbeat.start()           →  启动心跳监控                │
│  4. loopPickup()                →  循环领取任务                │
│     ├── claimTask()             →  获取待执行任务              │
│     ├── TaskFactory.create()    →  创建任务执行器              │
│     ├── taskDaemon.run()        →  执行任务                   │
│     └── completeTask()          →  上报任务结果                │
│  5. finishWorker()              →  结束构建                   │
└─────────────────────────────────────────────────────────────┘
```

### 2. TaskFactory - 任务工厂

任务工厂负责根据任务类型创建对应的执行器：

```kotlin
object TaskFactory {
    private val taskMap = ConcurrentHashMap<String, KClass<out ITask>>()
    
    fun init() {
        // 注册内置任务类型
        register(LinuxScriptElement.classType, LinuxScriptTask::class)
        register(WindowsScriptElement.classType, WindowsScriptTask::class)
        register(MarketBuildAtomElement.classType, MarketAtomTask::class)
        
        // 通过反射扫描注册插件任务
        val reflections = Reflections("com.tencent.devops.plugin.worker.task")
        // ...
    }
    
    fun create(type: String): ITask {
        val clazz = taskMap[type] ?: return EmptyTask(type)
        return clazz.primaryConstructor?.call() ?: EmptyTask(type)
    }
}
```

### 3. ITask - 任务接口

所有任务执行器的基类：

```kotlin
abstract class ITask {
    private val environment = HashMap<String, String>()
    
    // 任务执行入口
    fun run(buildTask: BuildTask, buildVariables: BuildVariables, workspace: File) {
        execute(buildTask, buildVariables, workspace)
    }
    
    // 子类实现具体执行逻辑
    protected abstract fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    )
    
    // 添加环境变量（输出变量）
    protected fun addEnv(env: Map<String, String>) {
        // 校验只读变量、变量名合法性
        environment.putAll(env)
    }
    
    fun getAllEnv(): Map<String, String> = environment
}
```

### 4. LoggerService - 日志服务

负责构建日志的收集、缓冲和上报：

```kotlin
object LoggerService {
    private val uploadQueue = LinkedBlockingQueue<LogMessage>(2000)
    
    fun addNormalLine(message: String) {
        // 1. 处理日志前缀（DEBUG/ERROR/WARN）
        // 2. 敏感信息过滤
        // 3. 本地落盘（如需要）
        // 4. 放入上报队列
        uploadQueue.put(logMessage)
    }
    
    fun addErrorLine(message: String) {
        addNormalLine("$LOG_ERROR_FLAG$message")
    }
    
    fun addWarnLine(message: String) {
        addNormalLine("$LOG_WARN_FLAG$message")
    }
    
    // 日志折叠
    fun addFoldStartLine(foldName: String) {
        addLog(LogMessage(message = "##[group]$foldName", ...))
    }
    
    fun addFoldEndLine(foldName: String) {
        addLog(LogMessage(message = "##[endgroup]$foldName", ...))
    }
}
```

### 5. Heartbeat - 心跳服务

维持与服务端的连接，监控任务超时：

```kotlin
object Heartbeat {
    fun start(jobTimeoutMills: Long, executeCount: Int) {
        // 定时心跳（每2秒）
        executor.scheduleWithFixedDelay({
            val heartBeatInfo = EngineService.heartbeat(...)
            // 处理取消任务
            if (!heartBeatInfo.cancelTaskIds.isNullOrEmpty()) {
                KillBuildProcessTree.killProcessTree(...)
            }
        }, 10, 2, TimeUnit.SECONDS)
        
        // Job 超时监控
        executor.scheduleWithFixedDelay({
            LoggerService.addErrorLine("Job timeout")
            EngineService.timeout()
            exitProcess(99)
        }, jobTimeoutMills, jobTimeoutMills, TimeUnit.MILLISECONDS)
    }
}
```

### 6. EngineService - 引擎服务

与 Process 服务通信的客户端：

```kotlin
object EngineService {
    // 上报启动状态
    fun setStarted(): BuildVariables
    
    // 领取任务
    fun claimTask(): BuildTask
    
    // 完成任务
    fun completeTask(taskResult: BuildTaskResult)
    
    // 结束构建
    fun endBuild(variables: Map<String, String>, result: BuildJobResult)
    
    // 心跳
    fun heartbeat(executeCount: Int, jobHeartbeatRequest: JobHeartbeatRequest): HeartBeatInfo
    
    // 超时上报
    fun timeout()
    
    // 错误上报
    fun submitError(errorInfo: ErrorInfo)
}
```

---

## 任务类型

### 内置任务

| 任务类型 | 类名 | 说明 |
|----------|------|------|
| `linuxScript` | `LinuxScriptTask` | Linux Shell 脚本 |
| `windowsScript` | `WindowsScriptTask` | Windows Bat 脚本 |
| `marketBuild` | `MarketAtomTask` | 研发商店插件 |
| `marketBuildLess` | `MarketAtomTask` | 无编译环境插件 |

### 插件任务（plugin 包）

| 任务类型 | 类名 | 说明 |
|----------|------|------|
| `reportArchive` | `ReportArchiveTask` | 报告归档 |
| `singleFileArchive` | `SingleFileArchiveTask` | 单文件归档 |
| `buildArchiveGet` | `BuildArchiveGetTask` | 获取构建产物 |
| `customizeArchiveGet` | `CustomizeArchiveGetTask` | 获取自定义产物 |
| `codeGitPull` | `CodeGitPullTask` | Git 代码拉取 |
| `codeGitlabPull` | `CodeGitlabPullTask` | GitLab 代码拉取 |
| `codeSvnPull` | `CodeSvnPullTask` | SVN 代码拉取 |
| `githubPull` | `GithubPullTask` | GitHub 代码拉取 |

---

## MarketAtomTask - 插件执行

研发商店插件的执行流程：

```
┌─────────────────────────────────────────────────────────────┐
│                   插件执行流程                               │
├─────────────────────────────────────────────────────────────┤
│  1. 获取插件信息                                             │
│     atomApi.getAtomEnv(projectCode, atomCode, atomVersion)  │
│                                                             │
│  2. 准备执行环境                                             │
│     ├── 创建临时工作目录                                     │
│     ├── 下载/缓存插件包                                      │
│     └── 校验 SHA 完整性                                      │
│                                                             │
│  3. 准备输入参数                                             │
│     ├── 解析 input.json                                     │
│     ├── 替换变量和凭据                                       │
│     └── 写入 .sdk.json（SDK 环境信息）                       │
│                                                             │
│  4. 执行插件                                                 │
│     ├── Linux: ShellUtil.execute()                          │
│     └── Windows: BatScriptUtil.execute()                    │
│                                                             │
│  5. 处理输出                                                 │
│     ├── 读取 output.json                                    │
│     ├── 处理输出变量                                         │
│     ├── 归档制品（artifact 类型）                            │
│     └── 归档报告（report 类型）                              │
└─────────────────────────────────────────────────────────────┘
```

### 插件 SDK 环境

Worker 为插件提供的 SDK 环境信息（`.sdk.json`）：

```kotlin
data class SdkEnv(
    val buildType: BuildType,    // 构建类型
    val projectId: String,       // 项目ID（english_name）
    val agentId: String,         // 构建机ID
    val secretKey: String,       // 密钥
    val gateway: String,         // 网关地址
    val buildId: String,         // 构建ID
    val vmSeqId: String,         // 虚拟机序号
    val fileGateway: String,     // 文件网关
    val taskId: String,          // 任务ID
    val executeCount: Int        // 执行次数
)
```

### 插件输出处理

```kotlin
// output.json 格式
{
    "status": "success",  // success/failure
    "data": {
        "outVar1": {
            "type": "string",
            "value": "xxx"
        },
        "outVar2": {
            "type": "artifact",
            "value": ["file1.zip", "file2.tar.gz"]
        },
        "outVar3": {
            "type": "report",
            "label": "测试报告",
            "path": "reports/",
            "target": "index.html"
        }
    },
    "qualityData": { ... }  // 质量红线数据
}
```

---

## API 客户端

### AbstractBuildResourceApi

所有 API 客户端的基类，提供 HTTP 请求能力：

```kotlin
abstract class AbstractBuildResourceApi : WorkerRestApiSDK {
    // 请求重试机制
    protected fun requestForResponse(
        request: Request,
        retryCount: Int = DEFAULT_RETRY_TIME
    ): Response {
        // 支持 502/503/504 自动重试
        // 支持 DNS 错误、连接超时重试
    }
    
    // 构建请求头（包含认证信息）
    private fun getAllHeaders(headers: Map<String, String>): Map<String, String> {
        return buildArgs.plus(headers).plus(
            AUTH_HEADER_DEVOPS_BUILD_ID to buildInfo.buildId,
            AUTH_HEADER_DEVOPS_VM_SEQ_ID to buildInfo.vmSeqId
        )
    }
}
```

### 主要 API 客户端

| 类名 | 职责 |
|------|------|
| `BuildResourceApi` | 构建任务相关 API |
| `LogResourceApi` | 日志上报 API |
| `ArchiveResourceApi` | 制品归档 API |
| `AtomArchiveResourceApi` | 插件下载 API |
| `CredentialResourceApi` | 凭据获取 API |
| `QualityGatewayResourceApi` | 质量红线 API |

---

## 工具类

### ShellUtil - Shell 脚本执行

```kotlin
object ShellUtil {
    fun execute(
        buildId: String,
        script: String,
        dir: File,
        workspace: File,
        buildEnvs: List<BuildEnv>,
        runtimeVariables: Map<String, String>,
        errorMessage: String = "Fail to run the script"
    ): String {
        // 1. 创建临时脚本文件
        // 2. 设置环境变量
        // 3. 执行脚本
        // 4. 处理输出和错误
    }
}
```

### BatScriptUtil - Bat 脚本执行

```kotlin
object BatScriptUtil {
    fun execute(
        buildId: String,
        script: String,
        runtimeVariables: Map<String, String>,
        dir: File,
        workspace: File,
        errorMessage: String
    ): String {
        // Windows 批处理脚本执行
    }
}
```

### CredentialUtils - 凭据工具

```kotlin
object CredentialUtils {
    // 解析凭据占位符
    fun String.parseCredentialValue(
        context: Map<String, String>?,
        acrossProjectId: String?
    ): String {
        // 解析 ${{credentials.xxx}} 格式
    }
}
```

### WorkspaceUtils - 工作空间工具

```kotlin
object WorkspaceUtils {
    // 获取流水线工作空间
    fun getPipelineWorkspace(pipelineId: String, workspace: String): File
    
    // 获取日志目录
    fun getPipelineLogDir(pipelineId: String): File
    
    // 获取构建日志属性
    fun getBuildLogProperty(...): TaskBuildLogProperty
}
```

---

## 环境变量

### AgentEnv - 构建机环境

```kotlin
object AgentEnv {
    fun getProjectId(): String      // 项目ID
    fun getAgentId(): String        // 构建机ID
    fun getAgentSecretKey(): String // 密钥
    fun getGateway(): String        // 网关地址
    fun getOS(): OSType             // 操作系统类型
    fun getLocaleLanguage(): String // 语言设置
    fun getRuntimeJdkPath(): String // JDK 路径
}
```

### BuildEnv - 构建环境

```kotlin
object BuildEnv {
    fun getBuildType(): BuildType   // 获取构建类型
    fun isThirdParty(): Boolean     // 是否第三方构建机
}
```

---

## 数据模型

### BuildTask - 构建任务

```kotlin
data class BuildTask(
    val buildId: String,
    val vmSeqId: String,
    val status: BuildTaskStatus,    // DO/WAIT/END
    val taskId: String?,
    val elementId: String?,
    val elementName: String?,
    val type: String?,              // 任务类型
    val params: Map<String, String>?,
    val buildVariable: Map<String, String>?,
    val containerType: String?,
    val executeCount: Int?,
    val stepId: String?,
    val signToken: String?
)
```

### BuildVariables - 构建变量

```kotlin
data class BuildVariables(
    val buildId: String,
    val vmSeqId: String,
    val vmName: String,
    val projectId: String,          // 项目ID（english_name）
    val pipelineId: String,
    val variables: Map<String, String>,
    val variablesWithType: List<BuildParameters>,
    val buildEnvs: List<BuildEnv>,
    val containerId: String,
    val containerHashId: String?,
    val jobId: String?,
    val timeoutMills: Long
)
```

### BuildTaskResult - 任务结果

```kotlin
data class BuildTaskResult(
    val buildId: String,
    val vmSeqId: String,
    val taskId: String,
    val elementId: String,
    val success: Boolean,
    val buildVariable: Map<String, String>?,
    val errorType: String?,
    val errorCode: Int?,
    val message: String?,
    val type: String?,
    val monitorData: Map<String, Any>?,
    val platformCode: String?,
    val platformErrorCode: Int?
)
```

---

## 构建流程时序图

```
┌──────┐     ┌────────┐     ┌─────────┐     ┌────────┐
│Worker│     │Process │     │   Log   │     │  Store │
└──┬───┘     └───┬────┘     └────┬────┘     └───┬────┘
   │             │               │              │
   │ setStarted  │               │              │
   │────────────>│               │              │
   │ BuildVariables              │              │
   │<────────────│               │              │
   │             │               │              │
   │ claimTask   │               │              │
   │────────────>│               │              │
   │ BuildTask   │               │              │
   │<────────────│               │              │
   │             │               │              │
   │             │  addLogLine   │              │
   │             │──────────────>│              │
   │             │               │              │
   │             │               │  getAtomEnv  │
   │             │               │─────────────>│
   │             │               │   AtomEnv    │
   │             │               │<─────────────│
   │             │               │              │
   │ heartbeat   │               │              │
   │────────────>│               │              │
   │ HeartBeatInfo               │              │
   │<────────────│               │              │
   │             │               │              │
   │ completeTask│               │              │
   │────────────>│               │              │
   │             │               │              │
   │ endBuild    │               │              │
   │────────────>│               │              │
   └─────────────┴───────────────┴──────────────┘
```

---

## 与其他模块的关系

```
                    ┌─────────────┐
                    │   Process   │
                    │  (流水线引擎) │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │            │            │
              ▼            ▼            ▼
        ┌─────────┐  ┌─────────┐  ┌─────────┐
        │Dispatch │  │   Log   │  │  Store  │
        │(调度服务)│  │(日志服务)│  │(研发商店)│
        └────┬────┘  └────┬────┘  └────┬────┘
             │            │            │
             └────────────┼────────────┘
                          │
                    ┌─────▼─────┐
                    │  Worker   │
                    │ (构建执行器)│
                    └─────┬─────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
         ▼                ▼                ▼
   ┌──────────┐    ┌──────────┐    ┌──────────┐
   │Artifactory│    │  Ticket  │    │Repository│
   │ (制品库)  │    │ (凭据)   │    │ (代码库) │
   └──────────┘    └──────────┘    └──────────┘
```

### 依赖关系说明

| 依赖模块 | 交互方式 | 用途 |
|----------|----------|------|
| Process | HTTP API | 任务领取、状态上报、心跳 |
| Log | HTTP API | 日志上报 |
| Store | HTTP API | 插件信息获取、插件下载 |
| Artifactory | HTTP API | 制品上传下载 |
| Ticket | HTTP API | 凭据获取 |
| Repository | HTTP API | 代码库信息 |

---

## 开发规范

### 新增任务类型

1. 创建任务类继承 `ITask`：

```kotlin
@TaskClassType(classTypes = ["myTaskType"], priority = 1)
class MyTask : ITask() {
    override fun execute(
        buildTask: BuildTask,
        buildVariables: BuildVariables,
        workspace: File
    ) {
        // 实现任务逻辑
        LoggerService.addNormalLine("执行任务...")
        
        // 输出变量
        addEnv(mapOf("OUTPUT_VAR" to "value"))
    }
}
```

2. 放置在 `com.tencent.devops.plugin.worker.task` 包下，自动注册

### 日志输出规范

```kotlin
// 普通日志
LoggerService.addNormalLine("正常信息")

// 警告日志（黄色）
LoggerService.addWarnLine("警告信息")

// 错误日志（红色）
LoggerService.addErrorLine("错误信息")

// 调试日志（灰色）
LoggerService.addDebugLine("调试信息")

// 折叠日志
LoggerService.addFoldStartLine("[安装依赖]")
// ... 详细日志
LoggerService.addFoldEndLine("-----")
```

### 错误处理

```kotlin
throw TaskExecuteException(
    errorMsg = "任务执行失败",
    errorType = ErrorType.USER,        // USER/SYSTEM/THIRD_PARTY/PLUGIN
    errorCode = ErrorCode.USER_TASK_OPERATE_FAIL
)
```

### 敏感信息处理

```kotlin
// 自动过滤敏感信息
SensitiveValueService.addSensitiveValue("password123")

// 日志中会自动替换为 ******
LoggerService.addNormalLine("密码是: password123")
// 输出: 密码是: ******
```

---

## 常见问题

### Q1: Worker 如何与服务端通信？

Worker 通过 HTTP API 与服务端通信，所有请求都带有认证头：
- `X-DEVOPS-BUILD-TYPE`: 构建类型
- `X-DEVOPS-PROJECT-ID`: 项目ID
- `X-DEVOPS-AGENT-ID`: 构建机ID
- `X-DEVOPS-AGENT-SECRET-KEY`: 密钥
- `X-DEVOPS-BUILD-ID`: 构建ID
- `X-DEVOPS-VM-SEQ-ID`: 虚拟机序号

### Q2: 插件执行失败如何排查？

1. 检查 `output.json` 输出
2. 查看 Worker 日志（构建机上的 `logs/` 目录）
3. 检查插件 SHA 校验是否通过
4. 确认插件运行环境（Python/Node.js 版本等）

### Q3: 心跳失败会怎样？

连续 12 次心跳失败后，Worker 会自动退出（`exitProcess(-1)`）。服务端会将构建标记为异常终止。

### Q4: 如何调试 Worker？

1. 设置环境变量 `DEVOPS_AGENT_LOG_DEBUG=true` 开启调试日志
2. 使用 `LoggerService.addDebugLine()` 输出调试信息
3. 检查构建机上的日志文件

### Q5: 插件包缓存机制？

- 第三方构建机：缓存在 `{user.dir}/store/cache/plugins/`
- 公共构建机：缓存在 `{workspace}/../cache/store/cache/plugins/`
- 缓存 Key：`{atomCode}-{version}-{fileName}`
- 测试版本插件不缓存

---

## 国际化

Worker 模块的国际化文件位于：
- `support-files/i18n/worker/message_zh_CN.properties`
- `support-files/i18n/worker/message_en_US.properties`

常量定义在 `WorkerMessageCode.kt`：

```kotlin
object WorkerMessageCode {
    const val BK_PREPARE_TO_BUILD = "bkPrepareToRunBuild"
    const val PARAMETER_ERROR = "parameterError"
    const val UNKNOWN_ERROR = "unknownError"
    const val AGENT_DNS_ERROR = "agentDnsError"
    const val AGENT_NETWORK_TIMEOUT = "agentNetworkTimeout"
    // ...
}
```
