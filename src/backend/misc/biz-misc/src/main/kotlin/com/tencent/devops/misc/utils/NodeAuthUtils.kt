package com.tencent.devops.misc.utils

import com.tencent.devops.common.api.util.HashUtil
import com.tencent.devops.common.auth.api.AuthResourceApi
import com.tencent.devops.common.auth.api.BkAuthResourceType
import com.tencent.devops.common.auth.code.EnvironmentAuthServiceCode
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class NodeAuthUtils @Autowired constructor(
    private val authResourceApi: AuthResourceApi,
    private val environmentAuthServiceCode: EnvironmentAuthServiceCode
) {

    fun createNodeResource(user: String, projectId: String, nodeId: Long, nodeStringId: String, nodeIp: String) {
        authResourceApi.createResource(
            user = user,
            serviceCode = environmentAuthServiceCode,
            resourceType = BkAuthResourceType.ENVIRONMENT_ENV_NODE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId),
            resourceName = "$nodeStringId($nodeIp)"
        )
    }

    fun deleteResource(projectId: String, nodeId: Long) {
        authResourceApi.deleteResource(
            serviceCode = environmentAuthServiceCode,
            resourceType = BkAuthResourceType.ENVIRONMENT_ENV_NODE,
            projectCode = projectId,
            resourceCode = HashUtil.encodeLongId(nodeId)
        )
    }
}