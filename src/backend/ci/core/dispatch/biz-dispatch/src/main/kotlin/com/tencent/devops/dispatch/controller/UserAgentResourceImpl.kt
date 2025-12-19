package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserAgentResource
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAgentResourceImpl @Autowired constructor(
    private val thirdPartyAgentService: ThirdPartyAgentService
) : UserAgentResource {
    override fun listAgentPipelineJobs(
        projectId: String,
        agentId: String?,
        envId: String?,
        page: Int?,
        pageSize: Int?,
        startTime: Long?,
        endTime: Long?,
        pipelineId: String?,
        jobId: String?,
        creator: String?
    ): Result<TPAPipelineBuildCountResp> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        return Result(
            thirdPartyAgentService.fetchBuildPipeline(
                projectId = projectId,
                agentId = agentId,
                envId = envRId,
                page = page,
                pageSize = pageSize,
                startTime = startTime,
                endTime = endTime,
                pipelineId = pipelineId,
                jobId = jobId,
                creator = creator
            )
        )
    }

    override fun listAgentPipelineJobsByPipelineName(
        projectId: String,
        agentId: String?,
        envId: String?,
        pipelineName: String?
    ): Result<List<PipelineIdAndName>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        return Result(thirdPartyAgentService.fetchPipelineIdAndName(projectId, agentId, envRId, pipelineName))
    }

    override fun listAgentPipelineJobsByJobName(
        projectId: String,
        agentId: String?,
        envId: String?,
        jobName: String?
    ): Result<List<JobIdAndName>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        return Result(thirdPartyAgentService.fetchJobIdAndName(projectId, agentId, envRId, jobName))
    }

    override fun listAgentPipelineJobsByCreator(
        projectId: String,
        agentId: String?,
        envId: String?,
        creator: String?
    ): Result<List<String>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        return Result(thirdPartyAgentService.fetchCreator(projectId, agentId, envRId, creator))
    }
}