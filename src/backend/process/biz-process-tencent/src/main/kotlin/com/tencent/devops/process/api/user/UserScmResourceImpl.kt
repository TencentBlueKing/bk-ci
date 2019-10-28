package com.tencent.devops.process.api.user

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.enums.RepositoryType
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.process.service.scm.ScmService
import com.tencent.devops.scm.pojo.RevisionInfo
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class UserScmResourceImpl @Autowired constructor(
    private val scmService: ScmService
) : UserScmResource {

    override fun getLatestRevision(
        projectId: String,
        repositoryId: String,
        branchName: String?,
        additionalPath: String?,
        repositoryType: RepositoryType?
    ): Result<RevisionInfo> {
        return scmService.getLatestRevision(projectId, getRepositoryConfig(repositoryId, repositoryType), branchName, additionalPath, null)
    }

    override fun listBranches(projectId: String, repositoryId: String, repositoryType: RepositoryType?) =
        scmService.listBranches(projectId, getRepositoryConfig(repositoryId, repositoryType))

    override fun listTags(projectId: String, repositoryId: String, repositoryType: RepositoryType?) =
        scmService.listTags(projectId, getRepositoryConfig(repositoryId, repositoryType))

    private fun getRepositoryConfig(repositoryId: String, repositoryType: RepositoryType?): RepositoryConfig {
        return if (repositoryType == null || repositoryType == RepositoryType.ID) {
            RepositoryConfig(repositoryId, null, RepositoryType.ID)
        } else {
            RepositoryConfig(null, repositoryId, repositoryType)
        }
    }
}