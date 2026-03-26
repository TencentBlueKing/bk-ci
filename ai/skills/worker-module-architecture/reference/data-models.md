# Worker 数据模型参考

## BuildTask - 构建任务

```kotlin
data class BuildTask(
    val buildId: String,
    val vmSeqId: String,
    val status: BuildTaskStatus,    // DO/WAIT/END
    val taskId: String?,
    val elementId: String?,
    val elementName: String?,
    val type: String?,              // 任务类型（对应 TaskFactory 注册的 classType）
    val params: Map<String, String>?,
    val buildVariable: Map<String, String>?,
    val containerType: String?,
    val executeCount: Int?,
    val stepId: String?,
    val signToken: String?
)
```

## BuildVariables - 构建变量

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

## BuildTaskResult - 任务结果

```kotlin
data class BuildTaskResult(
    val buildId: String,
    val vmSeqId: String,
    val taskId: String,
    val elementId: String,
    val success: Boolean,
    val buildVariable: Map<String, String>?,
    val errorType: String?,         // USER/SYSTEM/THIRD_PARTY/PLUGIN
    val errorCode: Int?,
    val message: String?,
    val type: String?,
    val monitorData: Map<String, Any>?,
    val platformCode: String?,
    val platformErrorCode: Int?
)
```

## SdkEnv - 插件 SDK 环境（.sdk.json）

```kotlin
data class SdkEnv(
    val buildType: BuildType,
    val projectId: String,
    val agentId: String,
    val secretKey: String,
    val gateway: String,
    val buildId: String,
    val vmSeqId: String,
    val fileGateway: String,
    val taskId: String,
    val executeCount: Int
)
```

## 插件输出格式（output.json）

```json
{
    "status": "success",
    "data": {
        "outVar1": { "type": "string", "value": "xxx" },
        "outVar2": { "type": "artifact", "value": ["file1.zip", "file2.tar.gz"] },
        "outVar3": { "type": "report", "label": "测试报告", "path": "reports/", "target": "index.html" }
    },
    "qualityData": { }
}
```

## AgentEnv / BuildEnv

```kotlin
object AgentEnv {
    fun getProjectId(): String
    fun getAgentId(): String
    fun getAgentSecretKey(): String
    fun getGateway(): String
    fun getOS(): OSType
    fun getLocaleLanguage(): String
    fun getRuntimeJdkPath(): String
}

object BuildEnv {
    fun getBuildType(): BuildType
    fun isThirdParty(): Boolean
}
```
