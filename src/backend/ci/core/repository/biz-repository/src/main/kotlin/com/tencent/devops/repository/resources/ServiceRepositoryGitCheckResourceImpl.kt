package com.tencent.devops.repository.resources

import com.tencent.devops.common.api.enums.RepositoryConfig
import com.tencent.devops.common.api.pojo.Result
import com.tencent.devops.common.web.RestResource
import com.tencent.devops.repository.api.ServiceRepositoryGitCheckResource
import com.tencent.devops.repository.pojo.RepositoryGitCheck
import com.tencent.devops.repository.service.RepoGitCheckService
import org.springframework.beans.factory.annotation.Autowired

@RestResource
class ServiceRepositoryGitCheckResourceImpl @Autowired constructor(
    private val repoGitCheckService: RepoGitCheckService
) : ServiceRepositoryGitCheckResource {
    override fun getGitCheck(
        pipelineId: String,
        commitId: String,
        context: String,
        repositoryConfig: RepositoryConfig
    ): Result<RepositoryGitCheck?> {
        return Result(repoGitCheckService.getGitCheck(pipelineId, repositoryConfig, commitId, context))
    }

    override fun createGitCheck(gitCheck: RepositoryGitCheck) {
        repoGitCheckService.creatGitCheck(gitCheck)
    }

    override fun updateGitCheck(gitCheckId: Long, buildNumber: Int) {
        repoGitCheckService.updateGitCheck(gitCheckId = gitCheckId, buildNumber = buildNumber)
    }
}
