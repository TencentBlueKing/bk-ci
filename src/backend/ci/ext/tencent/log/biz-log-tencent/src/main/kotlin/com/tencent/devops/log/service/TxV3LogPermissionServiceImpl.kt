package com.tencent.devops.log.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.process.api.service.ServicePipelineResource
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class TxV3LogPermissionServiceImpl @Autowired constructor(
    val client: Client,
    private val tokenCheckService: ClientTokenService
) : LogPermissionService {
    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        val action = TActionUtils.buildAction(permission ?: AuthPermission.VIEW, AuthResourceType.PIPELINE_DEFAULT)
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
            userId = userId,
            token = tokenCheckService.getSystemToken(null) ?: "",
            action = action,
            projectCode = projectCode,
            resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
            resourceCode = getPipelineId(pipelineId, projectCode),
            relationResourceType = null
        ).data ?: false
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
        private val logger = LoggerFactory.getLogger(TxV3LogPermissionServiceImpl::class.java)
    }
}
