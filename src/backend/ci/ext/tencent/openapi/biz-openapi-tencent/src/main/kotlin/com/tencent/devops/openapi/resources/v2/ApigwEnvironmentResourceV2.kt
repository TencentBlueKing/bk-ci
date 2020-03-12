package com.tencent.devops.openapi.resources.v2

import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.environment.api.ServiceEnvironmentResource
import com.tencent.devops.environment.api.ServiceNodeResource
import com.tencent.devops.environment.pojo.EnvWithPermission
import com.tencent.devops.environment.pojo.NodeBaseInfo
import com.tencent.devops.environment.pojo.NodeWithPermission
import com.tencent.devops.openapi.api.v2.ApigwEnvironmentResourceV2
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ApigwEnvironmentResourceV2 @Autowired constructor(
    private val client: Client
): ApigwEnvironmentResourceV2 {
    override fun listUsableServerNodes(userId: String, projectId: String): Result<List<NodeWithPermission>> {
        logger.info("listUsableServerNodes userId[$userId] project[$projectId]")
        return client.get(ServiceNodeResource::class).listUsableServerNodes(userId, projectId)
    }

    override fun listUsableServerEnvs(userId: String, projectId: String): Result<List<EnvWithPermission>> {
        logger.info("listUsableServerEnvs userId[$userId] project[$projectId]")
        return client.get(ServiceEnvironmentResource:: class).listUsableServerEnvs(userId, projectId)
    }

    override fun listRawByEnvNames(
        userId: String,
        projectId: String,
        envNames: List<String>
    ): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvNames userId[$userId] project[$projectId] envNames[$envNames]")
        return client.get(ServiceEnvironmentResource:: class).listRawByEnvNames(userId, projectId, envNames)
    }

    override fun listRawByEnvHashIds(
        userId: String,
        projectId: String,
        envHashIds: List<String>
    ): Result<List<EnvWithPermission>> {
        logger.info("listRawByEnvNames userId[$userId] project[$projectId] envHashIds[$envHashIds]")
        return client.get(ServiceEnvironmentResource::class).listRawByEnvHashIds(userId, projectId, envHashIds)
    }

    override fun listRawByHashIds(
        userId: String,
        projectId: String,
        nodeHashIds: List<String>
    ): Result<List<NodeBaseInfo>> {
        logger.info("listRawByEnvNames userId[$userId] project[$projectId] nodeHashIds[$nodeHashIds]")
        return client.get(ServiceNodeResource::class).listRawByHashIds(userId, projectId, nodeHashIds)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwEnvironmentResourceV2::class.java)
    }
}