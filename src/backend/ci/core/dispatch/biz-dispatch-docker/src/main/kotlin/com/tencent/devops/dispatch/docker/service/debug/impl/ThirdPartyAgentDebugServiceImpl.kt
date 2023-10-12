package com.tencent.devops.dispatch.docker.service.debug.impl

import com.tencent.devops.common.client.Client
import com.tencent.devops.common.service.config.CommonConfig
import com.tencent.devops.dispatch.api.ServiceAgentResource
import com.tencent.devops.dispatch.docker.service.debug.ExtDebugService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service("THIRD_PARTY_AGENT_DOCKER_BUILD_CLUSTER_RESULT")
class ThirdPartyAgentDebugServiceImpl @Autowired constructor(
    private val client: Client,
    private val commonConfig: CommonConfig
) : ExtDebugService {
    override fun startDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String
    ): String {
        var url = client.get(ServiceAgentResource::class).getDockerDebugUrl(
            userId = userId,
            projectId = projectId,
            pipelineId = pipelineId,
            buildId = buildId,
            vmSeqId = vmSeqId
        ).data!!

        url = url.removePrefix("ws://")
        val paramStart = url.indexOf("start_exec")
        val ipAndPort = url.substring(0, paramStart).removeSuffix("/").split(":")
        val suffix = url.substring(paramStart)
        url = "wss://${
            commonConfig.devopsBuildGateway?.removePrefix("https://")?.removePrefix("http://")
        }/agent-console/$suffix&ip=${ipAndPort[0]}&port=${ipAndPort[1]}"

        return url
    }

    override fun stopDebug(
        userId: String,
        projectId: String,
        pipelineId: String,
        vmSeqId: String,
        containerName: String
    ): Boolean {
        // 目前GoAgent客户端只要断开链接就会删掉容器，所以不用实现具体的stop
        return true
    }

    override fun getWebsocketUrl(
        userId: String,
        projectId: String,
        pipelineId: String,
        buildId: String?,
        vmSeqId: String,
        containerId: String
    ): String? {
        return null
    }
}
