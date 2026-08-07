package com.tencent.devops.openapi.resources.apigw.v4

import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.client.Client
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.openapi.api.apigw.v4.ApigwRepositoryRefResourceV4
import com.tencent.devops.repository.api.scm.UserScmRepositoryApiResource
import com.tencent.devops.scm.api.pojo.Reference
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ApigwRepositoryRefResourceV4Impl @Autowired constructor(private val client: Client) :
    ApigwRepositoryRefResourceV4 {

    override fun listBranches(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        search: String?,
        page: Int,
        pageSize: Int
    ): Result<List<Reference>> {
        logger.info(
            "OPENAPI_REPOSITORY_REF_V4|$userId|listBranches|$projectId|$repositoryId|$repositoryType|$search"
        )
        return client.get(UserScmRepositoryApiResource::class).listBranches(
            userId = userId,
            projectId = projectId,
            repositoryType = repositoryType,
            repoHashIdOrName = repositoryId,
            search = search,
            page = page,
            pageSize = pageSize
        )
    }

    override fun listTags(
        appCode: String?,
        apigwType: String?,
        userId: String,
        projectId: String,
        repositoryId: String,
        repositoryType: RepositoryType?,
        search: String?,
        page: Int,
        pageSize: Int
    ): Result<List<Reference>> {
        logger.info(
            "OPENAPI_REPOSITORY_REF_V4|$userId|listTags|$projectId|$repositoryId|$repositoryType|$search"
        )
        return client.get(UserScmRepositoryApiResource::class).listTags(
            userId = userId,
            projectId = projectId,
            repositoryType = repositoryType,
            repoHashIdOrName = repositoryId,
            search = search,
            page = page,
            pageSize = pageSize
        )
    }

    companion object {
        private val logger = LoggerFactory.getLogger(ApigwRepositoryRefResourceV4Impl::class.java)
    }
}
