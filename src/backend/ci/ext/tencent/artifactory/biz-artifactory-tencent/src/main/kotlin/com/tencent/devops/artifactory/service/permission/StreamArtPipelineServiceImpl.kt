package com.tencent.devops.artifactory.service.permission

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class StreamArtPipelineServiceImpl @Autowired constructor(
    private val client: Client,
    private val tokenCheckService: ClientTokenService
) : PipelineService(client) {
    override fun validatePermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?,
        message: String?
    ) {
        if (!hasPermission(userId, projectId, pipelineId, permission)) {
            throw PermissionForbiddenException(message)
        }
    }

    override fun hasPermission(
        userId: String,
        projectId: String,
        pipelineId: String?,
        permission: AuthPermission?
    ): Boolean {
        logger.info("StreamLocalPipelineServiceImpl |$userId|$projectId|$pipelineId|$permission|")
        return if (pipelineId != null) {
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
                userId = userId,
                token = tokenCheckService.getSystemToken(null) ?: "",
                action = permission?.value ?: "",
                projectCode = projectId,
                resourceCode = AuthResourceType.PIPELINE_DEFAULT.value
            ).data ?: false
        } else {
            client.get(ServiceProjectAuthResource::class).isProjectUser(
                token = tokenCheckService.getSystemToken(null) ?: "",
                userId = userId,
                projectCode = projectId
            ).data ?: false
        }
    }

    override fun filterPipeline(user: String, projectId: String): List<String> {
        if (!hasPermission(user, projectId, null, null)) {
            return emptyList()
        }
        val pipelineInfos = client.get(ServicePipelineResource::class).list(
            userId = user,
            projectId = projectId,
            page = 0,
            pageSize = 1000,
            channelCode = ChannelCode.GIT,
            checkPermission = false
        ).data?.records

        return if (!pipelineInfos.isNullOrEmpty()) {
            pipelineInfos.map { it.pipelineId }
        } else {
            emptyList()
        }
    }

    companion object {
        val logger = LoggerFactory.getLogger(StreamArtPipelineServiceImpl::class.java)
    }
}
