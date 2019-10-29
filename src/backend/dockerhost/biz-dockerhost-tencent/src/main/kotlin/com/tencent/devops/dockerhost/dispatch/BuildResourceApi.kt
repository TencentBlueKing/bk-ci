package com.tencent.devops.dockerhost.dispatch

import com.fasterxml.jackson.module.kotlin.readValue
import com.tencent.devops.common.api.exception.RemoteServiceException
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.OkhttpUtils
import com.tencent.devops.common.pipeline.enums.BuildStatus
import org.slf4j.LoggerFactory

class BuildResourceApi : AbstractBuildResourceApi() {
    private val logger = LoggerFactory.getLogger(BuildResourceApi::class.java)

    fun dockerStartFail(
        projectId: String,
        pipelineId: String,
        buildId: String,
        vmSeqId: String,
        status: BuildStatus
    ): Result<Boolean>? {
        val path =
            "/process/api/service/builds/$projectId/$pipelineId/$buildId/vmStatus?vmSeqId=$vmSeqId&status=${status.name}"
        val request = buildPut(path)
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("DockerHostBuildResourceApi $path fail. $responseContent")
                throw RemoteServiceException("BuildResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }

    fun reportContainerId(buildId: String, vmSeqId: String, containerId: String, hostTag: String): Result<Boolean>? {
        val path =
            "/dispatch/api/dockerhost/containerId?buildId=$buildId&vmSeqId=$vmSeqId&containerId=$containerId&hostTag=$hostTag"
        val request = buildPost(path)
        OkhttpUtils.doHttp(request).use { response ->
            val responseContent = response.body()!!.string()
            if (!response.isSuccessful) {
                logger.error("AgentThirdPartyAgentResourceApi $path fail. $responseContent")
                throw RemoteServiceException("AgentThirdPartyAgentResourceApi $path fail")
            }
            return objectMapper.readValue(responseContent)
        }
    }
}
