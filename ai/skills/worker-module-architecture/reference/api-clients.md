# Worker API 客户端参考

## AbstractBuildResourceApi

所有 API 客户端的基类（`worker-api-sdk`）：
- 自动重试 502/503/504、DNS 错误、连接超时
- 认证头自动注入：`X-DEVOPS-BUILD-ID`、`X-DEVOPS-VM-SEQ-ID` 等

## 认证头

| Header | 说明 |
|--------|------|
| `X-DEVOPS-BUILD-TYPE` | 构建类型 |
| `X-DEVOPS-PROJECT-ID` | 项目ID |
| `X-DEVOPS-AGENT-ID` | 构建机ID |
| `X-DEVOPS-AGENT-SECRET-KEY` | 密钥 |
| `X-DEVOPS-BUILD-ID` | 构建ID |
| `X-DEVOPS-VM-SEQ-ID` | 虚拟机序号 |

## API 客户端列表

| 类名 | 路径 | 职责 |
|------|------|------|
| `BuildResourceApi` | `worker-api-sdk/.../process/` | 构建任务相关（领取、完成、心跳） |
| `LogResourceApi` | `worker-api-sdk/.../log/` | 日志上报 |
| `ArchiveResourceApi` | `worker-api-sdk/.../archive/` | 制品归档上传下载 |
| `AtomArchiveResourceApi` | `worker-api-sdk/.../atom/` | 插件下载 |
| `CredentialResourceApi` | `worker-api-sdk/.../` | 凭据获取 |
| `QualityGatewayResourceApi` | `worker-api-sdk/.../` | 质量红线 |

## EngineService 方法

```kotlin
object EngineService {
    fun setStarted(): BuildVariables          // 上报启动状态
    fun claimTask(): BuildTask                // 领取任务
    fun completeTask(taskResult: BuildTaskResult)  // 完成任务
    fun endBuild(variables: Map<String, String>, result: BuildJobResult)  // 结束构建
    fun heartbeat(executeCount: Int, jobHeartbeatRequest: JobHeartbeatRequest): HeartBeatInfo
    fun timeout()                             // 超时上报
    fun submitError(errorInfo: ErrorInfo)      // 错误上报
}
```
