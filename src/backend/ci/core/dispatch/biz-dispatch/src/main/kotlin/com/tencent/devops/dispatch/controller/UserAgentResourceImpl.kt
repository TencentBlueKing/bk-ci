package com.tencent.devops.dispatch.controller

import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.dispatch.api.UserAgentResource
import com.tencent.devops.dispatch.exception.ErrorCodeEnum
import com.tencent.devops.dispatch.pojo.enums.PipelineTaskStatus
import com.tencent.devops.dispatch.pojo.thirdpartyagent.AgentPipelineContainerBuild
import com.tencent.devops.dispatch.pojo.thirdpartyagent.JobIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.PipelineIdAndName
import com.tencent.devops.dispatch.pojo.thirdpartyagent.TPAPipelineBuildCountResp
import com.tencent.devops.dispatch.service.ThirdPartyAgentService
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.AllCreateNodeEnv
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserAgentResourceImpl @Autowired constructor(
    private val client: Client,
    private val thirdPartyAgentService: ThirdPartyAgentService,
    private val checkTokenService: ClientTokenService
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
        creator: String?,
        taskStatus: PipelineTaskStatus?
    ): Result<TPAPipelineBuildCountResp> {
        val envRId = AllCreateNodeEnv.hashIdToId(envId)
        checkEnvOrAgentPermission(userId, projectId, agentId, envRId)
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
                creator = creator,
                status = taskStatus
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
        val envRId = AllCreateNodeEnv.hashIdToId(envId)
        checkEnvOrAgentPermission(userId, projectId, agentId, envRId)
        return Result(thirdPartyAgentService.fetchPipelineIdAndName(projectId, agentId, envRId, pipelineName))
    }

    override fun listAgentPipelineJobsByJobName(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        jobName: String?
    ): Result<List<JobIdAndName>> {
        val envRId = AllCreateNodeEnv.hashIdToId(envId)
        checkEnvOrAgentPermission(userId, projectId, agentId, envRId)
        return Result(thirdPartyAgentService.fetchJobIdAndName(projectId, agentId, envRId, jobName))
    }

    override fun listAgentPipelineJobsByCreator(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        creator: String?
    ): Result<List<String>> {
        val envRId = AllCreateNodeEnv.hashIdToId(envId)
        checkEnvOrAgentPermission(userId, projectId, agentId, envRId)
        return Result(thirdPartyAgentService.fetchCreator(projectId, agentId, envRId, creator))
    }

    override fun fetchAgentBuildsByJob(
        userId: String,
        projectId: String,
        agentId: String?,
        envId: String?,
        pipelineId: String,
        jobId: String,
        page: Int?,
        pageSize: Int?
    ): Result<Page<AgentPipelineContainerBuild>> {
        val envRId = AllCreateNodeEnv.hashIdToId(envId)
        checkEnvOrAgentPermission(userId, projectId, agentId, envRId)
        return Result(
            thirdPartyAgentService.fetchAgentBuildsByJob(
                userId = userId,
                projectId = projectId,
                agentId = agentId,
                envId = envRId,
                pipelineId = pipelineId,
                jobId = jobId,
                page = page,
                pageSize = pageSize
            )
        )
    }

    private fun checkEnvOrAgentPermission(
        userId: String,
        projectId: String,
        agentHashId: String?,
        envId: Long?
    ) {
        if (agentHashId != null) {
            val hasPermission = client.get(ServiceNodeResource::class).checkAgentPermission(
                userId = userId,
                projectId = projectId,
                agentHashId = agentHashId,
                permission = AuthPermission.VIEW
            ).data
            if (hasPermission == null || !hasPermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No node permission.",
                    params = arrayOf(agentHashId)
                )
            }
        }
        if (envId != null) {
            // 所有创作环境检验的是管理员权限
            val hasPermission = if (AllCreateNodeEnv.hasId(envId)) {
                client.get(ServiceProjectAuthResource::class).checkProjectManager(
                    token = checkTokenService.getSystemToken(),
                    userId = userId,
                    projectCode = projectId
                ).data
            } else {
                client.get(ServiceEnvironmentResource::class).checkEnvPermission(
                    userId = userId,
                    projectId = projectId,
                    envId = envId,
                    permission = AuthPermission.VIEW
                ).data
            }
            if (hasPermission == null || !hasPermission) {
                throw ErrorCodeException(
                    errorCode = "${ErrorCodeEnum.NO_NODE_PERMISSION.errorCode}",
                    defaultMessage = "No env permission.",
                    params = arrayOf(envId.toString())
                )
            }
        }
    }
}
