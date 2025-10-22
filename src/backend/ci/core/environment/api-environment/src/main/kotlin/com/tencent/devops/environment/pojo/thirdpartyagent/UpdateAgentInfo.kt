package com.tencent.devops.environment.pojo.thirdpartyagent

import com.tencent.devops.environment.pojo.EnvVar
import io.swagger.v3.oas.annotations.media.Schema

data class UpdateAgentInfo(
    @get:Schema(title = "Agent Hash ID", required = true)
    val agentHashId: String?,
    @get:Schema(title = "Node Hash ID", required = true)
    val nodeHashId: String?,
    @get:Schema(title = "节点名称", required = true)
    val displayName: String?,
    @get:Schema(title = "最大构建并发数", required = true)
    val parallelTaskCount: Int?,
    @get:Schema(title = "docker构建机通道数量", required = true)
    val dockerParallelTaskCount: Int?,
    @get:Schema(title = "Agent配置的环境变量,修改会直接覆盖", required = true)
    val envs: List<EnvVar>?
)
