package com.tencent.devops.artifactory.service.permission

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelinePermissionResource
import com.tencent.devops.process.api.service.ServicePipelineResource

class RbacArtPipelineServiceImpl(
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
        return if (pipelineId.isNullOrEmpty()) {
            client.get(ServiceProjectAuthResource::class).isProjectUser(
                userId = userId,
                token = tokenCheckService.getSystemToken(null) ?: "",
                projectCode = projectId
            ).data ?: false
        } else {
            client.get(ServicePipelinePermissionResource::class).checkPipelinePermission(
                userId = userId,
                projectId = projectId,
                pipelineId = pipelineId,
                permission = permission ?: AuthPermission.VIEW
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
            pageSize = 5000,
            channelCode = ChannelCode.BS,
            checkPermission = false
        ).data?.records

        return if (!pipelineInfos.isNullOrEmpty()) {
            pipelineInfos.map { it.pipelineId }
        } else {
            emptyList()
        }
    }
}
