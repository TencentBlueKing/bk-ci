package com.tencent.devops.environment.resources

import com.tencent.devops.common.api.exception.InvalidParamException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.pipeline.type.agent.AgentDockerConfig
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceDockerConfigResource
import com.tencent.devops.environment.dao.thirdpartyagent.ThirdPartyAgentDao
import com.tencent.devops.environment.service.DockerConfigService
import org.jooq.DSLContext
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceDockerConfigResourceImpl @Autowired constructor(
    private val context: DSLContext,
    private val agentDao: ThirdPartyAgentDao,
    private val service: DockerConfigService
) : ServiceDockerConfigResource {
    override fun get(agentId: String): Result<AgentDockerConfig> {
        val agent = agentDao.getAgent(context, HashUtil.decodeIdToLong(agentId))
            ?: throw InvalidParamException("Agent not found")
        val nodeId = agent.nodeId
        val config = if (nodeId == null || !agent.os.equals("LINUX", true)) {
            AgentDockerConfig()
        } else {
            service.forAgents(agent.projectId, listOf(nodeId))[nodeId] ?: AgentDockerConfig()
        }
        return Result(config)
    }
}
