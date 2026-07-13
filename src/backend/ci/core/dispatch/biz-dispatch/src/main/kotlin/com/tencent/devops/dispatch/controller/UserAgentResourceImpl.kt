package com.tencent.devops.dispatch.controller

import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserAgentResource
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineContainerBuild
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.AllCreateNodeEnv
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAgentResourceImpl @Autowired constructor(
    private val client: Client,
    private val thirdPartyAgentService: ThirdPartyAgentService
) : UserAgentResource {
    override fun listAgentPipelineJobs(
        userId: String,
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
        val envRId = envId?.let {
            if (it == AllCreateNodeEnv.hashId()) {
                AllCreateNodeEnv.ENV_ID
            } else {
                HashUtil.decodeIdToLong(it)
            }
        }
        if (agentId != null) {
            // TODO: 未来补齐env
            val hasNodePermission = client.get(ServiceNodeResource::class).checkAgentPermission(
                userId = userId,
                projectId = projectId,
                agentHashId = agentId,
                permission = AuthPermission.VIEW
            ).data
            if (hasNodePermission == null || !hasNodePermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No node permission.",
                    params = arrayOf(agentId)
                )
            }
        }
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
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        pipelineName: String?
    ): Result<List<PipelineIdAndName>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        if (agentId != null) {
            // TODO: 未来补齐env
            val hasNodePermission = client.get(ServiceNodeResource::class).checkAgentPermission(
                userId = userId,
                projectId = projectId,
                agentHashId = agentId,
                permission = AuthPermission.VIEW
            ).data
            if (hasNodePermission == null || !hasNodePermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No node permission.",
                    params = arrayOf(agentId)
                )
            }
        }
        return Result(thirdPartyAgentService.fetchPipelineIdAndName(projectId, agentId, envRId, pipelineName))
    }

    override fun listAgentPipelineJobsByJobName(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        jobName: String?
    ): Result<List<JobIdAndName>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        if (agentId != null) {
            // TODO: 未来补齐env
            val hasNodePermission = client.get(ServiceNodeResource::class).checkAgentPermission(
                userId = userId,
                projectId = projectId,
                agentHashId = agentId,
                permission = AuthPermission.VIEW
            ).data
            if (hasNodePermission == null || !hasNodePermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No node permission.",
                    params = arrayOf(agentId)
                )
            }
        }
        return Result(thirdPartyAgentService.fetchJobIdAndName(projectId, agentId, envRId, jobName))
    }

    override fun listAgentPipelineJobsByCreator(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        creator: String?
    ): Result<List<String>> {
        val envRId = if (envId.isNullOrBlank()) null else HashUtil.decodeIdToLong(envId)
        if (agentId != null) {
            // TODO: 未来补齐env
            val hasNodePermission = client.get(ServiceNodeResource::class).checkAgentPermission(
                userId = userId,
                projectId = projectId,
                agentHashId = agentId,
                permission = AuthPermission.VIEW
            ).data
            if (hasNodePermission == null || !hasNodePermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No node permission.",
                    params = arrayOf(agentId)
                )
            }
        }
        return Result(thirdPartyAgentService.fetchCreator(projectId, agentId, envRId, creator))
    }

    override fun fetchAgentBuildsByJob(
        userId: String,
        projectId: String,
        agentId: String,
        pipelineId: String,
        jobId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentPipelineContainerBuild>> {
        val hasNodePermission = client.get(ServiceNodeResource::class).checkAgentPermission(
            userId = userId,
            projectId = projectId,
            agentHashId = agentId,
            permission = AuthPermission.VIEW
        ).data
        if (hasNodePermission == null || !hasNodePermission) {
            throw ErrorCodeException(
                errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                defaultMessage = "No node permission.",
                params = arrayOf(agentId)
            )
        }
        return Result(
            thirdPartyAgentService.fetchAgentBuildsByJob(
                userId = userId,
                projectId = projectId,
                agentId = agentId,
                pipelineId = pipelineId,
                jobId = jobId,
                page = page,
                pageSize = pageSize
            )
        )
    }
}