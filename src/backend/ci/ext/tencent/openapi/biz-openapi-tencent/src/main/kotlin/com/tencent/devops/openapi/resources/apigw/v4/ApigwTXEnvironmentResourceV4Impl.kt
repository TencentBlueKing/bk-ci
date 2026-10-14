package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.environment.api.ServiceCmdbNodeResource
import com.tencent.devops.environment.api.thirdpartyagent.ServiceAgentResource
import com.tencent.devops.environment.pojo.job.AddCmdbNodesRes
import com.tencent.devops.openapi.api.apigw.v4.ApigwTXEnvironmentResourceV4
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwTXEnvironmentResourceV4Impl @Autowired constructor(
    val client: Client
) : ApigwTXEnvironmentResourceV4 {
    override fun addCmdbNodes(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        nodeIps: List<String>
    ): Result<AddCmdbNodesRes> {
        return client.get(ServiceCmdbNodeResource::class).addCmdbNodes(userId, projectId, nodeIps)
    }

    override fun updateCreateNodeProject(
        appCode: String?,
        apigwType: String?,
        userId: String,
        newProjectId: String,
        workspaceName: String
    ): Result<Boolean> {
        logger.info("updateCreateNodeProject|$userId|$newProjectId|$workspaceName")
        return client.get(ServiceAgentResource::class).updateCreateNodeProject(userId, newProjectId, workspaceName)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwTXEnvironmentResourceV4Impl::class.java)
    }
}
