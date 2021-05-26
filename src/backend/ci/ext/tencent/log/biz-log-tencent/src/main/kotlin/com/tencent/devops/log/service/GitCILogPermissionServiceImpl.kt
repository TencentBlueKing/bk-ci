package com.tencent.devops.log.service

import com.tencent.devops.auth.api.service.ServicePermissionAuthResource
import com.tencent.devops.common.auth.utils.GitCIUtils
import com.tencent.devops.common.client.Client
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

class GitCILogPermissionServiceImpl @Autowired constructor(
    val client: Client
) : LogPermissionService {
    override fun verifyUserLogPermission(projectCode: String, pipelineId: String, userId: String): Boolean {
        val gitProjectId = GitCIUtils.getGitCiProjectId(projectCode)
        logger.info("GitCILogPermissionServiceImpl user:$userId projectId: $projectCode gitProject: $gitProjectId")
        return client.get(ServicePermissionAuthResource::class).validateUserResourcePermission(
            userId, "", gitProjectId, null).data ?: false
    }

    companion object {
        val logger = LoggerFactory.getLogger(GitCILogPermissionServiceImpl::class.java)
    }
}
