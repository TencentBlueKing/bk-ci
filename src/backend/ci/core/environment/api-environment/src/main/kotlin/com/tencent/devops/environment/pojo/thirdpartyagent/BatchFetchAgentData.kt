package com.tencent.devops.environment.pojo.thirdpartyagent

import com.tencent.devops.environment.pojo.EnvVar
import io.swagger.v3.oas.annotations.media.Schema

@Schema(title = "批量查询Agent数据")
open class BatchFetchAgentData(
    @get:Schema(title = "Node Hash ID列表,和 agentHashId 选其一即可", required = false)
    open val nodeHashIds: Set<String>?,
    @get:Schema(title = "agent Hash ID列表,和 nodeHashId 选其一即可", required = false)
    open val agentHashIds: Set<String>?
)

@Schema(title = "批量修改Agent环境变量数据")
data class BatchUpdateAgentEnvVar(
    @get:Schema(title = "Node Hash ID列表,和 agentHashId 选其一即可", required = false)
    override val nodeHashIds: Set<String>?,
    @get:Schema(title = "agent Hash ID列表,和 nodeHashId 选其一即可", required = false)
    override val agentHashIds: Set<String>?,
    @get:Schema(title = "修改方式,支持3种输入(ADD,REMOVE,UPDATE),默认为UPDATE", required = false)
    val type: ThirdPartAgentUpdateType?,
    @get:Schema(title = "环境变量", required = true)
    val envVars: List<EnvVar>
) : BatchFetchAgentData(nodeHashIds, agentHashIds)