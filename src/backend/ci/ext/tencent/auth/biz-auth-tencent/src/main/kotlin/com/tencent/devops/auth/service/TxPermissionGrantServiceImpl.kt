package com.tencent.devops.auth.service

import com.tencent.bk.sdk.iam.config.IamConfiguration
import com.tencent.bk.sdk.iam.service.impl.GrantServiceImpl
import com.tencent.devops.auth.service.iam.impl.AbsPermissionGrantServiceImpl
import com.tencent.devops.common.auth.api.AuthPermission
import com.tencent.devops.common.auth.api.AuthResourceType
import com.tencent.devops.common.auth.utils.TActionUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class TxPermissionGrantServiceImpl @Autowired constructor(
    override val grantServiceImpl: GrantServiceImpl,
    override val iamConfiguration: IamConfiguration,
    val authPipelineIdService: AuthPipelineIdService
) : AbsPermissionGrantServiceImpl(grantServiceImpl, iamConfiguration) {
    override fun grantInstancePermission(
        userId: String,
        action: String,
        projectId: String,
        resourceCode: String,
        resourceType: String
    ): Boolean {
        // 如果校验的资源为pipeline,需要兼容传pipelineId的情况
        val useResourceCode = authPipelineIdService.findPipelineAutoId(resourceType, resourceCode)
        val newAction = TActionUtils.buildAction(AuthPermission.get(action), AuthResourceType.get(resourceType))
        return super.grantInstancePermission(userId, newAction, projectId, useResourceCode, resourceType)
    }
}
