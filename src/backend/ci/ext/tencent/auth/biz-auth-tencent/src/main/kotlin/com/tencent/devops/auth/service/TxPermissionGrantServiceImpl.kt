package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.pojo.dto.GrantInstanceDTO
import com.tencent.devops.auth.service.iam.impl.AbsPermissionGrantServiceImpl
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import com.tencent.devops.common.client.Client
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxPermissionGrantServiceImpl @Autowired constructor(
    override val grantServiceImpl: GrantServiceImpl,
    override val iamConfiguration: IamConfiguration,
    override val client: Client,
    val authPipelineIdService: AuthPipelineIdService
) : AbsPermissionGrantServiceImpl(grantServiceImpl, iamConfiguration, client) {

    override fun grantInstancePermission(projectId: String, grantInfo: GrantInstanceDTO): Boolean {
        // 如果校验的资源为pipeline,需要兼容传pipelineId的情况
        val pipelineInfo = authPipelineIdService.getPipelineInfo(
            resourceType = grantInfo.resourceType,
            resourceCode = grantInfo.resourceCode,
            resourceName = grantInfo.resourceName)
        val newAction = TActionUtils.buildAction(
            AuthPermission.get(grantInfo.permission),
            AuthResourceType.get(grantInfo.resourceType))
        val newGrantInfo = GrantInstanceDTO(
            permission = newAction,
            resourceType = grantInfo.resourceType,
            resourceCode = pipelineInfo.first,
            createUser = grantInfo.createUser,
            resourceName = pipelineInfo.second
        )
        return super.grantInstancePermission(projectId, newGrantInfo)
    }
}
