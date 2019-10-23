package com.tencent.devops.openapi.resources

import com.tencent.devops.common.api.enums.ScmType
import com.tencent.devops.common.api.pojo.Page
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.ApigwRepositoryResource
import com.tencent.devops.repository.api.ServiceRepositoryResource
import com.tencent.devops.repository.pojo.Repository
import com.tencent.devops.repository.pojo.RepositoryId
import com.tencent.devops.repository.pojo.RepositoryInfo
import com.tencent.devops.repository.pojo.enums.Permission
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRepositoryResourceImpl @Autowired constructor(private val client: Client) : ApigwRepositoryResource {
    override fun create(userId: String, projectId: String, repository: Repository): Result<RepositoryId> {
        logger.info("create repostitories in project:userId=$userId,projectId=$projectId,repository:$repository")
        return client.get(ServiceRepositoryResource::class).create(
            userId,
            projectId,
            repository
        )
    }

    override fun hasPermissionList(userId: String, projectId: String, repositoryType: ScmType?): Result<Page<RepositoryInfo>> {
        logger.info("get user's use repostitories in project:userId=$userId,projectId=$projectId,repositoryType:$repositoryType")
        return client.get(ServiceRepositoryResource::class).hasPermissionList(userId,
                projectId,
                repositoryType,
                Permission.USE)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwRepositoryResourceImpl::class.java)
    }
}