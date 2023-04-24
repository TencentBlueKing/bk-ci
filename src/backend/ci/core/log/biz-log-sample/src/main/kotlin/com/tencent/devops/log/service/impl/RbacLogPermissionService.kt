package com.tencent.devops.log.service.impl

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.api.exception.ErrorCodeException
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.RbacAuthUtils
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.client.ClientTokenService
import com.tencent.devops.common.pipeline.enums.ChannelCode
import com.tencent.devops.log.service.LogPermissionService
import com.tencent.devops.process.api.service.ServicePipelineResource
import com.tencent.devops.process.constant.ProcessMessageCode
import org.springframework.beans.factory.annotation.Autowired

class RbacLogPermissionService @Autowired constructor(
    val client: Client,
    private val tokenCheckService: ClientTokenService
) : LogPermissionService {
    override fun verifyUserLogPermission(
        projectCode: String,
        pipelineId: String,
        userId: String,
        permission: AuthPermission?
    ): Boolean {
        val pipelineInfo =
            client.get(ServicePipelineResource::class).getPipelineInfo(
                projectId = projectCode,
                pipelineId = pipelineId,
                channelCode = null
            ).data ?: throw ErrorCodeException(
                errorCode = ProcessMessageCode.ERROR_PIPELINE_NOT_EXISTS,
                defaultMessage = "流水线不存在"
            )
        // 兼容CodeCC场景，CodeCC创建的流水线未向权限中心注册，调鉴权接口会报错。
        return pipelineInfo.channelCode != ChannelCode.BS ||
            client.get(ServicePermissionAuthResource::class).validateUserResourcePermissionByRelation(
                userId = userId,
                token = tokenCheckService.getSystemToken(null) ?: "",
                action = RbacAuthUtils.buildAction(
                    permission ?: AuthPermission.VIEW, AuthResourceType.PIPELINE_DEFAULT
                ),
                projectCode = projectCode,
                resourceType = AuthResourceType.PIPELINE_DEFAULT.value,
                resourceCode = pipelineId,
                relationResourceType = null
            ).data ?: false
    }
}
