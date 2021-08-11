package com.tencent.devops.artifactory.service.permission

import com.tencent.devops.artifactory.service.PipelineService
import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.auth.api.service.ServiceProjectAuthResource
import com.tencent.devops.common.api.exception.PermissionForbiddenException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxV3ArtPipelineServiceImpl @Autowired constructor(
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
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                userId = userId,
                token = tokenCheckService.getSystemToken(null) ?: "",
                action = TActionUtils.buildAction(
                    authPermission = permission ?: AuthPermission.VIEW,
                    authResourceType = AuthResourceType.PIPELINE_DEFAULT),
                projectCode = projectId,
                resourceCode = getPipelineId(pipelineId!!, projectId),
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                relationResourceType = null
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
            channelCode = ChannelCode.BS,
            checkPermission = false
        ).data?.records

        return if (!pipelineInfos.isNullOrEmpty()) {
            pipelineInfos.map { it.pipelineId }
        } else {
            emptyList()
        }
    }

    private fun getPipelineId(pipelineId: String, projectCode: String): String {
        val pipelineIdInfo = client.get(ServicePipelineResource::class)
            .getPipelineId(projectCode, pipelineId).data
        if (pipelineIdInfo == null) {
            logger.warn("$pipelineId find autoId is empty")
            return pipelineId
        }
        return pipelineIdInfo.id.toString()
    }

    companion object {
        val logger = LoggerFactory.getLogger(TxV3ArtPipelineServiceImpl::class.java)
    }
}
