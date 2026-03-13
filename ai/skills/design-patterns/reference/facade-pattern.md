# 门面模式（Facade Pattern）

## ParamFacadeService — 参数门面服务

**位置**：`process/biz-process/src/main/kotlin/com/tencent/devops/process/service/ParamFacadeService.kt`

隐藏多个子系统的复杂交互，提供简单统一的接口，内部处理优先级和合并逻辑。

```kotlin
@Service
class ParamFacadeService(
    private val buildVariableService: BuildVariableService,
    private val pipelineContextService: PipelineContextService,
    private val secretService: SecretService,
    private val credentialService: CredentialService
) {
    fun getBuildParameters(
        projectId: String,
        pipelineId: String,
        buildId: String
    ): Map<String, String> {
        val contextVars = pipelineContextService.getAllVariables(projectId, pipelineId, buildId)
        val buildVars = buildVariableService.getAllVariable(projectId, pipelineId, buildId)
        val credentialVars = credentialService.getCredentialVariables(projectId, buildId)
        val secretVars = secretService.getSecretVariables(projectId, buildId)

        // 优先级：secret > credential > build > context
        return contextVars + buildVars + credentialVars + secretVars
    }
}
```
